package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** One resolved palette for all launcher-owned views, including dynamically added rows. */
final class ThemeStyle {
    final ThemePalette palette;
    final boolean customGlow;
    private final float density;
    private final Map<View, Boolean> styled = new WeakHashMap<>();

    ThemeStyle(Context context, JSONObject json, int background) {
        Map<String, Object> values = new HashMap<>();
        String[] colors = {"surface_color", "text_color", "text_secondary_color", "accent_color",
                "focus_color", "glow_color", "icon_shape", "tile_backplate"};
        int[] resources = {R.string.surface_color, R.string.text_color, R.string.text_secondary_color,
                R.string.accent_color, R.string.focus_color, R.string.glow_color,
                R.string.icon_shape, R.string.tile_backplate};
        for (int i = 0; i < colors.length; i++) values.put(colors[i], context.getString(resources[i]));
        values.put("corner_radius", context.getResources().getInteger(R.integer.corner_radius));
        values.put("pill_radius", context.getResources().getInteger(R.integer.pill_radius));
        values.put("spacing", context.getResources().getInteger(R.integer.spacing));
        Map<String, Object> overlay = new HashMap<>();
        if (json != null) for (String key : ThemePalette.KEYS) overlay.put(key, json.opt(key));
        palette = ThemePalette.resolve(values, overlay,
                context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getAll(), background);
        customGlow = json != null && ThemePalette.color(json.opt("glow_color"), 0) != 0;
        density = context.getResources().getDisplayMetrics().density;
    }

    static ThemeStyle get(Context context) { return Config.load(context).style; }
    int dp(int value) { return Math.round(value * density); }

    GradientDrawable surface(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(Math.max(1, dp(1)), ThemePalette.alpha(palette.text, 40));
        return drawable;
    }

    Drawable choice(boolean pill) {
        StateListDrawable drawable = new StateListDrawable();
        float radius = dp(pill ? palette.pillRadius : palette.radius);
        drawable.addState(new int[]{android.R.attr.state_focused}, surface(palette.focus, radius));
        drawable.addState(new int[]{android.R.attr.state_selected}, surface(palette.accent, radius));
        drawable.addState(new int[]{}, surface(palette.surface, radius));
        return drawable;
    }

    Drawable tile() {
        int fill = "none".equals(palette.backplate) ? 0
                : "glass".equals(palette.backplate) ? ThemePalette.alpha(palette.surface, 180) : palette.surface;
        return surface(fill, dp(palette.radius));
    }

    View scrim(Context context) {
        View scrim = new View(context) { };
        scrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        scrim.setBackgroundColor(ThemePalette.alpha(palette.background,
                Math.max(ThemePalette.scrimAlpha(palette.background, palette.text),
                        ThemePalette.scrimAlpha(palette.background, palette.secondary))));
        return scrim;
    }

    Drawable check(Context context) {
        Drawable mark = context.getDrawable(R.drawable.library_check).mutate();
        mark.setTint(ThemePalette.ink(palette.accent));
        return new android.graphics.drawable.LayerDrawable(new Drawable[]{
                surface(palette.accent, dp(palette.radius)), mark});
    }

