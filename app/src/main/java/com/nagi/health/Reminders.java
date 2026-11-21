package com.nagi.health;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

/** Системные напоминания: срабатывают даже когда приложение закрыто, у каждого типа своя мелодия. */
public class Reminders {
    static final String[] TYPES = {"water", "meal", "move", "train", "care", "weigh", "face", "alarm"};
    static final String[] NAMES = {"Вода", "Еда", "Перерыв", "Тренировка", "Уход", "Взвешивание", "Лицо", "Будильник"};
    static final String CH_VER = "_v1";
    static final int MAX = 80;

    static String channel(String type) {
        for (String t : TYPES) if (t.equals(type)) return "rem_" + t + CH_VER;
        return "rem_water" + CH_VER;
    }

    static void createChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        for (int i = 0; i < TYPES.length; i++) {
            String t = TYPES[i];
            NotificationChannel ch = new NotificationChannel("rem_" + t + CH_VER, NAMES[i], NotificationManager.IMPORTANCE_HIGH);
            int res = c.getResources().getIdentifier("snd_" + t, "raw", c.getPackageName());
            AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(t.equals("alarm") ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            ch.setSound(Uri.parse("android.resource://" + c.getPackageName() + "/" + res), aa);
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }
        nm.createNotificationChannel(new NotificationChannel("updates", "Обновления", NotificationManager.IMPORTANCE_DEFAULT));
    }

    static JSONArray load(Context c) {
        try { String s = Store.get(c, "reminders"); return s == null ? new JSONArray() : new JSONArray(s); }
        catch (Exception e) { return new JSONArray(); }
    }

    static PendingIntent pi(Context c, int idx, int flags) {
        Intent i = new Intent(c, AlarmReceiver.class).putExtra("idx", idx);
        return PendingIntent.getBroadcast(c, 1000 + idx, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static void scheduleAll(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        JSONArray r = load(c);
        for (int i = 0; i < MAX; i++) {
            PendingIntent old = pi(c, i, PendingIntent.FLAG_NO_CREATE);
            if (old != null) { am.cancel(old); old.cancel(); }
        }
        for (int i = 0; i < r.length() && i < MAX; i++) schedule(c, i, r.optJSONObject(i));
    }

    /** Ставит будильник на ближайшее подходящее время для напоминания idx */
    static void schedule(Context c, int idx, JSONObject o) {
        if (o == null) return;
        long at = next(o);
        if (at <= 0) return;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent p = pi(c, idx, PendingIntent.FLAG_UPDATE_CURRENT);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        } catch (SecurityException e) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p); }
    }

    static boolean dayOk(JSONObject o, Calendar cal) {
        JSONArray d = o.optJSONArray("dow");
        if (d == null || d.length() == 0) return true;
        int js = cal.get(Calendar.DAY_OF_WEEK) - 1; // JS: 0=вс
        for (int i = 0; i < d.length(); i++) if (d.optInt(i, -1) == js) return true;
        return false;
    }

    static long next(JSONObject o) {
        String t = o.optString("t", "");
        String[] hm = t.split(":");
        if (hm.length != 2) return -1;
        int h, m;
        try { h = Integer.parseInt(hm[0].trim()); m = Integer.parseInt(hm[1].trim()); } catch (Exception e) { return -1; }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, m);
        long now = System.currentTimeMillis();
        for (int k = 0; k < 8; k++) {
            if (cal.getTimeInMillis() > now + 1000 && dayOk(o, cal)) return cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return -1;
    }

    static final String[] ICONS = {"💧", "🍽", "🪑", "💪", "✨", "⚖️", "😎", "⏰"};
    static String icon(String type) { for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(type)) return ICONS[i]; return "🔔"; }

    static void notify(Context c, int idx, JSONObject o) {
        String type = o.optString("type", "water");
        String act = o.optString("act", "open");
        if (type.equals("water") && !act.contains("addWater")) act = "addWater();" + act;
        Intent open = new Intent(c, MainActivity.class).putExtra("act", act)
            .setData(Uri.parse("nagi://rem/" + idx + "/" + System.currentTimeMillis()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 2000 + idx, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(c, channel(type))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(icon(type) + " " + o.optString("text", "Напоминание"))
            .setContentText(o.optString("sub", "Нажми, чтобы отметить"))
            .setContentIntent(pi).setAutoCancel(true)
            .setCategory(type.equals("alarm") ? Notification.CATEGORY_ALARM : Notification.CATEGORY_REMINDER);
        if (type.equals("alarm")) b.setFullScreenIntent(pi, true);
        c.getSystemService(NotificationManager.class).notify(3000 + idx, b.build());
    }
}
