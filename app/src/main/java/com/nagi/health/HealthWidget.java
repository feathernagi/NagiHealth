package com.nagi.health;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;

/** Виджет: миссии дня с галочками, серия, следующее напоминание, карточка от Claude. */
public class HealthWidget extends AppWidgetProvider {
    static final int[] ROWS = {R.id.r0, R.id.r1, R.id.r2, R.id.r3, R.id.r4, R.id.r5, R.id.r6};

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { updateAll(c); }

    static PendingIntent open(Context c, int code, String act) {
        Intent i = new Intent(c, MainActivity.class).putExtra("act", act)
            .setData(Uri.parse("nagi://w/" + code))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, 5000 + code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, HealthWidget.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        JSONObject w;
        try { String s = Store.get(c, "widget"); w = s == null ? new JSONObject() : new JSONObject(s); } catch (Exception e) { w = new JSONObject(); }
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        boolean stale = !today.equals(w.optString("date"));
        v.setTextViewText(R.id.title, stale ? "Новый день — открой" : "Сегодня " + w.optInt("done") + "/" + w.optInt("total"));
        v.setTextViewText(R.id.streak, "🔥" + w.optInt("streak"));
        v.setTextViewText(R.id.next, w.optString("next", ""));
        String card = w.optString("card", "");
        v.setViewVisibility(R.id.card, card.isEmpty() || stale ? View.GONE : View.VISIBLE);
        v.setTextViewText(R.id.card, card);
        JSONArray items = stale ? new JSONArray() : w.optJSONArray("items");
        if (items == null) items = new JSONArray();
        for (int i = 0; i < ROWS.length; i++) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) { v.setViewVisibility(ROWS[i], View.GONE); continue; }
            v.setViewVisibility(ROWS[i], View.VISIBLE);
            boolean d = it.optBoolean("d");
            v.setTextViewText(ROWS[i], (d ? "✅  " : "⬜  ") + it.optString("l"));
            v.setTextColor(ROWS[i], d ? 0xFF8E96A8 : 0xFFF1F3F8);
            v.setOnClickPendingIntent(ROWS[i], open(c, i, it.optString("a", "open")));
        }
        v.setOnClickPendingIntent(R.id.water, open(c, 20, "addWater()"));
        v.setOnClickPendingIntent(R.id.desk, open(c, 21, "start('desk')"));
        v.setOnClickPendingIntent(R.id.root, open(c, 22, "open"));
        m.updateAppWidget(ids, v);
    }
}
