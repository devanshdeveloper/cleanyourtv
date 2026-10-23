"""Reject additional literal styling in production launcher sources.

The allowlist freezes exact tokens and counts, rather than exempting whole files.
Each entry explains why those specific literals remain. Runtime screenshot tests
are still required; this guard cannot prove that an XML fallback is replaced.
"""
from collections import Counter
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / 'launcher/app/src'
TEST_DATA = {
    'launcher/app/src/test/java/com/gathin/tvhome/CardsTest.java': 'Module-accent parser input and expected-colour fixtures.',
    'launcher/app/src/test/java/com/gathin/tvhome/FestivalTest.java': 'Festival parser input and expected-colour fixtures.',
    'launcher/app/src/test/java/com/gathin/tvhome/ThemePaletteTest.java': 'Light, dark, coloured and malformed palette fixtures.',
}
TOKENS = re.compile(r'0x[0-9a-fA-F]{6,8}\b|#[0-9a-fA-F]{3,8}\b|Color\.(?:WHITE|BLACK|GRAY|RED|GREEN|BLUE)|Color\.(?:rgb|argb)\(\s*\d|Color\.parseColor\(\s*"[^"]+"|Typeface\.create\(\s*"[^"]+"|Typeface\.(?:DEFAULT|SANS_SERIF|SERIF|MONOSPACE)|(?:setCornerRadius|setRadius)\(\s*[0-9.]+f?|android:radius="[0-9.]+dp"|fontFamily="(?!@)[^"]+"|@android:color/(?:black|white)')
ALLOWED = {
    # Historical neutral fallback for failed third-party artwork sampling, not an icon tint.
    "launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java": ["0xFF444444","0xFF444444","0xFF444444"],
    # Brass artwork palette; the finished launcher glyph receives theme tint at runtime.
    "launcher/app/src/main/java/com/gathin/tvhome/BellDrawable.java": ["0x66FFF2C0","0x995A3608","0xB3FFF4D2","0xFF3E2604","0xFF452A05","0xFF452A05","0xFF4E3208","0xFF5A3608","0xFF5A3A0A","0xFF5C380A","0xFF60400E","0xFF6B430E","0xFF7A4E12","0xFF9C6A1A","0xFFA87420","0xFFB07E22","0xFFB98424","0xFFD6A13A","0xFFD6A13A","0xFFD9A43A","0xFFE3B04A","0xFFF0CF72","0xFFF2D27A","0xFFFBE08A"],
    # Module-accent parsing and its built-in validator self-test, not launcher UI style.
    "launcher/app/src/main/java/com/gathin/tvhome/Cards.java": ["#E8A33D","#ffe8a33d","0xFF000000","0xFFE8A33D","0xFFE8A33D"],
    # Flame artwork palette; the finished launcher glyph receives theme tint at runtime.
    "launcher/app/src/main/java/com/gathin/tvhome/DiyaDrawable.java": ["0x00A0A0A0","0x00FF8A1A","0x00FF8A1A","0x00FF9A20","0x40FF9A2A","0x44FF9A2A","0x73FFE8D2","0x99FFC45A","0xA6FFB347","0xB09A9A9A","0xCCFFC24A","0xFF2B140A","0xFF3B170A","0xFF3D1408","0xFF4A1A0A","0xFF4A1E0E","0xFF5A2A10","0xFF6E2C14","0xFF7A4416","0xFF8E3E1E","0xFF8E8E8E","0xFF9A4220","0xFFA0A0A0","0xFFB8400A","0xFFC4612F","0xFFC4612F","0xFFD9430B","0xFFE58C57","0xFFEE9A68","0xFFF08A24","0xFFF2A12E","0xFFFF5A1A","0xFFFF7A14","0xFFFF9A2A","0xFFFFA62B","0xFFFFB040","0xFFFFB347","0xFFFFB83F","0xFFFFD25A","0xFFFFE0A3","0xFFFFE9A6","0xFFFFF0B8","0xFFFFF0B8","0xFFFFF4C8","0xFFFFF4C8","0xFFFFF8E1"],
    # Festival-specific gold fallback and RGB packing retained for existing festival themes.
    "launcher/app/src/main/java/com/gathin/tvhome/Festival.java": ["0xFF000000","0xFFFFB23C"],
    # Inflation fallback and RGB/alpha packing; setTheme replaces the visible rim.
    "launcher/app/src/main/java/com/gathin/tvhome/GlassView.java": ["0xCCFFFFFF","0xCCFFFFFF","0xE6000000","0xFFFFFF","0xFFFFFF","0xFFFFFF"],
    # RGB channel mask while sampling third-party artwork; never a UI colour.
    "launcher/app/src/main/java/com/gathin/tvhome/HeroArt.java": ["Color.BLACK"],
    # Neutral alpha texture used only as a mask, tinted with the resolved/app glow colour.
    "launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java": ["0x00FFFFFF","0x00FFFFFF","0x00FFFFFF","0x00FFFFFF","0x24FFFFFF","0x26FFFFFF","0x40FFFFFF","0x47FFFFFF","0x5CFFFFFF","0x66FFFFFF","0xA6FFFFFF"],
    # Petal artwork palette; the finished launcher glyph receives theme tint at runtime.
    "launcher/app/src/main/java/com/gathin/tvhome/MarigoldDrawable.java": ["0x005A1E00","0x665A1E00","0x805A1E00","0x805A1E00","0xFF000000","0xFF5C2403","0xFF6E2400","0xFF7A3004","0xFFA43E00","0xFFB04E08","0xFFE0821A","0xFFE5600A","0xFFF0800E","0xFFF0800E","0xFFF79A16","0xFFF9A31B","0xFFFFB524","0xFFFFB524","0xFFFFB84A","0xFFFFC63E","0xFFFFE9A6"],
    # Festival illustration artwork and RGB packing, rather than UI text or a tile surface.
    "launcher/app/src/main/java/com/gathin/tvhome/PetalShowerView.java": ["0xFF000000","0xFF7A2800","0xFFE85D04","0xFFF26A0D","0xFFFF8C1A","0xFFFFB627","0xFFFFC93C"],
    # Time-of-day sky artwork anchors, controlled by sky_intensity.
    "launcher/app/src/main/java/com/gathin/tvhome/Sky.java": ["0xFF04060E","0xFF070C1A","0xFF0A0A1C","0xFF0B1326","0xFF0B1524","0xFF0D1526","0xFF0E1A33","0xFF12203F","0xFF140C27","0xFF160F2E","0xFF17304F","0xFF191230","0xFF1B1030","0xFF1F3557","0xFF20395C","0xFF31587F","0xFF3A1F49","0xFF3B2352","0xFF43648B","0xFF4A2049","0xFF4C5470","0xFF5C3A63","0xFF6C2E52","0xFF8A4A6B","0xFFA54A5C","0xFFD3674F","0xFFE2704A"],
    # Sun highlight artwork and RGB alpha masks for the configured sky.
    "launcher/app/src/main/java/com/gathin/tvhome/SkyView.java": ["0x00FFFFFF","0x00FFFFFF","0xFFFFD9A0"],
    # Central default palette, WCAG black/white endpoints and RGB bit mask.
    "launcher/app/src/main/java/com/gathin/tvhome/ThemePalette.java": ["0x00FFFFFF","0xFF000000","0xFF1C1C1E","0xFF202024","0xFF6EC1FF","0xFFF1F1F3","0xFFFFFFFF"],
    # DST_IN alpha mask; RGB channels do not affect its opacity operation.
    "launcher/app/src/main/java/com/gathin/tvhome/TopFade.java": ["0x00000000","0xFF000000"],
    # Inflation-only root background; ThemeBackdrop installs the resolved background.
    "launcher/app/src/main/res/layout/activity_launcher_settings.xml": ["@android:color/black"],
    # Build-time theme defaults, resolved before JSON and on-TV values.
    "launcher/app/src/main/res/values/config.xml": ["#000000","#6EC1FF"],
    # Black starting-window fallback before resolved UI inflation; no visible activity owns this preview.
    "launcher/app/src/main/res/values/styles.xml": ["@android:color/black","@android:color/black"],
    # Inflation fallback colours and vector alpha seeds; runtime ThemeStyle replaces UI styling.
    "launcher/app/src/main/res/values/theme_seeds.xml": ["#00000000","#14FFFFFF","#1AFFFFFF","#246E58","#26FFFFFF","#2EFFFFFF","#33FFFFFF","#38FFFFFF","#596EC1FF","#73FFFFFF","#80FFFFFF","#88FFFFFF","#8CFFFFFF","#99111111","#99FFFFFF","#A6FFFFFF","#B3FFFFFF","#D9FFFFFF","#E6111111","#E6FFFFFF","#F22A2A2E","#F2FFFFFF","#F5161618","#F5FFFFFF","#FF000000","#FF111111","#FF1C1C1E","#FF34C759","#FF6EC1FF","#FF8C0049","#FFE20074","#FFFF3B2F","#FFFFFFFF"],
}


