# Theme consistency report

Work is local on `fix/theme-everywhere`, based on `561e5b3ab4be8b24b73705dae02d8eeb009320e1`.
No ADB command was run against a TV. No real-TV behavior or screenshot is claimed.
Java citations in the investigation below are relative to
`launcher/app/src/main/java/com/gathin/tvhome/` at that baseline revision.

## Root causes and hypothesis results

- **a — RULED OUT in source; runtime NOT TESTABLE.** `tools/theme.sh:32` targets
  the external files directory returned by `Config.java:330`. Its provider-first
  writes and Android 8/9 fallback are implemented at `tools/theme.sh:66-80`;
  PowerShell has the same mechanism at `tools/theme.ps1:49-59`.
  `ThemeProvider.java:23-40` resolves the same files. Provider access is protected
  by WRITE_SECURE_SETTINGS in `launcher/app/src/main/AndroidManifest.xml:173-178`.
  This rules out a directory/name mismatch in this checkout. Whether the target
  TV actually finds readable files is **NOT TESTABLE** without the TV.
- **b — CONFIRMED in part.** `Config.java:682-698` already logs invalid JSON and
  read failures; a claim that all parsing errors were silent is **RULED OUT**.
  Scalar helpers at `Config.java:727-741` intentionally fall back on bad types.
  Integer parsing truncated fractional values (`Config.java:737-741`), and mixed
  string arrays were partially accepted (`Config.java:782-795`). The settings
  preset writer swallowed IOException (`LauncherSettingsActivity.java:537-558`).
  Integers and arrays now use strict, JVM-tested validators; presets no longer
  write replacement config files, and asset preparation failures are logged.
- **c — CONFIRMED for presets; RULED OUT for tracked push/reset choices.**
  `Config.java:52-67` omitted `theme_preset`. The revision/signature mechanism
  already cleared the listed overrides (`Config.java:620-638`), and both tools
  issue unique revisions, including identical pushes and reset.
  Follow theme only removed that list (`LauncherSettingsActivity.java:804-813`).
  Before this change the preset preference was picker metadata, not a Config
  override: its omission caused stale labels, not a fresh-push precedence failure.
  The preset and all new visual overrides are now included. Library membership,
  dock order and module-card approvals intentionally retain their separate stores.
- **d — CONFIRMED for visible settings and non-Home windows.** Home loads Config at
  `MainActivity.java:357` and recreates on the settings revision
  (`MainActivity.java:821-824`); the push tools already restart Home.
  Settings set the backdrop only during creation
  (`LauncherSettingsActivity.java:97-131`). Its `changed()` only incremented a
  revision (`LauncherSettingsActivity.java:816-820`). Settings now refreshes
  immediately, and completed provider revisions notify visible launcher windows.
  A pre-existing static Config cache was **NOT FOUND**. The new process cache is
  keyed by file metadata, storage state and preferences, with explicit provider
  invalidation.
  Preference snapshots are compared by equality, so hash collisions cannot hide
  a changed on-TV choice.
- **e — CONFIRMED.** Fixed UI styling was spread through Java and XML. Every
  matching original location, including text sizes, font references and layout
  geometry, is listed with baseline path:line in
  [the style inventory](theme-style-inventory.md). The new
  [literal-style guard](../tests/test_theme_style_guard.py) freezes exact remaining
  colour/radius/font tokens and explains each exception. This is a source guard,
  not a rendering test.
- **f — CONFIRMED.** Home alone constructed WallpaperPlayer and configured the sky
  (`MainActivity.java:394-396`). Settings decoded an unsampled second image and
  copied it to dim it (`LauncherSettingsActivity.java:103-127`). Quick settings
  and ClockScene did not load Config. Full-screen launcher pages now share the
  bounded decoder/cache; panels have matching surfaces and scrims.
