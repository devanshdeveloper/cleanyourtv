package com.gathin.tvhome;

import java.util.HashMap;
import java.util.Map;

/** Runs the production palette with JSON values supplied by Python, without Android stubs. */
public final class ThemeProbe {
    public static void main(String[] args) {
        int background = ThemePalette.color(args[0], ThemePalette.BLACK);
        Map<String, Object> values = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            String[] pair = args[i].split("=", 2);
            String[] typed = pair[1].split(":", 2);
            Object value = "int".equals(typed[0]) ? Double.valueOf(typed[1])
                    : "bool".equals(typed[0]) ? Boolean.valueOf(typed[1]) : typed[1];
            values.put(pair[0], value);
        }
        ThemePalette palette = ThemePalette.resolve(new HashMap<>(), values, new HashMap<>(), background);
        if (ThemePalette.contrast(palette.text, background) < 4.5
                || ThemePalette.contrast(palette.secondary, background) < 4.5
                || ThemePalette.contrast(ThemePalette.ink(palette.surface), palette.surface) < 4.5
                || ThemePalette.contrast(ThemePalette.ink(palette.focus), palette.focus) < 4.5) {
            throw new AssertionError("contrast below 4.5:1");
        }
        System.out.println(palette.radius + ":" + palette.iconShape + ":" + palette.backplate);
    }
}
