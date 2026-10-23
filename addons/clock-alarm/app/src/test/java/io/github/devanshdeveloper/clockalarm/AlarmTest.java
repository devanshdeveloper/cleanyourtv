package io.github.devanshdeveloper.clockalarm;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AlarmTest {

    @Test
    public void testSerializationRoundtrip() {
        Alarm original = new Alarm(101, 7, 30, "Morning Wakeup", true, Alarm.DAYS_WEEKDAYS, "content://media/alarm/1");
        String serialized = original.serialize();
        Alarm parsed = Alarm.deserialize(serialized);

        assertNotNull(parsed);
        assertEquals(101, parsed.id);
        assertEquals(7, parsed.hour);
        assertEquals(30, parsed.minute);
        assertEquals("Morning Wakeup", parsed.label);
        assertTrue(parsed.enabled);
        assertEquals(Alarm.DAYS_WEEKDAYS, parsed.daysOfWeek);
        assertEquals("content://media/alarm/1", parsed.soundUri);
    }

    @Test
    public void testOneOffNextOccurrence() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 23, 10, 0, 0);
        long now = cal.getTimeInMillis();

        // Alarm set for 11:00 today (in the future)
        Alarm futureToday = new Alarm(1, 11, 0, "Test", true, 0, "");
        long nextTime = futureToday.getNextOccurrenceMillis(now);

        Calendar res = Calendar.getInstance();
        res.setTimeInMillis(nextTime);
        assertEquals(2026, res.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, res.get(Calendar.MONTH));
        assertEquals(23, res.get(Calendar.DAY_OF_MONTH));
        assertEquals(11, res.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, res.get(Calendar.MINUTE));

        // Alarm set for 09:00 today (already passed, should roll to tomorrow 24th)
        Alarm pastToday = new Alarm(2, 9, 0, "Test", true, 0, "");
        long nextTomorrow = pastToday.getNextOccurrenceMillis(now);

        res.setTimeInMillis(nextTomorrow);
        assertEquals(24, res.get(Calendar.DAY_OF_MONTH));
        assertEquals(9, res.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testRepeatingAlarmCalculation() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 23, 10, 0, 0); // Friday Oct 23, 2026
        long now = cal.getTimeInMillis();

        // Sunday only alarm: should trigger on Sunday Oct 25, 2026
        Alarm sundayAlarm = new Alarm(3, 8, 0, "Sunday Alarm", true, Alarm.DAY_SUNDAY, "");
        long sundayTime = sundayAlarm.getNextOccurrenceMillis(now);

        Calendar res = Calendar.getInstance();
        res.setTimeInMillis(sundayTime);
        assertEquals(Calendar.SUNDAY, res.get(Calendar.DAY_OF_WEEK));
        assertEquals(25, res.get(Calendar.DAY_OF_MONTH));
        assertEquals(8, res.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testSummaryStrings() {
        Alarm once = new Alarm(1, 7, 0, "Test", true, 0, "");
        assertEquals("Once", once.getDaysSummary());

        Alarm everyday = new Alarm(2, 7, 0, "Test", true, Alarm.DAYS_EVERYDAY, "");
        assertEquals("Every day", everyday.getDaysSummary());

        Alarm weekdays = new Alarm(3, 7, 0, "Test", true, Alarm.DAYS_WEEKDAYS, "");
        assertEquals("Weekdays", weekdays.getDaysSummary());
    }
}
