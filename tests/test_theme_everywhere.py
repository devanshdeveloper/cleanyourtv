"""Theme regressions without a TV. Device rendering is explicitly outside these checks."""
import json
import re
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'launcher/app/src/main/java/com/gathin/tvhome'
FIXTURES = ROOT / 'tests/fixtures/themes'


class ThemeEverywhereTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.classes = Path(cls.temp.name)
        javac = shutil.which('javac')
        cls.java = shutil.which('java')
        if not javac or not cls.java:
            raise unittest.SkipTest('JDK 17 needed for the production palette fixture checks')
        subprocess.run([javac, '-d', str(cls.classes), str(JAVA / 'ThemePalette.java'),
                        str(ROOT / 'tests/fixtures/ThemeProbe.java')], check=True, capture_output=True)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_fixture_themes_use_production_palette_and_materialize_for_device_checks(self):
        destination = ROOT / 'work/theme-fixtures'
        for name in ('light', 'dark', 'coloured'):
            with self.subTest(theme=name):
                config = json.loads((FIXTURES / name / 'config.json').read_text())
                args = []
                for key, value in config.items():
                    kind = 'bool' if isinstance(value, bool) else 'int' if isinstance(value, (int, float)) else 'str'
                    args.append(f'{key}={kind}:{value}')
                result = subprocess.run([self.java, '-cp', str(self.classes), 'com.gathin.tvhome.ThemeProbe',
                                         config['background_color'], *args], capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn(config['icon_shape'], result.stdout)
                target = destination / name
                target.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(FIXTURES / name / 'config.json', target / 'config.json')
                if name == 'coloured':
                    shutil.copyfile(ROOT / 'themes/aurora/wallpaper.jpg', target / 'wallpaper.jpg')
                    self.assertGreater((target / 'wallpaper.jpg').stat().st_size, 0)

    def test_presets_are_bundled_from_the_actual_theme_sources(self):
        gradle = (ROOT / 'launcher/app/build.gradle').read_text()
        self.assertIn("from(rootProject.file('../themes'))", gradle)
        self.assertIn("include '*/config.json', '*/wallpaper.jpg', '*/wallpapers/*'", gradle)
        settings = (JAVA / 'LauncherSettingsActivity.java').read_text()
        self.assertNotIn('minimalJson', settings)
        self.assertNotIn('FileOutputStream(', settings)
        self.assertIn('editor.putString("theme_preset", THEME_PRESETS[which])', settings)

    def test_every_on_tv_write_is_cleared_by_push_reset_and_follow_theme(self):
        settings = (JAVA / 'LauncherSettingsActivity.java').read_text()
        config = (JAVA / 'Config.java').read_text()
        keys = set(re.findall(r'(?:putBoolean|putInt|putString)\("([a-z_]+)"', settings))
        keys.update(re.findall(r'add(?:Tier2)?ToggleRow\("([a-z_]+)"', settings))
        tracked = config.split('static final String[] ON_TV_KEYS = {', 1)[1].split('};', 1)[0]
        for key in keys:
            self.assertIn('"' + key + '"', tracked, key)
        self.assertIn('for (String key : Config.ON_TV_KEYS)', settings)
        self.assertEqual(config.count('for (String key : ON_TV_KEYS) editor.remove(key);'), 2)
        for tool in ('theme.sh', 'theme.ps1'):
            self.assertIn('theme_revision', (ROOT / 'tools' / tool).read_text())

    def test_new_visual_keys_are_documented_and_resolved(self):
        palette = (JAVA / 'ThemePalette.java').read_text()
        keys = re.findall(r'"([a-z_]+)"', palette.split('static final String[] KEYS = {')[1].split('};')[0])
        docs = (ROOT / 'themes/README.md').read_text()
        for key in keys:
            self.assertRegex(docs, rf'(?m)^\| `{key}` \|')
            self.assertIn(key, (ROOT / 'launcher/app/src/main/res/values/config.xml').read_text())

    def test_backgrounds_share_a_bounded_decoder_and_release_when_hidden(self):
        settings = (JAVA / 'LauncherSettingsActivity.java').read_text()
        self.assertNotIn('decodeFile(', settings)
        player = (JAVA / 'WallpaperPlayer.java').read_text()
        self.assertIn('WallpaperSize.sampleSize', player)
        self.assertIn('SharedWallpaper.acquire', player)
        self.assertIn('SharedWallpaper.release(current)', player)
        pause = player.split('    void stop() {', 1)[1].split('    }', 1)[0]
        self.assertNotIn('setImageDrawable(null)', pause, 'translucent Quick settings must leave Home visible')
        self.assertIn('wallpaperPlayer.release();', (JAVA / 'MainActivity.java').read_text())
        self.assertIn('ThemeBackdrop', (JAVA / 'ClockScene.java').read_text())
        session = (JAVA / 'ThemeSession.java').read_text()
        self.assertIn('current = Config.resolve(app);', session)
        self.assertIn('identity = key(app);', session, 'cache the state after override invalidation')
        self.assertIn('ThemeChoiceRules.sameChoices(choices, now)', session)
        self.assertNotIn('getAll().hashCode()', session)

    def test_all_visible_activity_entries_use_the_theme(self):
        for name in ('MainActivity', 'LauncherSettingsActivity', 'ClockActivity', 'QuickSettingsActivity'):
            self.assertIn('extends ThemeActivity', (JAVA / (name + '.java')).read_text())
        self.assertIn('ThemeStyle.dialog(this)', (JAVA / 'LauncherSettingsActivity.java').read_text())
        self.assertIn('style.panel(panel)', (JAVA / 'AppLibraryDialog.java').read_text())
        style = (JAVA / 'ThemeStyle.java').read_text()
        self.assertIn('setCheckMarkTintList', style)
        self.assertIn('setSelector(selector)', style)

    def test_invalid_numbers_and_arrays_do_not_silently_coerce(self):
        config = (JAVA / 'Config.java').read_text()
        self.assertIn('ThemePalette.integer(v, fallback, min, max)', config)
        self.assertIn('ThemePalette.keycodes(values, fallback)', config)
        self.assertIn('ThemePalette.strings(values, fallback)', config)

    def test_icons_are_masked_without_tinting_the_app_logo(self):
        mask = (JAVA / 'ThemedIcon.java').read_text()
        self.assertIn('canvas.clipPath(mask)', mask)
        self.assertNotIn('createBitmap', mask)
        self.assertNotIn('source.setColorFilter', mask)
        self.assertIn('new android.util.LruCache<>(64)', (JAVA / 'AppLibraryDialog.java').read_text())
        own_banner = (JAVA / 'AppAdapter.java').read_text()
        self.assertIn('layers.setDrawable(0, style.tile())', own_banner)
        self.assertIn('layers.getDrawable(1).setTint(style.palette.text)', own_banner)


if __name__ == '__main__':
    unittest.main()
