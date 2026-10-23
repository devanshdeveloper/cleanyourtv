package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * On-TV configuration screen for CleanYourTV.
 * Organized into 7 category pages, visual style matching the active theme,
 * fully navigable via TV D-pad with instant back navigation.
 */
public final class LauncherSettingsActivity extends Activity {

    public static final String EXTRA_REORDER = "reorder";

    private static final int CAT_APPEARANCE = 0;
    private static final int CAT_LAYOUT = 1;
    private static final int CAT_TOP_BAR = 2;
    private static final int CAT_MOTION = 3;
    private static final int CAT_APPS = 4;
    private static final int CAT_BEHAVIOUR = 5;
    private static final int CAT_REMOTE = 6;
    private static final int CAT_COUNT = 7;

    private static final String[][] CATEGORY_KEYS = {
            // CAT_APPEARANCE
            {Config.WALLPAPER_CHOICE_KEY, "wallpaper_dim", "wallpaper_rotation_minutes", "wallpaper_order",
                    "wallpaper_true_color", "wallpaper_captions", "sky_intensity", "ambient_glow",
                    "app_glow", "glow_style", "cinematic_home", "show_stats", "shamsi_date"},
            // CAT_LAYOUT
            {"columns", Config.ROWS_BELOW_DOCK_KEY, Config.DOCK_OPACITY_KEY, "dock_row", "dock_title",
                    "dock_glass", "dock_even_padding", "fill_dock", "always_show_names"},
            // CAT_TOP_BAR
            {"top_menu", "launcher_settings_button", "show_network_pill", "show_inputs_pill",
                    "icon_only_buttons", "icon_only_inputs", "library_button", "reorder_button",
                    "wifi_label_width"},
            // CAT_MOTION
            {"animations", "advanced_effects", "reduced_motion", "focus_overshoot"},
            // CAT_APPS
            {"app_library", "module_cards", "tile_menu", "show_home_settings_shortcut"},
            // CAT_BEHAVIOUR
            {"home_idle_clock_minutes", "boot_to_live_tv", "f1_countdown"},
            // CAT_REMOTE
            {"settings_key_panel", "key_filter", "handle_view_inputs", "restore_key_services", "coocaa_sources"}
    };

    private static final String[] THEME_PRESETS = {
            "default", "aurora", "dussehra", "glass", "scenes", "six-columns"
    };

