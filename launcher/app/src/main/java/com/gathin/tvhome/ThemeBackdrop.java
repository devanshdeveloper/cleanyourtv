package com.gathin.tvhome;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/** Full-screen pages use the same bounded wallpaper decoder and sky as Home. */
final class ThemeBackdrop {
    private final FrameLayout root;
    private final WallpaperPlayer wallpaper;
    private final SkyView sky;
    private final View scrim;
    private boolean started;
    private final Runnable minute = new Runnable() {
        @Override public void run() {
            if (!started) return;
            sky.update();
            wallpaper.update();
            root.postDelayed(this, 60_000L);
        }
    };

    ThemeBackdrop(Context context, FrameLayout root, Config config) {
        this.root = root;
        root.setBackgroundColor(config.backgroundColor);
        wallpaper = new WallpaperPlayer(context, root, config);
        sky = new SkyView(context, null);
        root.addView(sky, config.wallpapers.isEmpty() ? 0 : 1,
                new FrameLayout.LayoutParams(-1, -1));
        sky.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        sky.setIntensity(config.skyIntensity);
        scrim = config.style.scrim(context);
        root.addView(scrim, root.indexOfChild(sky) + 1, new FrameLayout.LayoutParams(-1, -1));
        scrim.setVisibility(config.wallpapers.isEmpty() && config.skyIntensity == 0 ? View.GONE : View.VISIBLE);
    }

    void start() {
        started = true;
        wallpaper.start();
        sky.update();
        root.removeCallbacks(minute);
        root.postDelayed(minute, 60_000L);
    }

    void stop() {
        started = false;
        root.removeCallbacks(minute);
        wallpaper.release();
    }

    void close() {
        stop();
        wallpaper.close();
        root.removeView(sky);
        root.removeView(scrim);
    }
}
