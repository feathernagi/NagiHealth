package com.nagi.health;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        Reminders.createChannels(c);
        Reminders.scheduleAll(c);
        HealthWidget.updateAll(c);
    }
}
