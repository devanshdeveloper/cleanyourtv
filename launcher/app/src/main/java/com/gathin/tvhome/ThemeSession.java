package com.gathin.tvhome;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Process cache with explicit input identity. Never retains an Activity or View. */
final class ThemeSession {
    private static Config current;
    private static String identity;
    private static Map<String, ?> choices;
    static synchronized void invalidate() { current = null; identity = null; choices = null; }

    static synchronized Config config(Context context) {
        Context app = context.getApplicationContext();
        String key = key(app);
        Map<String, ?> now = app.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getAll();
        if (current == null || !key.equals(identity) || !ThemeChoiceRules.sameChoices(choices, now)) {
            current = Config.resolve(app);
            // Resolution may clear choices and record the new theme signature. Cache that final state.
            identity = key(app);
            choices = new HashMap<>(app.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getAll());
        }
        return current;
    }

    private static String key(Context app) {
        File dir = Config.dir(app);
        return Environment.getExternalStorageState() + ":" + stamp(dir, Config.FILE_NAME)
                + ":" + stamp(dir, "theme_revision");
    }

    private static String stamp(File directory, String name) {
        File file = directory == null ? null : new File(directory, name);
        return file != null && file.isFile() ? file.length() + "@" + file.lastModified() : "absent";
    }
}
