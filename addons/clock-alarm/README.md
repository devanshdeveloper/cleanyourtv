# Clock & Alarm

A clean, high-contrast digital clock, alarm manager, countdown timer, and stopwatch built specifically for Android TV and module card docks.

## Features

- **Large Digital Clock**: Big, readable display showing the current time (with seconds) and full date.
- **Alarm Scheduler**: Create, toggle, edit, and delete recurring or one-off alarms with custom labels and repeat days (weekdays, everyday, or custom).
- **Exact Ringing**: Uses `AlarmManager.setAlarmClock` to ensure alarms fire on time even during system idle/Doze states.
- **Full-Screen Ringing**: Full-screen alert screen with Snooze (+10m) and Dismiss buttons, playing the system alarm sound through `MediaPlayer`. Includes an automatic 5-minute timeout.
- **Boot Persistence**: Automatically restores all scheduled alarms on device reboot without needing any background polling services.
- **Countdown Timer & Stopwatch**: Quick timer presets (1m, 5m, 10m, 15m, 30m) with audible completion alert, and a precise millisecond stopwatch.

> **Note**: An alarm only rings while the TV is powered on or in standby as the TV firmware allows. Certain TV boards cut CPU power completely in deep standby; in such cases, alarms fire once the board resumes.

## Permissions

This add-on declares only standard, non-invasive system permissions required for alarm scheduling:

- `android.permission.RECEIVE_BOOT_COMPLETED`: Reschedules active alarms when the TV boots or restarts.
- `android.permission.WAKE_LOCK`: Keeps the screen awake while an alarm is ringing so the owner can dismiss it.
- `android.permission.VIBRATE`: Provides haptic feedback if running on a remote or device with vibration support.
- `android.permission.SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: Required on Android 12+ (API 31+) to schedule alarms that fire at the exact minute.

No network access or background services are used.
