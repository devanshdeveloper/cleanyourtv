package io.github.devanshdeveloper.clockalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        if (alarmId < 0) return;

        Alarm alarm = AlarmStore.getById(context, alarmId);
        if (alarm == null || !alarm.enabled) return;

        // Launch full-screen ring activity
        Intent ringIntent = new Intent(context, RingActivity.class);
        ringIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(ringIntent);

        if (alarm.daysOfWeek != 0) {
            // Reschedule next repeating occurrence
            AlarmScheduler.schedule(context, alarm);
        } else {
            // One-off alarm: disable and save
            alarm.enabled = false;
            AlarmStore.saveOrUpdate(context, alarm);
        }
    }
}
