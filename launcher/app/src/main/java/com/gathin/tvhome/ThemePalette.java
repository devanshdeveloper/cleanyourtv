package com.gathin.tvhome;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/** Pure value resolution and WCAG contrast maths; no Android or bitmap allocation. */
final class ThemePalette {
    static final String[] KEYS = {"surface_color", "text_color", "text_secondary_color", "accent_color",
            "focus_color", "glow_color", "corner_radius", "pill_radius", "spacing", "icon_shape", "tile_backplate"};
    static final int BLACK = 0xFF000000, WHITE = 0xFFFFFFFF;
    final int background, surface, text, secondary, accent, focus, glow;
    final int radius, pillRadius, spacing;
    final String iconShape, backplate;

    static ThemePalette resolve(Map<String, Object> defaults, Map<String, Object> theme,
                                Map<String, ?> choices, int background) {
        Map<String, Object> values = new HashMap<>(defaults);
        overlay(values, theme);
        overlay(values, choices);
        return new ThemePalette(values, background);
    }

    private static void overlay(Map<String, Object> values, Map<String, ?> layer) {
        for (String key : KEYS) {
            Object value = layer.get(key);
            boolean valid;
            if (key.endsWith("_color")) valid = "auto".equals(value) || color(value, 0) != 0;
            else if ("icon_shape".equals(key)) valid = option(value, "", "rounded", "circle", "squircle", "square").length() > 0;
            else if ("tile_backplate".equals(key)) valid = option(value, "", "none", "glass", "solid").length() > 0;
            else valid = integer(value, -1, 0, 48) >= 0;
            if (valid) values.put(key, value);
        }
    }

    ThemePalette(Map<String, Object> values, int background) {
        this.background = background;
        boolean light = luminance(background) > 0.179;
        surface = color(values.get("surface_color"), light ? 0xFFF1F1F3 : 0xFF1C1C1E);
        text = readable(color(values.get("text_color"), light ? BLACK : WHITE), surface, background);
        secondary = readable(color(values.get("text_secondary_color"), text), surface, background);
        accent = color(values.get("accent_color"), 0xFF6EC1FF);
        focus = color(values.get("focus_color"), light ? 0xFF202024 : WHITE);
        glow = color(values.get("glow_color"), accent);
        radius = integer(values.get("corner_radius"), 16, 0, 48);
        pillRadius = integer(values.get("pill_radius"), 21, 0, 48);
        spacing = integer(values.get("spacing"), 12, 0, 48);
        iconShape = option(values.get("icon_shape"), "square", "rounded", "circle", "squircle", "square");
        backplate = option(values.get("tile_backplate"), "solid", "none", "glass", "solid");
    }

    static int integer(Object value, int fallback, int min, int max) {
        if (!(value instanceof Number)) return fallback;
        double number = ((Number) value).doubleValue();
        return Double.isFinite(number) && number == Math.rint(number) && number >= min && number <= max
                ? (int) number : fallback;
    }

    static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    static String string(Object value, String fallback) {
        return value instanceof String ? ((String) value).trim() : fallback;
    }

    static List<String> strings(Object[] values, List<String> fallback) {
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof String)) return fallback;
            String trimmed = ((String) value).trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    static int[] keycodes(Object[] values, int[] fallback) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = integer(values[i], -1, 1, Integer.MAX_VALUE);
            if (result[i] < 1) return fallback;
        }
        return result;
    }

    static int color(Object value, int fallback) {
        if (!(value instanceof String)) return fallback;
        String hex = ((String) value).trim();
        if (!hex.matches("#[0-9a-fA-F]{6}")) return fallback;
        return BLACK | Integer.parseInt(hex.substring(1), 16);
    }

    static String option(Object value, String fallback, String... allowed) {
        if (value instanceof String) for (String option : allowed) {
            if (option.equals(value)) return option;
        }
        return fallback;
    }

    static int alpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    private static double linear(int channel) {
        double s = channel / 255.0;
        return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    static double luminance(int color) {
        return 0.2126 * linear((color >> 16) & 255) + 0.7152 * linear((color >> 8) & 255)
                + 0.0722 * linear(color & 255);
    }

    static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    static int ink(int background) {
        return contrast(BLACK, background) > contrast(WHITE, background) ? BLACK : WHITE;
    }

    int textOn(int backdrop) {
        return contrast(text, backdrop) >= 4.5 ? text : ink(backdrop);
    }

    static int composite(int foreground, int background, int alpha) {
        int out = BLACK;
        for (int shift : new int[]{16, 8, 0}) {
            int f = (foreground >> shift) & 255, b = (background >> shift) & 255;
            out |= ((f * alpha + b * (255 - alpha)) / 255) << shift;
        }
        return out;
    }

    /** Minimum matching scrim that keeps ink readable even over a black or white photograph. */
    static int scrimAlpha(int background, int text) {
        for (int alpha = 0; alpha <= 255; alpha++) {
            if (contrast(text, composite(background, BLACK, alpha)) >= 4.5
                    && contrast(text, composite(background, WHITE, alpha)) >= 4.5) return alpha;
        }
        return 255;
    }

    private static int readable(int proposed, int surface, int background) {
        if (contrast(proposed, surface) >= 4.5 && contrast(proposed, background) >= 4.5) return proposed;
        return ink(background);
    }
}
