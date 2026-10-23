package com.gathin.tvhome;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Presets bundled directly from themes/. Never rewrites a pushed theme. */
final class ThemePresets {
    static JSONObject read(Context context, String name, JSONObject fallback) {
        if (!name.matches("default|aurora|dussehra|glass|scenes|six-columns")) return fallback;
        try (InputStream in = context.getAssets().open("themes/" + name + "/config.json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception failure) {
            Log.w("TvHome.Theme", "Could not read preset " + name, failure);
            return fallback;
        }
    }

    static File directory(Context context, String name) {
        if (!name.matches("default|aurora|dussehra|glass|scenes|six-columns")) return null;
        File directory = new File(context.getFilesDir(), "theme-presets/" + name);
        try {
            long updated = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
            File ready = new File(directory, "ready");
            if (!ready.isFile() || ready.lastModified() != updated) {
                extract(context, "themes/" + name, directory);
                if (!ready.exists() && !ready.createNewFile()) throw new IOException("preset marker");
                if (!ready.setLastModified(updated)) throw new IOException("preset revision");
            }
            return directory;
        } catch (Exception failure) {
            Log.w("TvHome.Theme", "Could not prepare preset " + name, failure);
            return null;
        }
    }

    private static void extract(Context context, String asset, File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("preset directory");
        String[] names = context.getAssets().list(asset);
        if (names == null) throw new IOException("preset listing");
        java.util.Set<String> present = new java.util.HashSet<>(java.util.Arrays.asList(names));
        File[] previous = directory.listFiles();
        if (previous != null) for (File file : previous) {
            if (file.isFile() && !present.contains(file.getName())
                    && (ThemeFiles.collectionImage(file.getName()) || "captions.txt".equals(file.getName()))
                    && !file.delete()) throw new IOException("stale preset image");
        }
        for (String name : names) {
            if ("wallpapers".equals(name)) {
                extract(context, asset + "/" + name, new File(directory, name));
            } else if ("wallpaper.jpg".equals(name) || "captions.txt".equals(name)
                    || ThemeFiles.collectionImage(name)) {
                try (InputStream in = context.getAssets().open(asset + "/" + name);
                     FileOutputStream out = new FileOutputStream(new File(directory, name))) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                }
            }
        }
    }
}
