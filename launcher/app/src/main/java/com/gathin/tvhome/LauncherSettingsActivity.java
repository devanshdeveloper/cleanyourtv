package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;

    private static final class ToggleRow {
        final TextView view;
        final String key;
        final int titleRes;
        boolean fallback;

        ToggleRow(TextView view, String key, int titleRes, boolean fallback) {
            this.view = view;
            this.key = key;
            this.titleRes = titleRes;
            this.fallback = fallback;
        }
    }

    private static final class IntCycleRow {
        final TextView view;
        final String key;
        final int titleRes;
        final int[] values;
        final String unit;
        int fallback;

        IntCycleRow(TextView view, String key, int titleRes, int[] values, String unit, int fallback) {
            this.view = view;
            this.key = key;
            this.titleRes = titleRes;
            this.values = values;
            this.unit = unit;
            this.fallback = fallback;
        }
    }

    private final List<ToggleRow> toggles = new ArrayList<>();
    private final List<IntCycleRow> intCycles = new ArrayList<>();
    private TextView glowStyleChoice;
    private String glowStyleFallback;
    private LinearLayout container;
    private LayoutInflater inflater;

    private static final int[] SKY_INTENSITIES = {0, 25, 50, 75, 100};
    private static final int[] IDLE_CLOCK_MINUTES = {0, 5, 10, 15, 30};
    private static final int[] WIFI_LABEL_WIDTHS = {0, 60, 90, 120, 150};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        container = findViewById(R.id.additionalSettingsContainer);
        inflater = getLayoutInflater();

        addHeader(R.string.settings_section_look);
        addToggle("dock_glass", R.string.opt_dock_glass, config.dockGlass);
        addToggle("dock_even_padding", R.string.opt_dock_even_padding, config.dockEvenPadding);
        addToggle("dock_title", R.string.opt_dock_title, config.dockTitle);
        addToggle("always_show_names", R.string.opt_always_show_names, config.alwaysShowNames);
        addToggle("wallpaper_true_color", R.string.opt_wallpaper_true_color, config.wallpaperTrueColor);
        addToggle("wallpaper_captions", R.string.opt_wallpaper_captions, config.wallpaperCaptions);
        addToggle("cinematic_home", R.string.opt_cinematic_home, config.cinematicHome);
        addToggle("ambient_glow", R.string.opt_ambient_glow, config.ambientGlow);
        addToggle("app_glow", R.string.opt_app_glow, config.appGlow);
        addGlowStyleRow(config.glowStyle);
        addIntCycle("sky_intensity", R.string.opt_sky_intensity, SKY_INTENSITIES, "%", config.skyIntensity);
        addIntCycle("home_idle_clock_minutes", R.string.opt_home_idle_clock, IDLE_CLOCK_MINUTES, "m", config.homeIdleClockMinutes);

        addHeader(R.string.settings_section_dock_top);
        addToggle("fill_dock", R.string.opt_fill_dock, config.fillDock);
        addToggle("dock_row", R.string.opt_dock_row, config.dockRow);
        addToggle("top_menu", R.string.opt_top_menu, config.topMenu);
        addToggle("show_network_pill", R.string.opt_show_network_pill, config.showNetworkPill);
        addToggle("show_inputs_pill", R.string.opt_show_inputs_pill, config.showInputsPill);
        addToggle("icon_only_buttons", R.string.opt_icon_only_buttons, config.iconOnlyButtons);
        addToggle("icon_only_inputs", R.string.opt_icon_only_inputs, config.iconOnlyInputs);
        addToggle("library_button", R.string.opt_library_button, config.libraryButton);
        addIntCycle("wifi_label_width", R.string.opt_wifi_label_width, WIFI_LABEL_WIDTHS, " dp", config.wifiLabelWidth);
        addToggle("launcher_settings_button", R.string.opt_launcher_settings_button, config.launcherSettingsButton);
        addToggle("reorder_button", R.string.opt_reorder_button, config.reorderButton);
        addToggle("show_home_settings_shortcut", R.string.opt_show_home_settings_shortcut, config.showHomeSettingsShortcut);

        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            SharedPreferences.Editor editor = choices.edit();
            for (String key : Config.ON_TV_KEYS) editor.remove(key);
            editor.commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            updateFallbacks(current);
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        followThemeChoice.setSelected(!hasOnTvChoices());
        for (ToggleRow row : toggles) {
            boolean val = choices.getBoolean(row.key, row.fallback);
            row.view.setText(getString(R.string.setting_state, getString(row.titleRes),
                    getString(val ? R.string.on : R.string.off)));
        }
        for (IntCycleRow row : intCycles) {
            int val = choices.getInt(row.key, row.fallback);
            String label = val == 0 ? getString(R.string.off) : (val + row.unit);
            row.view.setText(getString(R.string.setting_state, getString(row.titleRes), label));
        }
        if (glowStyleChoice != null) {
            String glow = choices.getString("glow_style", glowStyleFallback);
            glowStyleChoice.setText(getString(R.string.setting_state, getString(R.string.opt_glow_style),
                    Config.GLOW_HORIZON.equalsIgnoreCase(glow) ? getString(R.string.glow_horizon) : getString(R.string.glow_radial)));
        }
    }

    private void addHeader(int titleRes) {
        TextView tv = (TextView) inflater.inflate(R.layout.settings_section_header, container, false);
        tv.setText(titleRes);
        container.addView(tv);
    }

    private void addToggle(String key, int titleRes, boolean fallback) {
        TextView tv = (TextView) inflater.inflate(R.layout.settings_row, container, false);
        ToggleRow row = new ToggleRow(tv, key, titleRes, fallback);
        toggles.add(row);
        tv.setOnClickListener(v -> {
            boolean current = choices.getBoolean(row.key, row.fallback);
            choices.edit().putBoolean(row.key, !current).apply();
            changed();
            refresh();
        });
        container.addView(tv);
    }

    private void addGlowStyleRow(String fallback) {
        TextView tv = (TextView) inflater.inflate(R.layout.settings_row, container, false);
        glowStyleChoice = tv;
        glowStyleFallback = fallback;
        glowStyleChoice.setOnClickListener(v -> {
            String cur = choices.getString("glow_style", glowStyleFallback);
            String next = Config.GLOW_HORIZON.equalsIgnoreCase(cur) ? Config.GLOW_RADIAL : Config.GLOW_HORIZON;
            choices.edit().putString("glow_style", next).apply();
            changed();
            refresh();
        });
        container.addView(tv);
    }

    private void addIntCycle(String key, int titleRes, int[] values, String unit, int fallback) {
        TextView tv = (TextView) inflater.inflate(R.layout.settings_row, container, false);
        IntCycleRow row = new IntCycleRow(tv, key, titleRes, values, unit, fallback);
        intCycles.add(row);
        tv.setOnClickListener(v -> {
            int current = choices.getInt(row.key, row.fallback);
            int idx = -1;
            for (int i = 0; i < row.values.length; i++) {
                if (row.values[i] == current) {
                    idx = i;
                    break;
                }
            }
            int next = row.values[(idx + 1) % row.values.length];
            choices.edit().putInt(row.key, next).apply();
            changed();
            refresh();
        });
        container.addView(tv);
    }

    private void updateFallbacks(Config current) {
        for (ToggleRow row : toggles) {
            if ("dock_glass".equals(row.key)) row.fallback = current.dockGlass;
            else if ("dock_even_padding".equals(row.key)) row.fallback = current.dockEvenPadding;
            else if ("dock_title".equals(row.key)) row.fallback = current.dockTitle;
            else if ("always_show_names".equals(row.key)) row.fallback = current.alwaysShowNames;
            else if ("wallpaper_true_color".equals(row.key)) row.fallback = current.wallpaperTrueColor;
            else if ("wallpaper_captions".equals(row.key)) row.fallback = current.wallpaperCaptions;
            else if ("cinematic_home".equals(row.key)) row.fallback = current.cinematicHome;
            else if ("ambient_glow".equals(row.key)) row.fallback = current.ambientGlow;
            else if ("app_glow".equals(row.key)) row.fallback = current.appGlow;
            else if ("fill_dock".equals(row.key)) row.fallback = current.fillDock;
            else if ("dock_row".equals(row.key)) row.fallback = current.dockRow;
            else if ("top_menu".equals(row.key)) row.fallback = current.topMenu;
            else if ("show_network_pill".equals(row.key)) row.fallback = current.showNetworkPill;
            else if ("show_inputs_pill".equals(row.key)) row.fallback = current.showInputsPill;
            else if ("icon_only_buttons".equals(row.key)) row.fallback = current.iconOnlyButtons;
            else if ("icon_only_inputs".equals(row.key)) row.fallback = current.iconOnlyInputs;
            else if ("library_button".equals(row.key)) row.fallback = current.libraryButton;
            else if ("launcher_settings_button".equals(row.key)) row.fallback = current.launcherSettingsButton;
            else if ("reorder_button".equals(row.key)) row.fallback = current.reorderButton;
            else if ("show_home_settings_shortcut".equals(row.key)) row.fallback = current.showHomeSettingsShortcut;
        }
        glowStyleFallback = current.glowStyle;
        for (IntCycleRow row : intCycles) {
            if ("sky_intensity".equals(row.key)) row.fallback = current.skyIntensity;
            else if ("home_idle_clock_minutes".equals(row.key)) row.fallback = current.homeIdleClockMinutes;
            else if ("wifi_label_width".equals(row.key)) row.fallback = current.wifiLabelWidth;
        }
    }

    private boolean hasOnTvChoices() {
        for (String key : Config.ON_TV_KEYS) {
            if (choices.contains(key)) return true;
        }
        return false;
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
