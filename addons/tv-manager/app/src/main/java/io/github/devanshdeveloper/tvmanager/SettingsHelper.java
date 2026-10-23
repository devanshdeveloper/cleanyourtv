package io.github.devanshdeveloper.tvmanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * Manages WRITE_SECURE_SETTINGS options with previous value backups and one-tap restore.
 */
public final class SettingsHelper {
    public static final String PERMISSION_WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS";
    private static final String PREF_BACKUPS = "settings_backups";

    private final Context context;
    private final SharedPreferences prefs;

    public SettingsHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_BACKUPS, Context.MODE_PRIVATE);
    }

    public boolean hasPermission() {
        return context.checkCallingOrSelfPermission(PERMISSION_WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    public void setAnimationScale(float scale) {
        if (!hasPermission()) return;
        ContentResolver cr = context.getContentResolver();
        savePreviousGlobal(Settings.Global.WINDOW_ANIMATION_SCALE);
        savePreviousGlobal(Settings.Global.TRANSITION_ANIMATION_SCALE);
        savePreviousGlobal(Settings.Global.ANIMATOR_DURATION_SCALE);

        Settings.Global.putFloat(cr, Settings.Global.WINDOW_ANIMATION_SCALE, scale);
        Settings.Global.putFloat(cr, Settings.Global.TRANSITION_ANIMATION_SCALE, scale);
        Settings.Global.putFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, scale);
    }

    public void setScreensaver(String component) {
        if (!hasPermission()) return;
        savePreviousSecure("screensaver_components");
        Settings.Secure.putString(context.getContentResolver(), "screensaver_components", component);
    }

    public void setPrivateDns(String mode, String specifier) {
        if (!hasPermission()) return;
        ContentResolver cr = context.getContentResolver();
        savePreviousGlobal("private_dns_mode");
        savePreviousGlobal("private_dns_specifier");

        Settings.Global.putString(cr, "private_dns_mode", mode);
        if (specifier != null) {
            Settings.Global.putString(cr, "private_dns_specifier", specifier);
        }
    }

    public boolean restorePreviousSettings() {
        if (!hasPermission()) return false;
        ContentResolver cr = context.getContentResolver();
        boolean restored = false;

        if (prefs.contains(Settings.Global.WINDOW_ANIMATION_SCALE)) {
            Settings.Global.putFloat(cr, Settings.Global.WINDOW_ANIMATION_SCALE,
                    prefs.getFloat(Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f));
            Settings.Global.putFloat(cr, Settings.Global.TRANSITION_ANIMATION_SCALE,
                    prefs.getFloat(Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f));
            Settings.Global.putFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE,
                    prefs.getFloat(Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f));
            restored = true;
        }

        if (prefs.contains("screensaver_components")) {
            Settings.Secure.putString(cr, "screensaver_components",
                    prefs.getString("screensaver_components", null));
            restored = true;
        }

        if (prefs.contains("private_dns_mode")) {
            Settings.Global.putString(cr, "private_dns_mode",
                    prefs.getString("private_dns_mode", "off"));
            Settings.Global.putString(cr, "private_dns_specifier",
                    prefs.getString("private_dns_specifier", ""));
            restored = true;
        }

        return restored;
    }

    private void savePreviousGlobal(String key) {
        if (!prefs.contains(key)) {
            String val = Settings.Global.getString(context.getContentResolver(), key);
            try {
                float fVal = Float.parseFloat(val);
                prefs.edit().putFloat(key, fVal).apply();
            } catch (Exception e) {
                prefs.edit().putString(key, val).apply();
            }
        }
    }

    private void savePreviousSecure(String key) {
        if (!prefs.contains(key)) {
            String val = Settings.Secure.getString(context.getContentResolver(), key);
            prefs.edit().putString(key, val).apply();
        }
    }
}