def literals(text):
    text = re.sub(r'/\*.*?\*/|<!--.*?-->', '', text, flags=re.S)
    text = re.sub(r'//[^\r\n]*', '', text)
    return Counter(TOKENS.findall(text))


class ThemeStyleGuardTest(unittest.TestCase):
    def test_no_unreviewed_literal_style(self):
        for path in SOURCES.rglob('*'):
            if path.suffix not in ('.xml', '.java'):
                continue
            name = path.relative_to(ROOT).as_posix()
            if name in TEST_DATA:
                self.assertTrue(TEST_DATA[name], 'Every test-data exception needs a reason')
                continue
            actual = literals(path.read_text(encoding='utf-8'))
            expected = Counter(ALLOWED.get(name, []))
            with self.subTest(path=name):
                self.assertEqual(actual, expected,
                                 'Resolve style through ThemeStyle, or document an exact justified exception')

    def test_guard_rejects_new_colours_radii_and_fonts(self):
        for value in ('paint.setColor(0xFF123456);', 'android:textColor="#123456"',
                      'shape.setCornerRadius(17f)', 'android:fontFamily="serif"',
                      'android:textColor="#FFF"', 'Color.rgb(255, 0, 0)',
                      'Typeface.create("serif", 0)', 'Color.parseColor("red")'):
            self.assertTrue(literals(value), value)


if __name__ == '__main__':
    unittest.main()
