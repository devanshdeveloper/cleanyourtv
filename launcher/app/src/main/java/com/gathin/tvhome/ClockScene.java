package com.gathin.tvhome;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Random;

/**
 * The clock screensaver's scene (layout dream_clock): the thin clock and date on black over a slow ambient
 * glow. Every minute the clock drifts to a new spot and the glow shifts colour, so nothing burns in.
 * Shared by ClockDream (the system screensaver) and ClockActivity (Home left idle).
 */
final class ClockScene {

    private static final long MOVE_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final View content;
    private final GradientDrawable glow;
    private int glowColor;
    private final Config config;
    private final ThemeBackdrop backdrop;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            shiftGlow();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    /** root is the inflated dream_clock layout. */
    ClockScene(View root) {
        config = Config.load(root.getContext());
        content = root.findViewById(R.id.dreamContent);
        FrameLayout sceneRoot = (FrameLayout) content.getParent();
        backdrop = new ThemeBackdrop(root.getContext(), sceneRoot, config);
        config.style.watch(sceneRoot);
        View ambient = root.findViewById(R.id.dreamAmbient);
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                glow.setGradientRadius((r - l) * 0.55f));
        setGlow(config.style.palette.glow);
    }

    void start() {
        backdrop.start();
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(duration(1200)).start();
        });
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, MOVE_MS);
    }

    void stop() {
        backdrop.stop();
        handler.removeCallbacksAndMessages(null);   // the tick and any glow steps still queued
        content.animate().cancel();
    }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(duration(900)).withLayer().withEndAction(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(duration(1200)).withLayer().start();
        }).start();
    }

    /** A random spot that keeps the whole clock on screen, with the glow centred behind it. */
    private void placeClock() {
        View parent = (View) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());
        float x = maxX * (0.1f + 0.8f * random.nextFloat());
        float y = maxY * (0.1f + 0.8f * random.nextFloat());
        content.setX(x);
        content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
        }
    }

    private void shiftGlow() {
        setGlow(config.style.palette.glow);
    }

    private long duration(int normal) {
        return !config.animations ? 0 : config.reducedMotion ? Motion.REDUCED_MS : normal;
    }

    void close() { stop(); backdrop.close(); }

    private void setGlow(int color) {
        glowColor = color;
        glow.setColors(new int[]{ThemePalette.alpha(color, 64), ThemePalette.alpha(color, 20), ThemePalette.alpha(color, 0)});
    }
}