- **g — Partial styling support CONFIRMED; exhaustive runtime coverage NOT TESTABLE.**
  `background_color` was parsed in Config (`Config.java:460`), but the general UI
  palette keys were **NOT FOUND**. `accent_color` only reached the festival parser
  (`Config.java:575-576`). Existing documentation/resource coverage checks remain
  in place and pass. Static cross-checks found no unused config.xml resources in
  either revision, no undocumented JSON helper keys, and no read resource keys
  missing from the Keys table. Complex-object parsing and Android rendering are
  not proven by those checks. There are no night or version-qualified resource directories
  in this checkout; those alternative style sources were **NOT FOUND**.
- **h — CONFIRMED.** The picker generated small substitutes
  (`LauncherSettingsActivity.java:537-546`), rather than the named themes.
  Compare Aurora's substitute with `themes/aurora/config.json:1-4`; Glass also lost
  its actual dock and cinematic settings. Presets now package the actual configs
  and artwork directly from `themes/`. Wallpaper choice already participated in
  Config's resolution (`Config.java:563-574`), but its settings preview was stale.
  A theme file already overwritten by the old picker cannot be reconstructed;
  push the desired source theme once after updating.

## Screen coverage

“After” describes the implemented source path; device rendering is unverified.

| Screen/window | Before | After / theme consumer |
|---|---|---|
| Home: clock, greeting, stats, header, shelf, grid | Partial Config; mostly fixed UI colours | `MainActivity` → `Config.style`, `ThemeActivity`, `ThemeStyle` |
| Dock and ordinary app tiles | Config layout/effects; fixed backplates and radii | `AppAdapter`, `DockDecoration`, `GlassView` |
| Module-card tiles | Same fixed tile treatment | Same `AppAdapter` rules and icon masks |
| Settings category overview | Background only, separate unsampled wallpaper | `LauncherSettingsActivity` → `ThemeBackdrop` + `ThemeStyle` |
| Appearance and Themes category | Fixed row/dialog styling; stale preview | Same shared style, live refresh, actual presets |
| Layout and Grid category | Fixed row/dialog styling | Same shared style |
| Top Bar and Header category | Fixed row/dialog styling | Same shared style |
| Motion category | Fixed row/dialog styling | Same shared style |
| Apps category | Fixed row/dialog styling | Same shared style |
| Clock/Behaviour category | Fixed row/dialog styling | Same shared style |
| Remote category | Fixed row/dialog styling | Same shared style |
| Settings choice, confirmation and info dialogs | Android default styling | `ThemeStyle.dialog()`, including system-created dialog rows |
| Module cards manager in Settings | Android default dialogs | `ThemeStyle.dialog()` |
| Module cards list from Home | Fixed glass resources | `MainActivity.glassPopup()` → `ThemeStyle.panel()` |
| Module-card approval prompt | Fixed XML | `MainActivity.showCardPromptNow()` → `ThemeStyle.panel()` |
| App library | Fixed programmatic palette | `AppLibraryDialog` → `ThemeStyle`, shared tile rules |
| Library app-actions menu | Fixed Inputs resources | `AppLibraryDialog` → `ThemeStyle.panel()` |
| Tile menu | Fixed Inputs resources | `MainActivity.glassPopup()` → `ThemeStyle.panel()` |
| Inputs menu | Fixed Inputs resources | Same shared popup style |
| Header gear menu and hold-Settings menu | Fixed Inputs resources | Same shared popup style |
| QuickSettingsActivity | No Config styling | `ThemeActivity`, `ThemeStyle.panel()`, matching scrim |
| ClockActivity | Fixed black/palette | `ClockScene` → Config, ThemeBackdrop, ThemeStyle |
| ClockDream | Fixed black/palette | Same ClockScene; observes completed theme revisions |
| InputsRequestActivity | No visible UI; forwards to Home | Unchanged forwarding; destination Inputs menu is themed |
| HomeSettingsActivity | No visible UI; opens system settings | Unchanged forwarding; Android/OEM settings remain external |