    private SharedPreferences choices;
    private Config config;
    private ScrollView categoriesScroll;
    private ScrollView subpageScroll;
    private LinearLayout categoriesContainer;
    private LinearLayout subpageItemsContainer;
    private TextView subpageBack;
    private TextView subpageTitle;
    private TextView subpageSubtitle;
    private TextView subpageReset;
    private View lastCategoryView;
    private int currentCategory = -1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);

        choices = getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        config = Config.load(this);

        // Theme dynamic backdrop
        View root = findViewById(R.id.settingsRoot);
        if (root != null) {
            root.setBackgroundColor(config.backgroundColor);
        }

        ImageView wallpaperView = findViewById(R.id.settingsWallpaper);
        if (wallpaperView != null && config.wallpapers != null && !config.wallpapers.isEmpty()) {
            File wp = config.wallpapers.get(0);
            if (wp != null && wp.isFile()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = config.wallpaperTrueColor ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                try {
                    Bitmap bm = BitmapFactory.decodeFile(wp.getAbsolutePath(), opts);
                    if (bm != null) {
                        if (config.wallpaperDim > 0) {
                            Bitmap mutable = bm.copy(opts.inPreferredConfig, true);
                            if (mutable != null) {
                                bm.recycle();
                                bm = mutable;
                                Canvas c = new Canvas(bm);
                                int dimColor = (config.wallpaperDim * 255 / 100) << 24;
                                c.drawColor(dimColor);
                            }
                        }
                        wallpaperView.setImageBitmap(bm);
                        wallpaperView.setVisibility(View.VISIBLE);
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        categoriesScroll = findViewById(R.id.categoriesScroll);
        subpageScroll = findViewById(R.id.subpageScroll);
        categoriesContainer = findViewById(R.id.categoriesContainer);
        subpageItemsContainer = findViewById(R.id.subpageItemsContainer);
        subpageBack = findViewById(R.id.subpageBack);
        subpageTitle = findViewById(R.id.subpageTitle);
        subpageSubtitle = findViewById(R.id.subpageSubtitle);
        subpageReset = findViewById(R.id.subpageReset);

        subpageBack.setOnClickListener(v -> closeCategory());
        subpageReset.setOnClickListener(v -> resetCurrentCategory());

        findViewById(R.id.followThemeChoice).setOnClickListener(v -> followThemeAndResetAll());
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));

        buildCategoriesList();
    }

    @Override
    public void onBackPressed() {
        if (subpageScroll.getVisibility() == View.VISIBLE) {
            closeCategory();
            return;
        }
        super.onBackPressed();
    }

    private void buildCategoriesList() {
        categoriesContainer.removeAllViews();

        addCategoryCard(CAT_APPEARANCE, R.string.category_appearance, R.string.category_appearance_desc, getAppearanceSummary());
        addCategoryCard(CAT_LAYOUT, R.string.category_layout, R.string.category_layout_desc, config.columns + " col · " + config.rowsBelowDock + " rows");
        addCategoryCard(CAT_TOP_BAR, R.string.category_top_bar, R.string.category_top_bar_desc, config.topMenu ? "Gear menu" : "Pill bar");
        addCategoryCard(CAT_MOTION, R.string.category_motion, R.string.category_motion_desc, config.reducedMotion ? "Reduced" : "Normal");
        addCategoryCard(CAT_APPS, R.string.category_apps, R.string.category_apps_desc, config.appLibrary ? "Library on" : "Library off");
        addCategoryCard(CAT_BEHAVIOUR, R.string.category_clock, R.string.category_clock_desc, config.showStats ? "Stats on" : "Stats off");
        addCategoryCard(CAT_REMOTE, R.string.category_remote, R.string.category_remote_desc, config.settingsKeyPanel ? "Quick panel" : "Standard");

        if (categoriesContainer.getChildCount() > 0) {
            categoriesContainer.getChildAt(0).requestFocus();
        }
    }

    private void addCategoryCard(int categoryIndex, int titleRes, int descRes, String summaryValue) {
        View card = getLayoutInflater().inflate(R.layout.item_setting_entry, categoriesContainer, false);
        TextView title = card.findViewById(R.id.entryTitle);
        TextView desc = card.findViewById(R.id.entrySummary);
        TextView val = card.findViewById(R.id.entryValue);

        title.setText(titleRes);
        desc.setText(descRes);
        desc.setVisibility(View.VISIBLE);
        val.setText(summaryValue);

        card.setOnClickListener(v -> openCategory(categoryIndex, v));
        categoriesContainer.addView(card);
    }

    private void openCategory(int categoryIndex, View triggerView) {
        lastCategoryView = triggerView;
        currentCategory = categoryIndex;
        populateCategory(categoryIndex);

        if (!config.animations || config.reducedMotion) {
            categoriesScroll.setVisibility(View.GONE);
            subpageScroll.setVisibility(View.VISIBLE);
        } else {
            categoriesScroll.setVisibility(View.GONE);
            subpageScroll.setAlpha(0f);
            subpageScroll.setVisibility(View.VISIBLE);
            subpageScroll.animate().alpha(1f).setDuration(120).start();
        }
        subpageScroll.scrollTo(0, 0);

        if (subpageItemsContainer.getChildCount() > 0) {
            subpageItemsContainer.getChildAt(0).requestFocus();
        } else {
            subpageBack.requestFocus();
        }
    }

    private void closeCategory() {
        currentCategory = -1;
        config = Config.load(this);
        buildCategoriesList();

        if (!config.animations || config.reducedMotion) {
            subpageScroll.setVisibility(View.GONE);
            categoriesScroll.setVisibility(View.VISIBLE);
        } else {
            subpageScroll.setVisibility(View.GONE);
            categoriesScroll.setAlpha(0f);
            categoriesScroll.setVisibility(View.VISIBLE);
            categoriesScroll.animate().alpha(1f).setDuration(120).start();
        }

        if (lastCategoryView != null) {
            lastCategoryView.requestFocus();
        } else if (categoriesContainer.getChildCount() > 0) {
            categoriesContainer.getChildAt(0).requestFocus();
        }
    }

    private void resetCurrentCategory() {
        if (currentCategory < 0 || currentCategory >= CATEGORY_KEYS.length) return;
        SharedPreferences.Editor editor = choices.edit();
        for (String key : CATEGORY_KEYS[currentCategory]) {
            editor.remove(key);
        }
        editor.apply();
        changed();
        config = Config.load(this);
        populateCategory(currentCategory);
        Toast.makeText(this, R.string.reset_category_defaults, Toast.LENGTH_SHORT).show();
    }

    private void populateCategory(int cat) {
        subpageItemsContainer.removeAllViews();
        config = Config.load(this);

        switch (cat) {
            case CAT_APPEARANCE:
                subpageTitle.setText(R.string.category_appearance);
                subpageSubtitle.setText(R.string.category_appearance_desc);
                populateAppearance();
                break;
            case CAT_LAYOUT:
                subpageTitle.setText(R.string.category_layout);
                subpageSubtitle.setText(R.string.category_layout_desc);
                populateLayout();
                break;
            case CAT_TOP_BAR:
                subpageTitle.setText(R.string.category_top_bar);
                subpageSubtitle.setText(R.string.category_top_bar_desc);
                populateTopBar();
                break;
            case CAT_MOTION:
                subpageTitle.setText(R.string.category_motion);
                subpageSubtitle.setText(R.string.category_motion_desc);
                populateMotion();
                break;
            case CAT_APPS:
                subpageTitle.setText(R.string.category_apps);
                subpageSubtitle.setText(R.string.category_apps_desc);
                populateApps();
                break;
            case CAT_BEHAVIOUR:
                subpageTitle.setText(R.string.category_clock);
                subpageSubtitle.setText(R.string.category_clock_desc);
                populateBehaviour();
                break;
            case CAT_REMOTE:
                subpageTitle.setText(R.string.category_remote);
                subpageSubtitle.setText(R.string.category_remote_desc);
                populateRemote();
                break;
        }
    }

    // --- Category 0: Appearance & Themes ---
    private void populateAppearance() {
        addClickRow(R.string.theme_label, 0, getThemeDisplay(), this::showThemePicker);
        addClickRow(R.string.wallpaper_label, 0, getWallpaperDisplay(), this::showWallpaperPicker);
        addClickRow(R.string.opt_wallpaper_rotation, 0, config.wallpaperRotationMinutes == 0 ? getString(R.string.off) : config.wallpaperRotationMinutes + " min", this::showRotationPicker);
        addClickRow(R.string.opt_wallpaper_order, 0, WallpaperRotation.ORDER_SEQUENTIAL.equals(config.wallpaperOrder) ? getString(R.string.wallpaper_order_sequential) : getString(R.string.wallpaper_order_shuffle), this::showOrderPicker);
        addClickRow(R.string.opt_wallpaper_dim, 0, config.wallpaperDim + "%", this::showDimPicker);
        addToggleRow("wallpaper_true_color", R.string.opt_wallpaper_true_color, 0, config.wallpaperTrueColor);
        addToggleRow("wallpaper_captions", R.string.opt_wallpaper_captions, 0, config.wallpaperCaptions);
        addToggleRow("dock_glass", R.string.opt_dock_glass, 0, config.dockGlass);
        addToggleRow("dock_even_padding", R.string.opt_dock_even_padding, 0, config.dockEvenPadding);
        addClickRow(R.string.dock_opacity_label, 0, config.dockOpacity + "%", this::showDockOpacityPicker);
        addClickRow(R.string.opt_sky_intensity, 0, config.skyIntensity + "%", this::showSkyPicker);
        addToggleRow("cinematic_home", R.string.opt_cinematic_home, 0, config.cinematicHome);
        addToggleRow("ambient_glow", R.string.opt_ambient_glow, 0, config.ambientGlow);
        addToggleRow("app_glow", R.string.opt_app_glow, 0, config.appGlow);
        addClickRow(R.string.opt_glow_style, 0, "horizon".equals(config.glowStyle) ? getString(R.string.glow_horizon) : getString(R.string.glow_radial), this::showGlowStylePicker);
        addToggleRow("show_stats", R.string.opt_show_stats, 0, config.showStats);
        addToggleRow("shamsi_date", R.string.opt_shamsi_date, 0, config.shamsiDate);
    }

    // --- Category 1: Layout & Grid ---
    private void populateLayout() {
        addClickRow(R.string.columns_label, 0, String.valueOf(config.columns), this::showColumnsPicker);
        addClickRow(R.string.rows_below_dock_label, 0, String.valueOf(config.rowsBelowDock), this::showRowsBelowPicker);
        addToggleRow("dock_row", R.string.opt_dock_row, 0, config.dockRow);
        addToggleRow("dock_title", R.string.opt_dock_title, 0, config.dockTitle);
        addToggleRow("fill_dock", R.string.opt_fill_dock, 0, config.fillDock);
        addToggleRow("always_show_names", R.string.opt_always_show_names, 0, config.alwaysShowNames);
        addClickRow(R.string.reorder, 0, "›", () -> {
            Intent data = new Intent().putExtra(EXTRA_REORDER, true);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    // --- Category 2: Top Bar & Header ---
    private void populateTopBar() {
        addToggleRow("top_menu", R.string.opt_top_menu, 0, config.topMenu);
        addToggleRow("launcher_settings_button", R.string.opt_launcher_settings_button, 0, config.launcherSettingsButton);
        addToggleRow("show_network_pill", R.string.opt_show_network_pill, 0, config.showNetworkPill);
        addToggleRow("show_inputs_pill", R.string.opt_show_inputs_pill, 0, config.showInputsPill);
        addToggleRow("icon_only_buttons", R.string.opt_icon_only_buttons, 0, config.iconOnlyButtons);
        addToggleRow("icon_only_inputs", R.string.opt_icon_only_inputs, 0, config.iconOnlyInputs);
        addClickRow(R.string.opt_wifi_label_width, 0, config.wifiLabelWidth + " dp", this::showWifiWidthPicker);
        addToggleRow("reorder_button", R.string.opt_reorder_button, 0, config.reorderButton);
        addToggleRow("library_button", R.string.opt_library_button, 0, config.libraryButton);
    }

    // --- Category 3: Motion & Performance ---
    private void populateMotion() {
        addToggleRow("animations", R.string.animations_label, 0, config.animations);
        addToggleRow("reduced_motion", R.string.opt_reduced_motion, 0, config.reducedMotion);
        addToggleRow("focus_overshoot", R.string.opt_focus_overshoot, 0, config.focusOvershoot);
        addClickRow(R.string.advanced_effects, R.string.advanced_effects_warning,
                config.advancedEffects ? getString(R.string.on) : getString(R.string.off), () -> {
                    if (config.advancedEffects) {
                        choices.edit().putBoolean("advanced_effects", false).apply();
                        changed();
                        populateCategory(CAT_MOTION);
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.advanced_effects)
                                .setMessage(R.string.advanced_effects_warning)
                                .setPositiveButton(R.string.enable, (dialog, which) -> {
                                    choices.edit().putBoolean("advanced_effects", true).apply();
                                    changed();
                                    populateCategory(CAT_MOTION);
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    }
                });
    }

    // --- Category 4: Apps & Library ---
    private void populateApps() {
        addToggleRow("app_library", R.string.opt_app_library, 0, config.appLibrary);
        addToggleRow("module_cards", R.string.opt_module_cards, 0, config.moduleCards);
        addClickRow(R.string.manage_addons, 0, "›", this::showModuleCardsManager);
        addToggleRow("tile_menu", R.string.opt_tile_menu, 0, config.tileMenu);
        addToggleRow("show_home_settings_shortcut", R.string.opt_show_home_settings_shortcut, 0, config.showHomeSettingsShortcut);
        addClickRow(R.string.home_settings, R.string.home_settings_help, "›", () -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        addClickRow(R.string.tv_settings, 0, "›", () -> open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
    }

    // --- Category 5: Behaviour & Clock ---
    private void populateBehaviour() {
        addClickRow(R.string.opt_home_idle_clock, 0, config.homeIdleClockMinutes == 0 ? getString(R.string.off) : config.homeIdleClockMinutes + " min", this::showIdleClockPicker);
        boolean liveTvAvailable = config.liveTvPackage != null && !config.liveTvPackage.isEmpty();
        addTier2ToggleRow("boot_to_live_tv", R.string.opt_boot_to_live_tv, 0, config.bootToLiveTv,
                liveTvAvailable, getString(R.string.disabled_no_live_tv));
        addToggleRow("f1_countdown", R.string.opt_f1_countdown, 0, config.f1Countdown);
    }

    // --- Category 6: Remote & Keys ---
    private void populateRemote() {
        boolean homeKeyActive = isAccessibilityServiceEnabled(HomeKeyService.class);
        addTier2ToggleRow("settings_key_panel", R.string.opt_settings_key_panel, 0, config.settingsKeyPanel,
                homeKeyActive, getString(R.string.disabled_requires_home_key_service));

        boolean filterActive = isAccessibilityServiceEnabled(HomeKeyService.class) || isAccessibilityServiceEnabled(InputKeyService.class);
        addTier2ToggleRow("key_filter", R.string.opt_key_filter, 0, config.keyFilter,
                filterActive, getString(R.string.disabled_requires_accessibility));

        addToggleRow("handle_view_inputs", R.string.opt_handle_view_inputs, 0, config.handleViewInputs);

        boolean writeSecureGranted = checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
        addTier2ToggleRow("restore_key_services", R.string.opt_restore_key_services, 0, config.restoreKeyServices,
                writeSecureGranted, getString(R.string.disabled_requires_secure_settings));

        boolean coocaaInstalled = isPackageInstalled("com.skyworth.tv");
        addTier2ToggleRow("coocaa_sources", R.string.opt_coocaa_sources, 0, config.coocaaSources,
                coocaaInstalled, getString(R.string.disabled_only_coocaa));
    }

    // --- UI Helper Rows ---
    private void addToggleRow(String prefKey, int titleRes, int summaryRes, boolean currentValue) {
        View row = getLayoutInflater().inflate(R.layout.item_setting_entry, subpageItemsContainer, false);
        TextView title = row.findViewById(R.id.entryTitle);
        TextView summary = row.findViewById(R.id.entrySummary);
        TextView value = row.findViewById(R.id.entryValue);

        title.setText(titleRes);
        if (summaryRes != 0) {
            summary.setText(summaryRes);
            summary.setVisibility(View.VISIBLE);
        }
        value.setText(currentValue ? R.string.on : R.string.off);

        row.setOnClickListener(v -> {
            boolean next = !currentValue;
            choices.edit().putBoolean(prefKey, next).apply();
            changed();
            populateCategory(currentCategory);
        });
        subpageItemsContainer.addView(row);
    }

    private void addTier2ToggleRow(String prefKey, int titleRes, int summaryRes, boolean currentValue,
                                   boolean isAvailable, String disabledReason) {
        View row = getLayoutInflater().inflate(R.layout.item_setting_entry, subpageItemsContainer, false);
        TextView title = row.findViewById(R.id.entryTitle);
        TextView summary = row.findViewById(R.id.entrySummary);
        TextView value = row.findViewById(R.id.entryValue);

        title.setText(titleRes);
        if (!isAvailable) {
            summary.setText(disabledReason);
            summary.setVisibility(View.VISIBLE);
            value.setText(R.string.off);
            row.setEnabled(false);
            row.setAlpha(0.5f);
            row.setFocusable(false);
        } else {
            if (summaryRes != 0) {
                summary.setText(summaryRes);
                summary.setVisibility(View.VISIBLE);
            }
            value.setText(currentValue ? R.string.on : R.string.off);
            row.setOnClickListener(v -> {
                boolean next = !currentValue;
                choices.edit().putBoolean(prefKey, next).apply();
                changed();
                populateCategory(currentCategory);
            });
        }
        subpageItemsContainer.addView(row);
    }

    private void addClickRow(int titleRes, int summaryRes, String valueText, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_setting_entry, subpageItemsContainer, false);
        TextView title = row.findViewById(R.id.entryTitle);
        TextView summary = row.findViewById(R.id.entrySummary);
        TextView value = row.findViewById(R.id.entryValue);

        title.setText(titleRes);
        if (summaryRes != 0) {
            summary.setText(summaryRes);
            summary.setVisibility(View.VISIBLE);
        }
        value.setText(valueText);

        if (action != null) {
            row.setOnClickListener(v -> action.run());
        }
        subpageItemsContainer.addView(row);
    }

    private boolean isAccessibilityServiceEnabled(Class<?> serviceClass) {
        try {
            String service = getPackageName() + "/" + serviceClass.getName();
            String enabledServices = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabledServices != null && enabledServices.contains(service);
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isPackageInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // --- Dialog Pickers ---
    private void showThemePicker() {
        String current = choices.getString("theme_preset", "default");
        String[] labels = new String[THEME_PRESETS.length + 1];
        int checked = 0;
        for (int i = 0; i < THEME_PRESETS.length; i++) {
            labels[i] = THEME_PRESETS[i].substring(0, 1).toUpperCase() + THEME_PRESETS[i].substring(1);
            if (THEME_PRESETS[i].equalsIgnoreCase(current)) checked = i;
        }
        labels[THEME_PRESETS.length] = getString(R.string.theme_reset_choice);
        if (choices.getString("theme_preset", null) == null) checked = THEME_PRESETS.length;

        new AlertDialog.Builder(this)
                .setTitle(R.string.theme_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == THEME_PRESETS.length) {
                        choices.edit().remove("theme_preset").apply();
                        File dest = new File(getFilesDir(), Config.FILE_NAME);
                        if (dest.exists()) dest.delete();
                    } else {
                        String chosen = THEME_PRESETS[which];
                        choices.edit().putString("theme_preset", chosen).apply();
                        applyThemePreset(chosen);
                    }
                    changed();
                    populateCategory(CAT_APPEARANCE);
                    Toast.makeText(this, R.string.theme_label, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applyThemePreset(String preset) {
        try {
            File source = new File(getExternalFilesDir(null), "../../themes/" + preset + "/config.json");
            if (!source.exists()) {
                source = new File("/sdcard/Android/data/com.gathin.tvhome/files/config.json");
            }
            File dest = new File(getFilesDir(), Config.FILE_NAME);
            if (source.exists()) {
                byte[] data = java.nio.file.Files.readAllBytes(source.toPath());
                try (FileOutputStream out = new FileOutputStream(dest)) {
                    out.write(data);
                }
            } else {
                String minimalJson = "{\"columns\":4}";
                if ("six-columns".equals(preset)) minimalJson = "{\"columns\":6}";
                else if ("aurora".equals(preset)) minimalJson = "{\"background_color\":\"#0A1118\",\"sky_intensity\":60}";
                else if ("glass".equals(preset)) minimalJson = "{\"dock_glass\":true,\"top_menu\":true}";
                try (FileOutputStream out = new FileOutputStream(dest)) {
                    out.write(minimalJson.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void showColumnsPicker() {
        String[] options = {"4", "5", "6", "7"};
        int checked = Math.max(0, Math.min(config.columns - 4, 3));
        new AlertDialog.Builder(this)
                .setTitle(R.string.columns_label)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putInt("columns", which + 4).apply();
                    changed();
                    populateCategory(CAT_LAYOUT);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showRowsBelowPicker() {
        String[] options = {"0", "1", "2", "3"};
        int checked = Math.max(0, Math.min(config.rowsBelowDock, 3));
        new AlertDialog.Builder(this)
                .setTitle(R.string.rows_below_dock_label)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, which).apply();
                    changed();
                    populateCategory(CAT_LAYOUT);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDockOpacityPicker() {
        int[] values = {100, 70, 50, 30};
        String[] options = {"100%", "70%", "50%", "30%"};
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (config.dockOpacity == values[i]) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dock_opacity_label)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putInt(Config.DOCK_OPACITY_KEY, values[which]).apply();
                    changed();
                    populateCategory(CAT_APPEARANCE);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDimPicker() {
        int[] values = {0, 10, 20, 30, 40, 50, 60};
        String[] options = {"0%", "10%", "20%", "30%", "40%", "50%", "60%"};
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (config.wallpaperDim == values[i]) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.opt_wallpaper_dim)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putInt("wallpaper_dim", values[which]).apply();
                    changed();
                    populateCategory(CAT_APPEARANCE);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showRotationPicker() {
        int[] values = {0, 1, 5, 15, 30, 60};
        String[] options = {getString(R.string.off), "1 min", "5 min", "15 min", "30 min", "60 min"};
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (config.wallpaperRotationMinutes == values[i]) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.opt_wallpaper_rotation)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putInt("wallpaper_rotation_minutes", values[which]).apply();
                    changed();
                    populateCategory(CAT_APPEARANCE);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showOrderPicker() {
        String[] values = {WallpaperRotation.ORDER_SHUFFLE, WallpaperRotation.ORDER_SEQUENTIAL};
        String[] options = {getString(R.string.wallpaper_order_shuffle), getString(R.string.wallpaper_order_sequential)};
        int checked = WallpaperRotation.ORDER_SEQUENTIAL.equals(config.wallpaperOrder) ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle(R.string.opt_wallpaper_order)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putString("wallpaper_order", values[which]).apply();
                    changed();
                    populateCategory(CAT_APPEARANCE);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSkyPicker() {
        int[] values = {0, 20, 40, 60, 80, 100};
        String[] options = {"0%", "20%", "40%", "60%", "80%", "100%"};
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (config.skyIntensity == values[i]) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.opt_sky_intensity)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putInt("sky_intensity", values[which]).apply();
                    changed();
                    populateCategory(CAT_APPEARANCE);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showGlowStylePicker() {
        String[] values = {"radial", "horizon"};
        String[] options = {getString(R.string.glow_radial), getString(R.string.glow_horizon)};
        int checked = "horizon".equals(config.glowStyle) ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle(R.string.opt_glow_style)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putString("glow_style", values[which]).apply();
                    changed();
                    populateCategory(CAT_APPEARANCE);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showIdleClockPicker() {
        int[] values = {0, 5, 10, 15, 30, 60};
        String[] options = {getString(R.string.off), "5 min", "10 min", "15 min", "30 min", "60 min"};
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (config.homeIdleClockMinutes == values[i]) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.opt_home_idle_clock)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putInt("home_idle_clock_minutes", values[which]).apply();
                    changed();
                    populateCategory(CAT_BEHAVIOUR);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showWifiWidthPicker() {
        int[] values = {0, 60, 100, 140, 180};
        String[] options = {"Full / 0 dp", "60 dp", "100 dp", "140 dp", "180 dp"};
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (config.wifiLabelWidth == values[i]) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.opt_wifi_label_width)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putInt("wifi_label_width", values[which]).apply();
                    changed();
                    populateCategory(CAT_TOP_BAR);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size() : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, images.get(which).getName()).apply();
                    }
                    changed();
                    populateCategory(CAT_APPEARANCE);
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    populateCategory(CAT_APPEARANCE);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showModuleCardsManager() {
        List<android.content.pm.ResolveInfo> leanback = getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
        List<Cards.Card> discovered = CardFeature.run(() -> Cards.discover(this, Collections.emptySet(),
                () -> Cards.load(Cards.storeFile(this)), leanback), Collections.emptyList());
        if (discovered == null || discovered.isEmpty() || !CardFeature.working()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.manage_addons)
                    .setMessage(R.string.no_cards_installed)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        List<Cards.Card> listed = new ArrayList<>(discovered);
        Collections.sort(listed, Cards::byLabel);
        String[] items = new String[listed.size()];
        boolean[] checked = new boolean[listed.size()];
        for (int i = 0; i < listed.size(); i++) {
            Cards.Card card = listed.get(i);
            items[i] = card.label + " (" + card.pkg + ")";
            checked[i] = card.state == Cards.State.SHOWN;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.manage_addons)
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    Cards.Card card = listed.get(which);
                    CardFeature.run(() -> Cards.decide(Cards.storeFile(this), card, isChecked), false);
                    changed();
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }


    private void followThemeAndResetAll() {
        SharedPreferences.Editor editor = choices.edit();
        for (String key : Config.ON_TV_KEYS) {
            editor.remove(key);
        }
        editor.commit();
        changed();
        config = Config.load(this);
        buildCategoriesList();
        Toast.makeText(this, R.string.follow_theme, Toast.LENGTH_SHORT).show();
    }

    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private String getAppearanceSummary() {
        String wp = getWallpaperDisplay();
        return "Sky " + config.skyIntensity + "% · " + wp;
    }

    private String getThemeDisplay() {
        String preset = choices.getString("theme_preset", null);
        return preset != null ? preset.substring(0, 1).toUpperCase() + preset.substring(1) : getString(R.string.theme_built_in);
    }

    private String getWallpaperDisplay() {
        String wp = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        if ("none".equals(wp)) return getString(R.string.wallpaper_none);
        if (wp != null) return wp;
        return getString(R.string.wallpaper_theme_default);
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
