package com.gathin.tvhome;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class ThemePaletteTest {
    @Test public void equalHashesCannotHideAChoiceChange() {
        Map<String, Object> before = new HashMap<>(), after = new HashMap<>();
        before.put("Aa", 1);
        after.put("BB", 1);
        assertEquals(before.hashCode(), after.hashCode());
        assertFalse(ThemeChoiceRules.sameChoices(before, after));
        assertTrue(ThemeChoiceRules.sameChoices(before, new HashMap<>(before)));
    }

    @Test public void choicesWinUntilThemeInvalidation() {
        Map<String, Object> defaults = new HashMap<>(), theme = new HashMap<>(), choices = new HashMap<>();
        defaults.put("corner_radius", 16);
        theme.put("corner_radius", 24);
        choices.put("corner_radius", 32);
        assertEquals(32, ThemePalette.resolve(defaults, theme, choices, ThemePalette.BLACK).radius);
        choices.clear();
        assertEquals(24, ThemePalette.resolve(defaults, theme, choices, ThemePalette.BLACK).radius);
        theme.put("corner_radius", "wrong");
        assertEquals(16, ThemePalette.resolve(defaults, theme, choices, ThemePalette.BLACK).radius);
    }

    @Test public void pushResetAndFirstPushClearOverridesButUpgradeDoesNot() {
        assertTrue(ThemeChoiceRules.clear("theme:old", "theme:new", "new"));
        assertTrue(ThemeChoiceRules.clear("theme:old", "absent:reset", "reset"));
        assertTrue(ThemeChoiceRules.clear(null, "theme:new", "new"));
        assertFalse(ThemeChoiceRules.clear(null, "absent:absent", "absent"));
        assertFalse(ThemeChoiceRules.clear("theme:same", "theme:same", "same"));
    }
    @Test public void lightDarkAndColouredBackgroundsRemainReadable() {
        for (int background : new int[]{0xFFF5F5F7, 0xFF080A10, 0xFF143A32}) {
            ThemePalette palette = new ThemePalette(new HashMap<>(), background);
            assertTrue(ThemePalette.contrast(palette.text, background) >= 4.5);
            assertTrue(ThemePalette.contrast(palette.secondary, background) >= 4.5);
            assertTrue(ThemePalette.contrast(ThemePalette.ink(palette.focus), palette.focus) >= 4.5);
            assertTrue(ThemePalette.contrast(ThemePalette.ink(palette.surface), palette.surface) >= 4.5);
            int alpha = ThemePalette.scrimAlpha(background, palette.text);
            for (int photo : new int[]{ThemePalette.BLACK, ThemePalette.WHITE}) {
                assertTrue(ThemePalette.contrast(palette.text, ThemePalette.composite(background, photo, alpha)) >= 4.5);
            }
        }
    }

    @Test public void unreadableRequestedTextIsCorrected() {
        Map<String, Object> values = new HashMap<>();
        values.put("text_color", "#FFFFFF");
        ThemePalette palette = new ThemePalette(values, ThemePalette.WHITE);
        assertEquals(ThemePalette.BLACK, palette.text);
        values.put("surface_color", "#FFFFFF");
        ThemePalette opposite = new ThemePalette(values, ThemePalette.BLACK);
        assertTrue(ThemePalette.contrast(opposite.textOn(opposite.surface), opposite.surface) >= 4.5);
        assertTrue(ThemePalette.contrast(opposite.textOn(opposite.background), opposite.background) >= 4.5);
    }

    @Test public void malformedAndFractionalValuesFallBack() {
        for (Object bad : new Object[]{true, "24", 1.5, -1, 49, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals(16, ThemePalette.integer(bad, 16, 0, 48));
        }
        assertEquals(0, ThemePalette.integer(0, 16, 0, 48));
        assertEquals(48, ThemePalette.integer(48, 16, 0, 48));
        for (Object bad : new Object[]{true, 42, "red", "#FFFF", "#GGGGGG", "#00FFFFFF"}) {
            assertEquals(ThemePalette.BLACK, ThemePalette.color(bad, ThemePalette.BLACK));
        }
    }

    @Test public void stringsBooleansAndArraysNeverCoerceTypes() {
        assertTrue(ThemePalette.bool("false", true));
        assertFalse(ThemePalette.bool(false, true));
        assertEquals("fallback", ThemePalette.string(12, "fallback"));
        assertEquals("value", ThemePalette.string(" value ", "fallback"));
        java.util.List<String> fallback = java.util.Collections.singletonList("keep");
        assertSame(fallback, ThemePalette.strings(new Object[]{"valid", true}, fallback));
        assertEquals(java.util.Arrays.asList("one", "two"), ThemePalette.strings(new Object[]{"one", " two "}, fallback));
        int[] keys = {83};
        assertSame(keys, ThemePalette.keycodes(new Object[]{1, 2.5}, keys));
        assertSame(keys, ThemePalette.keycodes(new Object[]{"83"}, keys));
        assertArrayEquals(new int[]{83, 1000}, ThemePalette.keycodes(new Object[]{83, 1000}, keys));
    }

    @Test public void shapeAndBackplateEnumsAreBounded() {
        for (String shape : new String[]{"rounded", "circle", "squircle", "square"}) {
            Map<String, Object> values = new HashMap<>();
            values.put("icon_shape", shape);
            assertEquals(shape, new ThemePalette(values, ThemePalette.BLACK).iconShape);
        }
        Map<String, Object> bad = new HashMap<>();
        bad.put("icon_shape", "triangle");
        bad.put("tile_backplate", 5);
        ThemePalette palette = new ThemePalette(bad, ThemePalette.BLACK);
        assertEquals("square", palette.iconShape);
        assertEquals("solid", palette.backplate);
    }
}
