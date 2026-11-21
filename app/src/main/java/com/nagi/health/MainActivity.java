package com.nagi.health;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;

public class MainActivity extends Activity {
    WebView web;
    boolean loaded = false;
    String pendingAct = null;
    long lastUpdateCheck = 0;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0B0D12"));
        getWindow().setNavigationBarColor(Color.parseColor("#0B0D12"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0B0D12"));
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(this), "Android");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                loaded = true;
                new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::runPending, 700);
            }
        });
        pendingAct = actFrom(getIntent());
        web.loadUrl("file:///android_asset/index.html" + (pendingAct != null ? "#today" : ""));

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        Reminders.createChannels(this);
        Reminders.scheduleAll(this);
        Remote.fetchConfig(this);
    }

    @Override protected void onResume() {
        super.onResume();
        if (System.currentTimeMillis() - lastUpdateCheck > 3 * 3600_000L) {
            lastUpdateCheck = System.currentTimeMillis();
            Updater.check(this, false);
        }
        if (loaded) web.evaluateJavascript("window.render&&render()", null);
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        pendingAct = actFrom(i);
        if (loaded) runPending();
    }

    static String actFrom(Intent i) { return i == null ? null : i.getStringExtra("act"); }

    void runPending() {
        if (pendingAct == null) return;
        String act = pendingAct;
        pendingAct = null;
        if (act.equals("open")) { web.evaluateJavascript("location.hash='today';render()", null); return; }
        web.evaluateJavascript("location.hash='today';render();try{" + act + "}catch(e){}", null);
    }

    /** Вызывается после загрузки свежего config.json из GitHub */
    void configUpdated() {
        if (loaded) web.evaluateJavascript("window.__cfgUpdate&&window.__cfgUpdate()", null);
    }

    void showUpdateBanner(String ver) {
        if (loaded) web.evaluateJavascript("window.__update&&window.__update(" + JSONObject.quote(ver) + ")", null);
    }

    @Override public void onBackPressed() {
        web.evaluateJavascript("(function(){if(document.getElementById('player').style.display==='block'){stop();return 1}if(location.hash==='#today'){location.hash='';return 1}if(TAB!=='home'){go('home');return 1}return 0})()",
            r -> { if (!"1".equals(r)) MainActivity.super.onBackPressed(); });
    }
}
