package com.nagi.health;

import android.content.Intent;
import android.webkit.JavascriptInterface;

/** Мост между веб-интерфейсом и Android. В JS доступен как window.Android */
public class Bridge {
    private final MainActivity a;
    Bridge(MainActivity a) { this.a = a; }

    @JavascriptInterface public String get(String path) { return Store.get(a, path); }
    @JavascriptInterface public void put(String path, String json) { Store.put(a, path, json); }
    @JavascriptInterface public String list(String coll) { return Store.list(a, coll); }
    @JavascriptInterface public int version() { return Updater.currentVersion(a); }

    @JavascriptInterface public void setReminders(String json) {
        String old = Store.get(a, "reminders");
        if (json.equals(old)) return;
        Store.put(a, "reminders", json);
        Reminders.scheduleAll(a);
    }

    @JavascriptInterface public void widget(String json) {
        Store.put(a, "widget", json);
        HealthWidget.updateAll(a);
    }

    @JavascriptInterface public void share(String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        a.runOnUiThread(() -> a.startActivity(Intent.createChooser(i, "Отправить отчёт Claude")));
    }

    @JavascriptInterface public void checkUpdate() { a.runOnUiThread(() -> Updater.check(a, true)); }
}
