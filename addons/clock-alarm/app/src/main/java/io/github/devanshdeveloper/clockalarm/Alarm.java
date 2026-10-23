package io.github.devanshdeveloper.clockalarm;

import java.util.Calendar;
import java.util.Locale;

public class Alarm {

    public static final int DAY_SUNDAY = 1 << Calendar.SUNDAY;
    public static final int DAY_MONDAY = 1 << Calendar.MONDAY;
    public static final int DAY_TUESDAY = 1 << Calendar.TUESDAY;
    public static final int DAY_WEDNESDAY = 1 << Calendar.WEDNESDAY;
    public static final int DAY_THURSDAY = 1 << Calendar.THURSDAY;
    public static final int DAY_FRIDAY = 1 << Calendar.FRIDAY;
    public static final int DAY_SATURDAY = 1 << Calendar.SATURDAY;
    public static final int DAYS_EVERYDAY = DAY_SUNDAY | DAY_MONDAY | DAY_TUESDAY | DAY_WEDNESDAY | DAY_THURSDAY | DAY_FRIDAY | DAY_SATURDAY;
    public static final int DAYS_WEEKDAYS = DAY_MONDAY | DAY_TUESDAY | DAY_WEDNESDAY | DAY_THURSDAY | DAY_FRIDAY;

    public int id;
    public int hour;
    public int minute;
    public String label;
    public boolean enabled;
    public int daysOfWeek; // bitmask of Calendar day of week, 0 for one-off
    public String soundUri;

    public Alarm(int id, int hour, int minute, String label, boolean enabled, int daysOfWeek, String soundUri) {
        this.id = id;
        this.hour = Math.max(0, Math.min(23, hour));
        this.minute = Math.max(0, Math.min(59, minute));
        this.label = label != null ? label.trim() : "Alarm";
        if (this.label.isEmpty()) this.label = "Alarm";
        this.enabled = enabled;
        this.daysOfWeek = daysOfWeek;
        this.soundUri = soundUri != null ? soundUri : "";
    }

    public long getNextOccurrenceMillis(long nowMillis) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);

        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(nowMillis);
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (daysOfWeek == 0) {
            // One-off
            if (target.getTimeInMillis() <= nowMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1);
            }
            return target.getTimeInMillis();
        }

        // Repeating alarm
        for (int i = 0; i < 8; i++) {
            int calDay = target.get(Calendar.DAY_OF_WEEK);
            boolean activeOnDay = (daysOfWeek & (1 << calDay)) != 0;
            if (activeOnDay && target.getTimeInMillis() > nowMillis) {
                return target.getTimeInMillis();
            }
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        return target.getTimeInMillis();
    }

    public String getTimeString() {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    public String getDaysSummary() {
        if (daysOfWeek == 0) return "Once";
        if (daysOfWeek == DAYS_EVERYDAY) return "Every day";
        if (daysOfWeek == DAYS_WEEKDAYS) return "Weekdays";

        StringBuilder sb = new StringBuilder();
        if ((daysOfWeek & DAY_SUNDAY) != 0) sb.append("Sun ");
        if ((daysOfWeek & DAY_MONDAY) != 0) sb.append("Mon ");
        if ((daysOfWeek & DAY_TUESDAY) != 0) sb.append("Tue ");
        if ((daysOfWeek & DAY_WEDNESDAY) != 0) sb.append("Wed ");
        if ((daysOfWeek & DAY_THURSDAY) != 0) sb.append("Thu ");
        if ((daysOfWeek & DAY_FRIDAY) != 0) sb.append("Fri ");
        if ((daysOfWeek & DAY_SATURDAY) != 0) sb.append("Sat ");
        return sb.toString().trim();
    }

    public String serialize() {
        String safeLabel = label.replace('\t', ' ').replace('\n', ' ');
        return id + "\t" + hour + "\t" + minute + "\t" + safeLabel + "\t" + (enabled ? "1" : "0") + "\t" + daysOfWeek + "\t" + soundUri;
    }

    public static Alarm deserialize(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = line.split("\t", -1);
        if (parts.length < 5) return null;
        try {
            int id = Integer.parseInt(parts[0]);
            int hour = Integer.parseInt(parts[1]);
            int min = Integer.parseInt(parts[2]);
            String lbl = parts[3];
            boolean en = "1".equals(parts[4]);
            int days = parts.length > 5 ? Integer.parseInt(parts[5]) : 0;
            String uri = parts.length > 6 ? parts[6] : "";
            return new Alarm(id, hour, min, lbl, en, days, uri);
        } catch (Exception e) {
            return null;
        }
    }
}
