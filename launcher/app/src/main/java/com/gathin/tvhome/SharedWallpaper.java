package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Reference-counted images shared by Home and full-screen launcher pages. */
final class SharedWallpaper {
    private static final Map<String, Entry> IMAGES = new HashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final class Entry {
        final Bitmap bitmap;
        int users;
        Entry(Bitmap bitmap) { this.bitmap = bitmap; }
    }

    static synchronized Bitmap acquire(String key, Supplier<Bitmap> decode) {
        Entry entry = IMAGES.get(key);
        if (entry == null) {
            Bitmap bitmap = decode.get();
            if (bitmap == null) return null;
            entry = new Entry(bitmap);
            IMAGES.put(key, entry);
        }
        entry.users++;
        return entry.bitmap;
    }

    static synchronized void release(Bitmap bitmap) {
        if (bitmap == null) return;
        for (Map.Entry<String, Entry> item : IMAGES.entrySet()) {
            Entry entry = item.getValue();
            if (entry.bitmap != bitmap) continue;
            if (--entry.users == 0) {
                String key = item.getKey();
                // Covers an activity handoff and the RenderThread's last frame using the old texture.
                MAIN.postDelayed(() -> {
                    synchronized (SharedWallpaper.class) {
                        if (entry.users == 0 && IMAGES.get(key) == entry) {
                            IMAGES.remove(key);
                            entry.bitmap.recycle();
                        }
                    }
                }, 500);
            }
            return;
        }
    }
}
