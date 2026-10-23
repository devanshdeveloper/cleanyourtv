# TV Manager

A lightweight TV system diagnostics and download management module card for Clean Your TV, maintained by [devanshdeveloper](https://github.com/devanshdeveloper).

This add-on absorbs and expands upon [Capacity Stats](https://github.com/lionlev21) by [lionlev21](https://github.com/lionlev21) (MIT License).

## What it does

- **Dashboard**: Live RAM (available, used, total with old-kernel MemAvailable fallback), internal storage capacity, uptime, model and device codes, Android version & API level, display resolution and density. Includes bidirectional text / RTL Hebrew number isolation.
- **Storage**: Real-time storage gauge with plain-language status and low-storage warnings when free space drops below 1 GB.
- **Applications**: Lists installed leanback and standard applications. Directly opens system App Info or system Uninstall dialogs. Supports sorting by app name or install date.
- **ADB Hygiene**: Reports USB debugging and network ADB port 5555 status. Displays instructions to close ADB ports securely, and quotes the check command: `python3 tools/adb-closed.py <tv ip>`.
- **Network**: Reports active connection type (Ethernet/Wi-Fi), Wi-Fi SSID and signal strength (dBm and bars), and local IP address.
- **Download Manager**: Lists files in the public Downloads directory and app download storage with file sizes and dates. Allows opening `.apk` packages using the system installer, deleting individual files, and a one-click "Delete All Installers" action with confirmation.
- **Settings Controls**: Optional presets for window animation scales (0x, 0.5x, 1x), screensaver selection (including `com.gathin.tvhome/.ClockDream`), and Private DNS presets (AdGuard, Cloudflare, Off) with a one-tap revert to previous settings.
- **System Report**: Exports a complete plain text hardware and system report to the app's local storage.
- **Sleep Timer**: Omitted by design. On Android TV API 26, putting the display or device to sleep requires signature-level permissions (`DEVICE_POWER`) or a user-enabled Accessibility service. No documented public API puts the device to sleep on API 26 without manual service setup.

## Permissions

- `REQUEST_INSTALL_PACKAGES`: Required to pass downloaded APKs to the Android system package installer when you choose to install an APK file in the Download Manager.
- `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`): Used to inspect downloaded files in the public Downloads folder.
- `WRITE_SECURE_SETTINGS`: Optional privileged permission for system settings controls (animation speed, private DNS, screensaver). The app **never** grants permissions to itself. If not granted, settings rows remain disabled and show the manual ADB grant command:
  ```bash
  pm grant io.github.devanshdeveloper.tvmanager android.permission.WRITE_SECURE_SETTINGS
  ```

No `INTERNET` permission is requested. No memory cleaner, no background services, no telemetry, and no ads.

## Credits & License

- Original capacity calculation and /proc/meminfo parsing logic by [lionlev21](https://github.com/lionlev21) from Capacity Stats.
- Distributed under the MIT License.

## Tested on

- Mi TV 4A (Android TV 9, Pie)