Hebrew layout files retain their view structure, ordering and D-pad geometry.
The runtime styling never changes layout direction. Outfit remains the UI font;
the existing Vietnamese system-family fallback is retained for missing glyphs.

## Decisions, defaults and resource use

1. Keep the provider-first push paths, install/Home guards, permissions, services,
   SDK levels and CI gate. No network feature or dependency was added.
2. Resolve config.xml defaults, the selected theme, then on-TV choices. A selected
   preset is an on-TV theme-source choice; Follow theme returns to the untouched
   pushed theme. A new push/reset clears that choice too. The picker's existing
   Reset to built-in action selects config.xml through an on-TV override, rather
   than deleting the pushed files; Follow theme can therefore recover the pushed
   theme afterwards. A pushed overlay is displayed as Theme default, not Built-in.
3. Bundle original preset configs and images through one Gradle Sync task. There
   is no hand-maintained second copy of those configs and no new tracked binary.
   Preset images are extracted to app-private storage once per app update.
4. Use runtime styles for existing XML inflation fallbacks. Fixed layout sizes
   that define the established D-pad geometry remain. No night resource variant
   or new RTL layout was introduced.
5. Use opaque matching panel surfaces and contrast-correct text; focus uses its
   own contrasting ink. Native choice dialogs use themed checkmarks and outlined
   selection surfaces rather than an OEM selector colour. Wallpaper/sky/artwork
   get a background-matching scrim calculated against black and white image extremes. This may darken/lighten
   existing photos more than `wallpaper_dim` alone; it is an intentional readability
   change. The existing dim and RGB_565/ARGB_8888 decoder choices still apply.
6. Share bitmap references between visible pages. No second full-sized wallpaper
   is decoded for a matching file, size, colour mode and dim setting. Release
   fully hidden pages, with a 500 ms retirement window for RenderThread and
   activity handoff. A paused Home retains its image behind translucent panels.
7. Icon masks clip the original drawable without a bitmap copy or colour filter.
   Home entries retain their wrapped drawable; the library has a 64-package LRU.
   Banners keep their app artwork; the tile container supplies theme geometry.
8. Defaults for existing behavioral config keys are unchanged. Visible default
   differences required by the shared styling are: unified 16 dp panel/tile
   corners, opaque matching panel/pill surfaces, contrast-correct secondary text
   rather than fixed white alpha, themed glyph/status accents, and a theme-accent
   clock glow instead of a random multicolour cycle. The launcher-owned Live TV
   placeholder uses the palette instead of its fixed magenta fill and white glyph.
   The default square icon mask
   preserves the icon's existing outline; other shapes are explicitly selected.
   Explicit app-glow/cinematic behavior remains unless `glow_color` overrides it.
9. Persist the last focused tile key so the tools' existing process restart can
   restore it. In-process Home recreation also keeps the existing static focus
   key. Focus restoration still needs a device check.
10. Make popup/clock motion honor animations/reduced_motion and library focus
    honor focus_overshoot. Do not introduce decorative background animation.
11. Keep fixtures in tests, materialized into ignored work/ for device pushes.
    No theme with fabricated or missing real-TV screenshots was published.
12. Correct two existing lint indentation findings with statement separation.
    Existing unrelated add-on files, screenshots and local build outputs are
    left alone. Do not weaken screenshot tests to manufacture a green suite.

