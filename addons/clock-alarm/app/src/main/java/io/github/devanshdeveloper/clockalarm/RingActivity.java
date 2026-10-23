package io.github.devanshdeveloper.clockalarm;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RingActivity extends Activity {

    private static final long AUTO_DISMISS_MS = 5 * 60 * 1000L; // 5 minutes max duration

    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoDismissRunnable = this::dismiss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn screen on and keep on
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        int alarmId = getIntent().getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        Alarm alarm = alarmId >= 0 ? AlarmStore.getById(this, alarmId) : null;
        String label = alarm != null ? alarm.label : "Alarm";

        buildUi(label);
        startSound(alarm != null ? alarm.soundUri : null);

        handler.postDelayed(autoDismissRunnable, AUTO_DISMISS_MS);
    }

    private void buildUi(String label) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF1B1208); // Dark warm amber/black background
        int pad = (int) (32 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(R.string.ring_title);
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(0xFFFF9800);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        TextView tvTime = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(sdf.format(new Date()));
        tvTime.setTextSize(64);
        tvTime.setTextColor(Color.WHITE);
        tvTime.setGravity(Gravity.CENTER);
        root.addView(tvTime);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(24);
        tvLabel.setTextColor(0xFFFFE082);
        tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setPadding(0, 8, 0, (int) (32 * getResources().getDisplayMetrics().density));
        root.addView(tvLabel);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);

        Button btnDismiss = new Button(this);
        btnDismiss.setText(R.string.btn_dismiss);
        btnDismiss.setTextSize(16);
        btnDismiss.setTextColor(Color.WHITE);
        btnDismiss.setBackgroundResource(R.drawable.btn_pill);
        btnDismiss.setFocusable(true);
        btnDismiss.requestFocus();
        btnDismiss.setOnClickListener(v -> dismiss());
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                (int) (160 * getResources().getDisplayMetrics().density),
                (int) (48 * getResources().getDisplayMetrics().density)
        );
        dLp.rightMargin = (int) (16 * getResources().getDisplayMetrics().density);
        btnRow.addView(btnDismiss, dLp);

        Button btnSnooze = new Button(this);
        btnSnooze.setText(R.string.btn_snooze);
        btnSnooze.setTextSize(16);
        btnSnooze.setTextColor(0xFFFFB74D);
        btnSnooze.setBackgroundResource(R.drawable.btn_pill);
        btnSnooze.setFocusable(true);
        btnSnooze.setOnClickListener(v -> snooze());
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                (int) (180 * getResources().getDisplayMetrics().density),
                (int) (48 * getResources().getDisplayMetrics().density)
        );
        btnRow.addView(btnSnooze, sLp);

        root.addView(btnRow);
        setContentView(root);
    }

    private void startSound(String soundUriStr) {
        Uri uri = null;
        if (soundUriStr != null && !soundUriStr.isEmpty()) {
            try {
                uri = Uri.parse(soundUriStr);
            } catch (Exception ignored) {
            }
        }
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception ignored) {
        }
    }

    private void stopSound() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
    }

    private void dismiss() {
        handler.removeCallbacks(autoDismissRunnable);
        stopSound();
        finish();
    }

    private void snooze() {
        handler.removeCallbacks(autoDismissRunnable);
        stopSound();

        // Schedule snooze for +10 minutes
        Alarm snoozeAlarm = new Alarm(
                (int) (System.currentTimeMillis() % 100000),
                0, 0, "Snooze", true, 0, ""
        );
        // Set exact trigger 10 minutes from now
        AlarmScheduler.schedule(this, snoozeAlarm);

        finish();
    }

    @Override
    public void onBackPressed() {
        dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSound();
        handler.removeCallbacks(autoDismissRunnable);
    }
}
