# WattScope 1.1.4

[简体中文](README.md) | **English**

[Download](https://github.com/Refract-Eclipse/WattScope/releases/tag/v1.1.4) · [Changelog](CHANGELOG.md) · [Background guidance](docs/background-guidance.md) · [Verification](VERIFICATION.md) · [MIT License](LICENSE)

A native Android battery-side power monitor with a dashboard, ongoing notification, draggable overlay, power chart, and CSV export. Android 8.0+ (API 26), targeting API 36. Java and platform UI, no third-party runtime dependencies, no internet permission, no Root or server required.

## New in 1.1.4

- Unified background guidance for vivo/iQOO, Xiaomi/Redmi/POCO, OPPO, OnePlus, realme, Huawei, Honor, Samsung, Google/Pixel, and other Android devices.
- Automatic brand suggestions, a persistent manual override, public settings shortcuts, and fallbacks when an entry is unavailable.
- First-recording explanation of screen-off sampling and its battery cost, with an option to proceed without requesting an exemption.
- Android battery optimization status is read from the system. Manufacturer restrictions always require manual verification; opening a settings page never implies a grant.
- A renewable partial wake lock during active recording, released when monitoring stops or the service is destroyed.
- A cleaner main page with detailed permission and sampling diagnostics in a collapsed section.
- Shared version metadata, Gradle-integrated tests, and a signed Release APK with debugging disabled.

## Installation and upgrade compatibility

Download **WattScope-1.1.4.apk** from Releases. Source archives cannot be installed directly.

| Installed version | Behavior when installing 1.1.4 |
| --- | --- |
| Universal guidance trial `1.1.3-guide-test1` | Same application ID and signing certificate; supports an in-place upgrade and preserves preferences |
| Original GitHub 1.1.3 / 1.1.2 (`dev.wattscope`) | Different application ID; installs alongside it and requires separate permissions |
| Other background/screen-off trials | Different package IDs remain separate; no automatic preference or history import |

The application ID remains `dev.wattscope.guidetrial` to preserve upgrade compatibility with the most recent trial. The display name is “瓦特计”, version name is 1.1.4, and versionCode is 9. Keeping this identifier is a compatibility decision, not a claim that all devices have been validated.

Export important recordings before upgrading: installation restarts the process and in-memory samples are not preserved. Run only one recorder when comparing versions.

Open the app, tap Start, and check both Android battery optimization and your manufacturer's background settings. Grant notification permission on Android 13+ if needed. Enabling the overlay opens the system “display over other apps” permission screen. Background guidance remains available through the screen-off recording settings button.

## Features and statistics

- Battery-side net watts, signed current, voltage, battery percentage, and temperature, sampled every 1, 2, or 5 seconds.
- Chart ranges: 1/5/15 minutes, 1 hour, all retained samples, or a custom 1–360 minutes. Up to 21,601 samples from the latest six hours are retained.
- A single charging/discharging direction uses the full chart height with absolute values; mixed directions use signed power. Charging is cyan and the discharge curve is orange.
- **Average and peak magnitude use the selected time range. Accumulated charge and discharge energy cover the full current recording.** Changing the range does not clear history or reset session totals.
- Average magnitude is weighted by valid elapsed sampling time. Peaks use raw samples before chart reduction.
- Missing readings and gaps longer than `max(5 seconds, 3 × sampling interval)` interrupt integration and the curve. Missing data is never fabricated.
- Standard/compact overlay, 70%–200% size adjustment, drag to move, tap to return, and long-press to hide.
- CSV export through the system document picker. UTC timestamps, signed `net_power_W`, and empty fields for unavailable numeric values.

## Measurement and background limitations

Power is estimated as `voltage V × current mA / 1000`. Positive current flows into the battery; negative current flows out. While charging, this is not total device consumption, charger input power, or an individual CPU/GPU power reading.

Android `CURRENT_NOW` is preferred, with `CURRENT_AVERAGE` used only when the first property is unsupported. Sensor update speed, units, polarity, and availability vary. Manual calibration changes reset the recording. Do not infer a 2× voltage multiplier solely from a dual-cell model or a reading near 4 V; confirm the interface semantics first.

The partial wake lock **increases power consumption and changes standby behavior**. Vendor policies, power-saving modes, or process termination may still interrupt sampling. An Android exemption does not confirm that manufacturer restrictions have been removed. Brand profiles are guidance, not universal device certification.

Monitoring does not auto-start at boot or restart after process termination. Samples and statistics remain in process memory; starting a new session, process restart, or calibration changes clear them. Export records you need to keep.

## Permissions and privacy

| Permission | Purpose |
| --- | --- |
| FOREGROUND_SERVICE / FOREGROUND_SERVICE_SPECIAL_USE | User-started foreground monitoring |
| POST_NOTIFICATIONS | Monitoring notification on Android 13+ |
| SYSTEM_ALERT_WINDOW | Optional floating overlay |
| WAKE_LOCK | Keeping the CPU awake during recording |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | User-initiated exemption request |

No internet, storage, accessibility, or location permission; no account, ads, or telemetry. Manufacturer information is used locally to select guidance. See [PRIVACY.md](PRIVACY.md).

## Build and test

Use JDK 17, Android SDK Platform 36, and Build Tools 36.0.0. The project uses AGP 8.11.1 and Gradle 8.13.

```bash
./gradlew testDebugUnitTest assembleDebug
```

Use `gradlew.bat` on Windows. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. JUnit and Robolectric are test-only dependencies. The test suite includes the original 130 core assertions plus guidance, permission, settings fallback, and service lifecycle tests.

For a signed Release build, provide `WATTSCOPE_KEYSTORE` (absolute path), `WATTSCOPE_STORE_PASSWORD`, `WATTSCOPE_KEY_ALIAS`, and `WATTSCOPE_KEY_PASSWORD`, then run `assembleRelease`. Without signing credentials, the Release output is unsigned. Private keys are excluded from the repository and source archive. A locally generated key generally cannot update the published APK.

`build-apk.ps1` is a separate Windows development build route using the same version metadata. It outputs `app/build/outputs/apk/debug/WattScope-1.1.4-debug.apk`; it was not executed in this release environment. The GitHub release contains the locally verified non-debuggable Release build.

See [VERIFICATION.md](VERIFICATION.md) for actual results and limits, and [CONTRIBUTING.md](CONTRIBUTING.md) for contribution instructions.

## Source and license

Production code is in `app/src/main/java/dev/wattscope/`. `BackgroundGuidance`, `BackgroundGuideDialog`, and `BackgroundSettings` handle routing, guidance UI, and public settings access. Guidance text is in `app/src/main/res/values/background_guidance.xml`.

Project code and original resources use [MIT](LICENSE). Gradle and test tools retain their respective licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
