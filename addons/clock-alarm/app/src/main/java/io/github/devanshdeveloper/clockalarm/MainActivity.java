package io.github.devanshdeveloper.clockalarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout llContent;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private int currentTab = 0; // 0=Clock, 1=Alarms, 2=Timer, 3=Stopwatch

    // Timer state
    private long timerRemainingMs = 5 * 60 * 1000L;
    private boolean timerRunning = false;
    private long timerLastTick = 0;
    private TextView tvTimerDisplay;
    private Button btnTimerToggle;

    // Stopwatch state
    private long stopwatchElapsedMs = 0;
    private boolean stopwatchRunning = false;
    private long stopwatchStartTime = 0;
    private TextView tvStopwatchDisplay;
    private Button btnStopwatchToggle;

    private final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentTab == 0) {
                updateClockDisplay();
            } else if (currentTab == 2 && timerRunning) {
                updateTimerTick();
            } else if (currentTab == 3 && stopwatchRunning) {
                updateStopwatchTick();
            }
            clockHandler.postDelayed(this, 500);
        }
    };

    private TextView tvBigClock;
    private TextView tvClockDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF121212);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        // Top tab row
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER_HORIZONTAL);
        tabRow.setPadding(0, 0, 0, (int) (16 * getResources().getDisplayMetrics().density));

        addTab(tabRow, getString(R.string.tab_clock), 0);
        addTab(tabRow, getString(R.string.tab_alarms), 1);
        addTab(tabRow, getString(R.string.tab_timer), 2);
        addTab(tabRow, getString(R.string.tab_stopwatch), 3);
        root.addView(tabRow);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(scroll, scrollLp);

        llContent = new LinearLayout(this);
        llContent.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(llContent);

        setContentView(root);
        switchTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clockHandler.post(tickerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(tickerRunnable);
    }

    private void addTab(LinearLayout container, String title, int tabIndex) {
        Button btn = new Button(this);
        btn.setText(title);
        btn.setTextSize(14);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundResource(R.drawable.btn_tab);
        btn.setFocusable(true);
        btn.setOnClickListener(v -> switchTab(tabIndex));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int) (130 * getResources().getDisplayMetrics().density),
                (int) (40 * getResources().getDisplayMetrics().density)
        );
        lp.rightMargin = (int) (12 * getResources().getDisplayMetrics().density);
        container.addView(btn, lp);
    }

    private void switchTab(int tab) {
        currentTab = tab;
        llContent.removeAllViews();
        if (tab == 0) renderClockPage();
        else if (tab == 1) renderAlarmsPage();
        else if (tab == 2) renderTimerPage();
        else if (tab == 3) renderStopwatchPage();
    }

    // -------------------------------------------------------------
    // Page 0: Clock
    // -------------------------------------------------------------
    private void renderClockPage() {
        llContent.setGravity(Gravity.CENTER);

        tvBigClock = new TextView(this);
        tvBigClock.setTextSize(72);
        tvBigClock.setTextColor(Color.WHITE);
        tvBigClock.setGravity(Gravity.CENTER);
        llContent.addView(tvBigClock);

        tvClockDate = new TextView(this);
        tvClockDate.setTextSize(20);
        tvClockDate.setTextColor(0xFFFF9800);
        tvClockDate.setGravity(Gravity.CENTER);
        tvClockDate.setPadding(0, 8, 0, 0);
        llContent.addView(tvClockDate);

        updateClockDisplay();
    }

    private void updateClockDisplay() {
        if (tvBigClock != null) {
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            tvBigClock.setText(tf.format(new Date()));
        }
        if (tvClockDate != null) {
            SimpleDateFormat df = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            tvClockDate.setText(df.format(new Date()));
        }
    }

    // -------------------------------------------------------------
    // Page 1: Alarms
    // -------------------------------------------------------------
    private void renderAlarmsPage() {
        llContent.setGravity(Gravity.NO_GRAVITY);

        Button btnAdd = new Button(this);
        btnAdd.setText(R.string.btn_add_alarm);
        btnAdd.setTextSize(14);
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setBackgroundResource(R.drawable.btn_pill);
        btnAdd.setFocusable(true);
        btnAdd.setOnClickListener(v -> promptAddAlarm());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                (int) (160 * getResources().getDisplayMetrics().density),
                (int) (40 * getResources().getDisplayMetrics().density)
        );
        btnLp.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
        llContent.addView(btnAdd, btnLp);

        List<Alarm> alarms = AlarmStore.load(this);
        if (alarms.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_alarms);
            empty.setTextSize(16);
            empty.setTextColor(0xFF888888);
            empty.setPadding(0, 16, 0, 0);
            llContent.addView(empty);
            return;
        }

        for (Alarm a : alarms) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setBackgroundResource(R.drawable.card_bg);
            item.setFocusable(true);
            int pad = (int) (12 * getResources().getDisplayMetrics().density);
            item.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(-1, -2);
            itemLp.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
            item.setLayoutParams(itemLp);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, -2, 1f);
            col.setLayoutParams(colLp);

            TextView tvTime = new TextView(this);
            tvTime.setText(a.getTimeString());
            tvTime.setTextSize(26);
            tvTime.setTextColor(a.enabled ? 0xFFFF9800 : 0xFF777777);
            col.addView(tvTime);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(a.label + " \u00B7 " + a.getDaysSummary());
            tvDesc.setTextSize(13);
            tvDesc.setTextColor(a.enabled ? 0xFFCCCCCC : 0xFF666666);
            col.addView(tvDesc);

            item.addView(col);

            Button btnToggle = new Button(this);
            btnToggle.setText(a.enabled ? "ON" : "OFF");
            btnToggle.setTextSize(12);
            btnToggle.setTextColor(a.enabled ? 0xFF81C784 : 0xFFE57373);
            btnToggle.setBackgroundResource(R.drawable.btn_pill);
            btnToggle.setFocusable(true);
            btnToggle.setOnClickListener(v -> {
                a.enabled = !a.enabled;
                AlarmStore.saveOrUpdate(this, a);
                if (a.enabled) AlarmScheduler.schedule(this, a);
                else AlarmScheduler.cancel(this, a.id);
                renderAlarmsPage();
            });
            item.addView(btnToggle);

            Button btnDelete = new Button(this);
            btnDelete.setText("Delete");
            btnDelete.setTextSize(12);
            btnDelete.setTextColor(0xFFEF9A9A);
            btnDelete.setBackgroundResource(R.drawable.btn_pill);
            btnDelete.setFocusable(true);
            btnDelete.setOnClickListener(v -> {
                AlarmScheduler.cancel(this, a.id);
                AlarmStore.delete(this, a.id);
                renderAlarmsPage();
            });
            item.addView(btnDelete);

            llContent.addView(item);
        }
    }

    private void promptAddAlarm() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText etLabel = new EditText(this);
        etLabel.setHint(R.string.alarm_label_prompt);
        layout.addView(etLabel);

        final EditText etHour = new EditText(this);
        etHour.setHint(R.string.alarm_hour_prompt);
        layout.addView(etHour);

        final EditText etMinute = new EditText(this);
        etMinute.setHint(R.string.alarm_minute_prompt);
        layout.addView(etMinute);

        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_add_alarm)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        String lbl = etLabel.getText().toString().trim();
                        int h = Integer.parseInt(etHour.getText().toString().trim());
                        int m = Integer.parseInt(etMinute.getText().toString().trim());
                        int id = (int) (System.currentTimeMillis() % 1000000);
                        Alarm alarm = new Alarm(id, h, m, lbl, true, Alarm.DAYS_EVERYDAY, "");
                        AlarmStore.saveOrUpdate(this, alarm);
                        AlarmScheduler.schedule(this, alarm);
                        Toast.makeText(this, R.string.alarm_saved, Toast.LENGTH_SHORT).show();
                        renderAlarmsPage();
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid time input", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // -------------------------------------------------------------
    // Page 2: Countdown Timer
    // -------------------------------------------------------------
    private void renderTimerPage() {
        llContent.setGravity(Gravity.CENTER);

        tvTimerDisplay = new TextView(this);
        tvTimerDisplay.setTextSize(64);
        tvTimerDisplay.setTextColor(Color.WHITE);
        tvTimerDisplay.setGravity(Gravity.CENTER);
        updateTimerDisplay();
        llContent.addView(tvTimerDisplay);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, (int) (24 * getResources().getDisplayMetrics().density));

        btnTimerToggle = new Button(this);
        btnTimerToggle.setText(timerRunning ? R.string.btn_pause : R.string.btn_start);
        btnTimerToggle.setTextSize(15);
        btnTimerToggle.setTextColor(Color.WHITE);
        btnTimerToggle.setBackgroundResource(R.drawable.btn_pill);
        btnTimerToggle.setFocusable(true);
        btnTimerToggle.setOnClickListener(v -> toggleTimer());
        controls.addView(btnTimerToggle, new LinearLayout.LayoutParams((int) (120 * getResources().getDisplayMetrics().density), (int) (44 * getResources().getDisplayMetrics().density)));

        Button btnReset = new Button(this);
        btnReset.setText(R.string.btn_reset);
        btnReset.setTextSize(15);
        btnReset.setTextColor(0xFFCCCCCC);
        btnReset.setBackgroundResource(R.drawable.btn_pill);
        btnReset.setFocusable(true);
        btnReset.setOnClickListener(v -> resetTimer());
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams((int) (120 * getResources().getDisplayMetrics().density), (int) (44 * getResources().getDisplayMetrics().density));
        rLp.leftMargin = (int) (16 * getResources().getDisplayMetrics().density);
        controls.addView(btnReset, rLp);

        llContent.addView(controls);

        // Preset buttons
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);

        addPresetButton(presets, "1m", 1 * 60 * 1000L);
        addPresetButton(presets, "5m", 5 * 60 * 1000L);
        addPresetButton(presets, "10m", 10 * 60 * 1000L);
        addPresetButton(presets, "15m", 15 * 60 * 1000L);
        addPresetButton(presets, "30m", 30 * 60 * 1000L);
        llContent.addView(presets);
    }

    private void addPresetButton(LinearLayout container, String label, long durationMs) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setBackgroundResource(R.drawable.btn_tab);
        b.setFocusable(true);
        b.setOnClickListener(v -> {
            timerRunning = false;
            timerRemainingMs = durationMs;
            if (btnTimerToggle != null) btnTimerToggle.setText(R.string.btn_start);
            updateTimerDisplay();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int) (64 * getResources().getDisplayMetrics().density),
                (int) (36 * getResources().getDisplayMetrics().density)
        );
        lp.rightMargin = 8;
        container.addView(b, lp);
    }

    private void toggleTimer() {
        timerRunning = !timerRunning;
        if (timerRunning) {
            timerLastTick = SystemClock.elapsedRealtime();
        }
        if (btnTimerToggle != null) {
            btnTimerToggle.setText(timerRunning ? R.string.btn_pause : R.string.btn_start);
        }
    }

    private void resetTimer() {
        timerRunning = false;
        timerRemainingMs = 5 * 60 * 1000L;
        if (btnTimerToggle != null) btnTimerToggle.setText(R.string.btn_start);
        updateTimerDisplay();
    }

    private void updateTimerTick() {
        long now = SystemClock.elapsedRealtime();
        long delta = now - timerLastTick;
        timerLastTick = now;
        timerRemainingMs -= delta;
        if (timerRemainingMs <= 0) {
            timerRemainingMs = 0;
            timerRunning = false;
            if (btnTimerToggle != null) btnTimerToggle.setText(R.string.btn_start);
            Toast.makeText(this, R.string.timer_done, Toast.LENGTH_LONG).show();
        }
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        if (tvTimerDisplay == null) return;
        long totalSec = Math.max(0, timerRemainingMs / 1000);
        long m = totalSec / 60;
        long s = totalSec % 60;
        tvTimerDisplay.setText(String.format(Locale.US, "%02d:%02d", m, s));
    }

    // -------------------------------------------------------------
    // Page 3: Stopwatch
    // -------------------------------------------------------------
    private void renderStopwatchPage() {
        llContent.setGravity(Gravity.CENTER);

        tvStopwatchDisplay = new TextView(this);
        tvStopwatchDisplay.setTextSize(64);
        tvStopwatchDisplay.setTextColor(Color.WHITE);
        tvStopwatchDisplay.setGravity(Gravity.CENTER);
        updateStopwatchDisplay();
        llContent.addView(tvStopwatchDisplay);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);

        btnStopwatchToggle = new Button(this);
        btnStopwatchToggle.setText(stopwatchRunning ? R.string.btn_pause : R.string.btn_start);
        btnStopwatchToggle.setTextSize(15);
        btnStopwatchToggle.setTextColor(Color.WHITE);
        btnStopwatchToggle.setBackgroundResource(R.drawable.btn_pill);
        btnStopwatchToggle.setFocusable(true);
        btnStopwatchToggle.setOnClickListener(v -> toggleStopwatch());
        controls.addView(btnStopwatchToggle, new LinearLayout.LayoutParams((int) (120 * getResources().getDisplayMetrics().density), (int) (44 * getResources().getDisplayMetrics().density)));

        Button btnReset = new Button(this);
        btnReset.setText(R.string.btn_reset);
        btnReset.setTextSize(15);
        btnReset.setTextColor(0xFFCCCCCC);
        btnReset.setBackgroundResource(R.drawable.btn_pill);
        btnReset.setFocusable(true);
        btnReset.setOnClickListener(v -> resetStopwatch());
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams((int) (120 * getResources().getDisplayMetrics().density), (int) (44 * getResources().getDisplayMetrics().density));
        rLp.leftMargin = (int) (16 * getResources().getDisplayMetrics().density);
        controls.addView(btnReset, rLp);

        llContent.addView(controls);
    }

    private void toggleStopwatch() {
        stopwatchRunning = !stopwatchRunning;
        if (stopwatchRunning) {
            stopwatchStartTime = SystemClock.elapsedRealtime() - stopwatchElapsedMs;
        }
        if (btnStopwatchToggle != null) {
            btnStopwatchToggle.setText(stopwatchRunning ? R.string.btn_pause : R.string.btn_start);
        }
    }

    private void resetStopwatch() {
        stopwatchRunning = false;
        stopwatchElapsedMs = 0;
        if (btnStopwatchToggle != null) btnStopwatchToggle.setText(R.string.btn_start);
        updateStopwatchDisplay();
    }

    private void updateStopwatchTick() {
        stopwatchElapsedMs = SystemClock.elapsedRealtime() - stopwatchStartTime;
        updateStopwatchDisplay();
    }

    private void updateStopwatchDisplay() {
        if (tvStopwatchDisplay == null) return;
        long totalSec = stopwatchElapsedMs / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        long ms = (stopwatchElapsedMs % 1000) / 100;
        tvStopwatchDisplay.setText(String.format(Locale.US, "%02d:%02d.%d", m, s, ms));
    }
}
