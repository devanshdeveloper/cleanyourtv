package com.gathin.tvhome;

import android.service.dreams.DreamService;

/**
 * Screensaver: the launcher's thin clock and date on black, over a slow ambient glow (ClockScene).
 * No network, no images: cheap enough for a 1 GB TV.
 */
public class ClockDream extends DreamService {

    private ClockScene scene;
    private final android.database.ContentObserver themeChanges = new android.database.ContentObserver(
            new android.os.Handler(android.os.Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) {
            if (scene != null) scene.close();
            scene = new ClockScene(getWindow().getDecorView());
            scene.start();
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_clock);
        scene = new ClockScene(getWindow().getDecorView());
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        getContentResolver().registerContentObserver(android.net.Uri.parse("content://" + getPackageName() + ".theme"), true, themeChanges);
        scene.start();
    }

    @Override
    public void onDreamingStopped() {
        getContentResolver().unregisterContentObserver(themeChanges);
        scene.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (scene != null) scene.close();
        super.onDetachedFromWindow();
    }
}