    /** A layout callback only styles newly inserted views, not every animation frame. */
    void watch(View root) {
        apply(root);
        Object previous = root.getTag(R.id.theme_style_watcher);
        if (previous instanceof android.view.ViewTreeObserver.OnGlobalLayoutListener) {
            root.getViewTreeObserver().removeOnGlobalLayoutListener(
                    (android.view.ViewTreeObserver.OnGlobalLayoutListener) previous);
        }
        android.view.ViewTreeObserver.OnGlobalLayoutListener listener = () -> apply(root);
        root.setTag(R.id.theme_style_watcher, listener);
        root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    void apply(View view) { apply(view, false); }

    private void apply(View view, boolean appTile) { apply(view, appTile, palette.background); }

    private void apply(View view, boolean appTile, int backdrop) {
        int id = view.getId();
        appTile |= id == R.id.card;
        if (appTile || view.isFocusable() && view.getBackground() != null) backdrop = palette.surface;
        else if (view.getBackground() instanceof GradientDrawable) {
            ColorStateList fill = ((GradientDrawable) view.getBackground()).getColor();
            if (fill != null && (fill.getDefaultColor() >>> 24) == 255) backdrop = fill.getDefaultColor();
        }
        if (!styled.containsKey(view)) {
            styled.put(view, true);
            if (view instanceof TextView) {
                TextView text = (TextView) view;
                boolean secondary = id == R.id.status || id == R.id.entrySummary || id == R.id.subpageSubtitle
                        || id == R.id.mainSubtitle || id == R.id.stats;
                int proposed = secondary ? palette.secondary : palette.text;
                int ink = ThemePalette.contrast(proposed, backdrop) >= 4.5 ? proposed : ThemePalette.ink(backdrop);
                ColorStateList colors = new ColorStateList(new int[][]{{android.R.attr.state_focused},
                        {android.R.attr.state_selected}, {}},
                        new int[]{ThemePalette.ink(palette.focus), ThemePalette.ink(palette.accent), ink});
                text.setTextColor(view.isFocusable() || view.isDuplicateParentStateEnabled()
                        ? colors : ColorStateList.valueOf(ink));
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    int weight = text.getTypeface() != null ? text.getTypeface().getWeight() : 400;
                    int family = weight <= 250 ? R.string.font_extralight : weight <= 350 ? R.string.font_light
                            : weight >= 500 ? R.string.font_medium : R.string.font_regular;
                    text.setTypeface(UiFont.get(view.getContext(), family));
                } // API 26/27 retain their XML/UiFont weight; Typeface.getWeight is not available there.
                text.setCompoundDrawableTintList(colors);
                if (text instanceof android.widget.CheckedTextView) {
                    int mark = ThemePalette.contrast(palette.accent, backdrop) >= 3 ? palette.accent : ink;
                    ((android.widget.CheckedTextView) text).setCheckMarkTintList(ColorStateList.valueOf(mark));
                }
            }
            if (view instanceof android.widget.AbsListView) {
                // Native dialog rows keep matching ink; selection is an outlined surface, not an OEM accent fill.
                GradientDrawable selector = surface(palette.surface, dp(palette.radius));
                int edge = ThemePalette.contrast(palette.accent, palette.surface) >= 3
                        ? palette.accent : ThemePalette.ink(palette.surface);
                selector.setStroke(Math.max(1, dp(2)), edge);
                ((android.widget.AbsListView) view).setSelector(selector);
            }
            if (view instanceof ImageView && !appTile && id == R.id.icon
                    && !Boolean.TRUE.equals(view.getTag(R.id.theme_style_watcher))) {
                int ink = ThemePalette.contrast(palette.text, backdrop) >= 4.5 ? palette.text : ThemePalette.ink(backdrop);
                ((ImageView) view).setImageTintList(new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}},
                        new int[]{ThemePalette.ink(palette.focus), ink}));
            }
            if (view.isFocusable() && !appTile && view.getBackground() != null) {
                view.setBackground(choice(view instanceof TextView));
            } else if (!appTile && view.getBackground() != null
                    && !Boolean.TRUE.equals(view.getTag(R.id.theme_style_watcher))
                    && view.getBackground().getClass().getSimpleName().equals("LayerDrawable")) {
                view.setBackground(surface(palette.surface, dp(palette.radius)));
            }
            if (id == R.id.dot && !Boolean.TRUE.equals(view.getTag(R.id.theme_style_watcher))) {
                view.setBackground(surface(palette.accent, dp(palette.radius)));
            }
            if (view.getClass() == View.class && view.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
                int alpha = ((android.graphics.drawable.ColorDrawable) view.getBackground()).getColor() >>> 24;
                view.setBackgroundColor(ThemePalette.alpha(palette.text, alpha));
            }
            if (view instanceof GlassView) ((GlassView) view).setTheme(this);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) apply(group.getChildAt(i), appTile, backdrop);
        }
    }

    /** Popup backgrounds are opaque enough to keep contrast over any wallpaper. */
    void panel(View root) {
        root.setBackground(surface(palette.surface, dp(palette.radius)));
        root.setPaddingRelative(dp(palette.spacing), root.getPaddingTop(), dp(palette.spacing), root.getPaddingBottom());
        watch(root);
    }

    static AlertDialog.Builder dialog(Context context) {
        return new AlertDialog.Builder(context) {
            @Override public AlertDialog create() {
                AlertDialog dialog = super.create();
                dialog.setOnShowListener(d -> {
                    if (dialog.getWindow() != null) {
                        View root = dialog.getWindow().getDecorView();
                        get(context).panel(root);
                        dialogFont(root);
                    }
                });
                return dialog;
            }
        };
    }

    private static void dialogFont(View view) {
        if (view instanceof TextView) ((TextView) view).setTypeface(UiFont.get(view.getContext(), R.string.font_regular));
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) dialogFont(group.getChildAt(i));
        }
    }
}
