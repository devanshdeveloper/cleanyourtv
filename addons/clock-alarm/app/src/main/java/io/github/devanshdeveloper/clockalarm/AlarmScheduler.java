package io.github.devanshdeveloper.clockalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

public final class AlarmScheduler {

    public static final String EXTRA_ALARM_ID = "alarm_id";

    private AlarmScheduler() {}

    public static void scheduleAll(Context context) {
        List<Alarm> alarms = AlarmStore.load(context);
        for (Alarm a : alarms) {
            if (a.enabled) {
                schedule(context, a);
            } else {
                cancel(context, a.id);
            }
        }
    }

    public static void schedule(Context context, Alarm alarm) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        cancel(context, alarm.id);
        if (!alarm.enabled) return;

        long triggerAt = alarm.getNextOccurrenceMillis(System.currentTimeMillis());

        Intent triggerIntent = new Intent(context, AlarmReceiver.class);
        triggerIntent.putExtra(EXTRA_ALARM_ID, alarm.id);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                alarm.id,
                triggerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent showIntent = new Intent(context, MainActivity.class);
        PendingIntent showPi = PendingIntent.getActivity(
                context,
                alarm.id,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, showPi), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException se) {
            // Android 12+ exact alarm permission fallback
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void cancel(Context context, int alarmId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent triggerIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                alarmId,
                triggerIntent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }
}
