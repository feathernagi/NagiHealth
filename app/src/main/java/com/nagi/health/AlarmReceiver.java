package com.nagi.health;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        int idx = i.getIntExtra("idx", -1);
        JSONObject o = Reminders.load(c).optJSONObject(idx);
        if (o != null) {
            Reminders.notify(c, idx, o);
            Reminders.schedule(c, idx, o); // следующий раз
        }
        HealthWidget.updateAll(c);
        Updater.checkBackground(c, goAsync());
    }
}