The ten added keys are `surface_color`, `text_color`, `text_secondary_color`,
`focus_color`, `glow_color`, `corner_radius`, `pill_radius`, `spacing`,
`icon_shape`, and `tile_backplate`. Defaults: automatic palette colours,
16/21/12 dp respectively, `square`, and `solid`. Existing `background_color`
and `accent_color` are now general palette inputs (black and #6EC1FF).
Types, ranges and examples are documented in [the Keys table](../themes/README.md#keys).
Strict parsing additionally rejects fractional integers and mixed-type arrays.

## Removed styles and explicit exceptions

XML colour/radius literals moved to central inflation seeds; runtime ThemeStyle
replaces screen text, selectors, glyph tint, surfaces and corner styling.
MainActivity's shelf scrim, labels, dots, race accents and separators now use the
palette. App-library text and surfaces, dock band/edge, focus rim, clock glow,
VPN badge and wallpaper-caption styling also use it.

The guard allowlist retains: central defaults; initial window fallback; RGB
packing/parser masks; neutral glow/DST_IN masks; third-party artwork sampling;
time-of-day sky artwork; and festival illustration palettes whose glyph
silhouettes are tinted at runtime. The petal illustration retains its festival
art colours. Each exact remaining token and reason is in the guard. This is an
explicit artistic exception; it is not a claim that every source literal vanished.

## Validation limits and results

The release gate read from `.github/workflows/ci.yml:51` is
**10,485,760 bytes**; the smaller number mentioned in maintainer prose is stale.

- JDK 17 / Android SDK 34: `gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease`
  **passed**. The XML results total **198 JVM tests**, zero failures/errors.
- Lint: **zero errors, 103 warnings**. Warnings were not suppressed to make the build pass.
- Release APK: **1,841,744 bytes**, below the unchanged **10,485,760-byte** gate.
- Focused Python theme tests: **46 run, OK, five POSIX-only skips**. This includes
  the three fixtures, source guard, theme paths and documentation-key coverage.
- Strict complete Python suite: **676 run, 19 failures, 21 skipped**. The failures
  are the eight inherited failures below, one local generated-APK failure, and
  ten missing new-key screenshots. Later cache, owned-glyph and native-dialog guards were also
  checked in the focused run; this is not a claim of a green full suite.
- The requested Python requirements installed into the existing environment.
  Recreating `tools/venv` reported a launcher-copy warning because this checkout's
  environment is redirected to `.venv`; the existing interpreter remained usable.
- No TV was contacted, no PR was opened, no branch was pushed, and no APK was committed.
- Final repository state: local commits; clean tracked worktree; `git diff --stat` empty.
- Final change summary against the starting branch: TO_BE_RECORDED.

The unchanged baseline runs 666 Python tests and has eight failures (21 skipped):
four missing add-on screenshots, a stale capacity-stats wanted-list entry, and
three old settings-layout documentation checks. The working tree also contains
two pre-existing generated add-on APKs, which trigger its no-prebuilt check.

The ten new keys require real-TV screenshot rows under the existing switches
policy. Those checks cannot pass while obeying the instruction not to touch a TV.
No screenshots are fabricated and no screenshot exceptions were added.
The added tests exercise palette precedence/contrast/types and revision rules,
plus source-level coverage and clearing guards; they are not end-to-end Android
SharedPreferences or provider integration tests for every legacy JSON key.

Third-party logo colours and screens inside other apps are deliberately outside
the implementation. Android/OEM settings and system-rendered toast windows are
not launcher-owned activities. No TV test, performance gain, actual file-access
result, or complete rendering coverage is claimed.

## Implementation references

These references use the final source rather than the baseline:

- Resolution and invalidation: `launcher/app/src/main/java/com/gathin/tvhome/Config.java:338`,
  `launcher/app/src/main/java/com/gathin/tvhome/ThemeSession.java:16`,
  `launcher/app/src/main/java/com/gathin/tvhome/ThemeChoiceRules.java:5`.
- Shared view/dialog rendering: `launcher/app/src/main/java/com/gathin/tvhome/ThemeStyle.java:27`,
  `launcher/app/src/main/java/com/gathin/tvhome/ThemeStyle.java:105`,
  `launcher/app/src/main/java/com/gathin/tvhome/ThemeStyle.java:181`.
- Shared backgrounds: `launcher/app/src/main/java/com/gathin/tvhome/ThemeBackdrop.java:23`,
  `launcher/app/src/main/java/com/gathin/tvhome/SharedWallpaper.java:20`,
  `launcher/app/src/main/java/com/gathin/tvhome/SharedWallpaper.java:32`.
- Tiles/icons: `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java:458`,
  `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryDialog.java:73`.
- Clock and presets: `launcher/app/src/main/java/com/gathin/tvhome/ClockScene.java:38`,
  `launcher/app/src/main/java/com/gathin/tvhome/ThemePresets.java:29`.
- Contrast: `launcher/app/src/main/java/com/gathin/tvhome/ThemePalette.java:122`,
  `launcher/app/src/main/java/com/gathin/tvhome/ThemePalette.java:136`.

## Device checks

Run these yourself in Git Bash from the repository root. They are **not executed**
by this change. Replace the placeholder with your TV's address:

```bash
export TV_HOST='<tv ip>'
export JAVA_HOME='/c/Program Files/Microsoft/jdk-17.0.20.8-hotspot'
launcher/build-and-install.sh
```

The documented installer rebuilds and installs this source using its existing Home
guards. The built output is `launcher/app/build/outputs/apk/release/app-release.apk`.
On this Windows checkout invoke tv.py with the repository's Python executable.

First materialize the fixtures without a TV (the fixture test copies the existing
Aurora wallpaper for the coloured input):

```bash
tools/venv/Scripts/python.exe -m unittest discover -s tests -p test_theme_everywhere.py -v
```

Then capture Home, the settings overview, and all seven category pages for each
theme. The actual push paths are under work/, rather than themes/<name>, because
test fixtures must not be published as themes without real-TV screenshots.

```bash
tv() { tools/venv/Scripts/python.exe -I tools/tv.py "$@"; }
pages=(appearance layout top-bar motion apps clock remote)
for name in light dark coloured; do
  tools/theme.sh "work/theme-fixtures/$name"
  tv --screenshot "shots/$name-home.png" HOME --wait 2
  tv 'am start -n com.gathin.tvhome/.MainActivity -a com.gathin.tvhome.action.LAUNCHER_SETTINGS'
  tv --screenshot "shots/$name-settings.png" --wait 2
  tv 'input keyevent KEYCODE_BACK'
  for i in 0 1 2 3 4 5 6; do
    tv 'am start -n com.gathin.tvhome/.MainActivity -a com.gathin.tvhome.action.LAUNCHER_SETTINGS'
    keys=()
    for ((j=0; j<i; j++)); do keys+=(DPAD_DOWN); done
    tv --screenshot "shots/$name-${pages[$i]}.png" "${keys[@]}" DPAD_CENTER --wait 2
    tv 'input keyevent KEYCODE_BACK; input keyevent KEYCODE_BACK'
  done
done
tools/theme.sh --reset
tv --screenshot shots/reset-home.png HOME --wait 2
tv 'am start -n com.gathin.tvhome/.MainActivity -a com.gathin.tvhome.action.LAUNCHER_SETTINGS'
tv --screenshot shots/reset-settings.png --wait 2
```

Wait for the expected screen before capturing if the TV is slower than these waits.
If a category entry is not focused, use the real D-pad to select that named page;
do not treat a capture of the wrong page as proof.

Expected: light gives light surfaces/dark text and circular icons; dark gives dark
surfaces/light text with glass backplates; coloured gives green-toned surfaces,
warm accents, teal glow, squircle icons and the Aurora photograph under a matching
scrim. App logos retain their original colours throughout. Reset removes the
overlay and tracked choices and returns to config.xml.

Also inspect the library and its actions, hold-OK tile menu, Inputs/gear menus,
module-card list and approval prompt, Quick settings, ClockActivity and ClockDream
with the real remote. Exercise D-pad edges, animations off/reduced motion, all
three themes, a repeated identical push after making on-TV choices, and Follow
theme after selecting a preset. Check Hebrew mirroring and focus retention.
Screenshots showing Wi-Fi names, people or identifiers must be cropped or redacted
before sharing. No shell key injection proves an accessibility-helper button.
