package com.nagi.health;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

/** Автообновление: проверяет последний релиз на GitHub, скачивает APK и открывает установку. */
public class Updater {
    static final String API = "https://api.github.com/repos/" + Remote.REPO + "/releases/latest";

    static int currentVersion(Context c) {
        try { return (int) c.getPackageManager().getPackageInfo(c.getPackageName(), 0).getLongVersionCode(); }
        catch (Exception e) { return 0; }
    }

    /** {ver, url, notes} или null */
    static JSONObject latest() throws Exception {
        JSONObject r = new JSONObject(Remote.http(API));
        int v = Integer.parseInt(r.optString("tag_name", "v0").replaceAll("\\D", ""));
        JSONArray as = r.optJSONArray("assets");
        String url = null;
        for (int i = 0; as != null && i < as.length(); i++) {
            JSONObject a = as.getJSONObject(i);
            if (a.optString("name").endsWith(".apk")) url = a.optString("browser_download_url");
        }
        if (url == null) return null;
        return new JSONObject().put("ver", v).put("url", url).put("notes", r.optString("body", ""));
    }

    static void check(MainActivity a, boolean manual) {
        new Thread(() -> {
            try {
                JSONObject l = latest();
                int cur = currentVersion(a);
                if (l != null && l.getInt("ver") > cur) a.runOnUiThread(() -> ask(a, l));
                else if (manual) a.runOnUiThread(() -> Toast.makeText(a, "У тебя последняя версия 1." + cur, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                if (manual) a.runOnUiThread(() -> Toast.makeText(a, "Не удалось проверить обновление", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    static void ask(MainActivity a, JSONObject l) {
        if (a.isFinishing()) return;
        a.showUpdateBanner("1." + l.optInt("ver"));
        new AlertDialog.Builder(a)
            .setTitle("🚀 Доступна версия 1." + l.optInt("ver"))
            .setMessage("Что нового:\n" + l.optString("notes") + "\n\nОбновить сейчас? Данные сохранятся.")
            .setPositiveButton("Обновить", (d, w) -> download(a, l.optString("url"), l.optInt("ver")))
            .setNegativeButton("Позже", null).show();
    }

    static void download(MainActivity a, String url, int ver) {
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(a, "Разреши установку из этого приложения и нажми «Обновить» ещё раз", Toast.LENGTH_LONG).show();
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName())));
            return;
        }
        DownloadManager dm = a.getSystemService(DownloadManager.class);
        String name = "NagiHealth-" + ver + ".apk";
        java.io.File f = new java.io.File(a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name);
        if (f.exists()) f.delete();
        DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url))
            .setTitle("NagiHealth 1." + ver)
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(a, Environment.DIRECTORY_DOWNLOADS, name);
        long id = dm.enqueue(r);
        Toast.makeText(a, "Скачиваю обновление…", Toast.LENGTH_SHORT).show();
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return;
                try { c.unregisterReceiver(this); } catch (Exception ignored) {}
                Uri u = dm.getUriForDownloadedFile(id);
                if (u == null) { Toast.makeText(c, "Ошибка загрузки", Toast.LENGTH_SHORT).show(); return; }
                Intent inst = new Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(inst);
            }
        };
        IntentFilter f2 = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) a.registerReceiver(br, f2, Context.RECEIVER_EXPORTED);
        else a.registerReceiver(br, f2);
    }

    /** Фоновая проверка (не чаще раза в 6 часов): уведомление «Доступно обновление». */
    static void checkBackground(Context c, BroadcastReceiver.PendingResult pr) {
        long last = Store.sp(c).getLong("upd_last", 0);
        if (System.currentTimeMillis() - last < 6 * 3600_000L) { pr.finish(); return; }
        Store.sp(c).edit().putLong("upd_last", System.currentTimeMillis()).apply();
        new Thread(() -> {
            try {
                JSONObject l = latest();
                if (l != null && l.getInt("ver") > currentVersion(c) && l.getInt("ver") != Store.sp(c).getInt("upd_notified", 0)) {
                    Store.sp(c).edit().putInt("upd_notified", l.getInt("ver")).apply();
                    Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pi = PendingIntent.getActivity(c, 9000, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Notification n = new Notification.Builder(c, "updates")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle("🚀 Доступно обновление NagiHealth 1." + l.getInt("ver"))
                        .setContentText("Открой приложение и нажми «Обновить»")
                        .setContentIntent(pi).setAutoCancel(true).build();
                    c.getSystemService(NotificationManager.class).notify(9000, n);
                }
            } catch (Exception ignored) {
            } finally { pr.finish(); }
        }).start();
    }
}
