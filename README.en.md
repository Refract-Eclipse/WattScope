# WattScope 1.1.3

[简体中文](README.md) | **English**

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84)](app/build.gradle)
[![Version 1.1.3](https://img.shields.io/badge/version-1.1.3-blue)](CHANGELOG.md)
[![License MIT](https://img.shields.io/badge/license-MIT-green)](LICENSE)

A native Android battery power monitor with a dashboard, an ongoing notification, and a draggable floating overlay. Built with Java and native Android views, WattScope runs entirely on your device and requires no backend, root access, or Google Play services.

**[Download APK](https://github.com/Refract-Eclipse/WattScope/releases/download/v1.1.3/WattScope-1.1.3.apk)** · [Release notes](https://github.com/Refract-Eclipse/WattScope/releases/tag/v1.1.3) · [Repository](https://github.com/Refract-Eclipse/WattScope)

Requires Android 8.0 or later (API 26). Compile and target SDK: API 36. Package: `dev.wattscope`; version code: `5`.

Additional documentation, currently in Chinese: [Changelog](CHANGELOG.md) · [Contributing and tests](CONTRIBUTING.md) · [Privacy](PRIVACY.md) · [Verification](VERIFICATION.md) · [Third-party notices](THIRD_PARTY_NOTICES.md).

## Features

- Live battery net power in watts, signed current in milliamps, voltage, battery level, and battery temperature.
- Selectable sampling intervals of 1, 2, or 5 seconds. The sampling interval does not determine how often the device hardware updates its readings.
- Chart ranges of 1, 5, or 15 minutes, 1 hour, all retained samples, or a custom range of 1–360 minutes.
- Up to 6 hours of raw samples, with missing readings and long sampling gaps shown as breaks in the chart.
- Automatic chart modes: charging only, discharging only, or mixed signed power.
- Average and peak power magnitude for the selected chart range; cumulative charged and discharged energy for the entire monitoring session.
- Standard and compact floating overlays, with continuous scaling from 70% to 200%.
- UTF-8 CSV export of up to 21,601 retained samples through the Android system file picker.
- Manual calibration of current units, current polarity, and voltage multiplier.

## Install and get started

1. Download `WattScope-1.1.3.apk` from [Releases](https://github.com/Refract-Eclipse/WattScope/releases/tag/v1.1.3), or build an APK using the instructions below. Transfer it to your Android device and open it. If prompted, allow installation from the source you are using.
2. Open WattScope, shown as “瓦特计” in the launcher, and tap “开始监测” (Start monitoring). On Android 13 or later, allow notifications to see live readings in the notification shade.
3. Enable “屏幕悬浮窗” (Floating overlay). On first use, grant “Display over other apps” in Android settings, then return to WattScope.
4. Choose a time range above the power chart. “自定义” (Custom) accepts 1–360 minutes. Press and hold the chart to inspect a reading and its timestamp. Changing the range keeps the recorded samples.
5. Drag the overlay size slider to choose 70%–200%. Changes take effect immediately. “恢复 100%” restores the default scale; the style button switches between standard and compact layouts. Position, scale, style, and chart range are saved.
6. Drag the overlay to move it, tap it to return to the app, or long-press it to hide it. You can also hide it from the notification.
7. Tap “停止监测” (Stop monitoring) in the app or notification to stop sampling and close the overlay.

The current app interface uses Chinese labels. The English labels above explain their meaning.

The status bar shows a lightning icon. Expand the WattScope notification to see numerical readings; watts are displayed in the notification content and floating overlay.

## What's new in 1.1.3

- Average and peak power magnitude now follow the selected chart range and update when the range changes or older samples leave the window.
- The average remains time-weighted over valid sampling intervals. The peak is taken from all valid raw samples in the selected range, before chart downsampling.
- Cumulative charged energy, discharged energy, and valid observation time continue to accumulate from the start of the monitoring session.
- No valid data is shown as “—”. One isolated valid reading can provide a peak, but an average requires a valid interval between samples.

This version also includes the dark blue dashboard, gradient chart, automatic charging/discharging chart modes, adjustable overlay, and six-hour sample history.

## Understand the readings

WattScope estimates battery power using Android's public battery APIs:

```text
signed current (mA) = CURRENT_NOW (µA) / 1000
signed net power (W) = voltage (V) × signed current (mA) / 1000
main power number = absolute value of signed net power
```

Positive current means net current entering the battery; negative current means net current leaving it. The main power number shows the magnitude, with a direction label indicating net charging or net discharging. The CSV column `net_power_W` retains the sign.

When the charger is disconnected, net battery discharge power can approximate the device's load at the battery. When a charger is connected, the reading represents net power entering or leaving the battery. It is not a direct measurement of charger input power or total device consumption while charging. WattScope does not measure separate CPU, GPU, or per-app power.

The app first reads `BATTERY_PROPERTY_CURRENT_NOW`. It falls back to `CURRENT_AVERAGE` only when the instantaneous-current property returns the unsupported sentinel, and labels that source in the UI. A zero-current reading remains zero. Current and voltage are not guaranteed to be sampled by the hardware at exactly the same instant, so the calculated power is an estimate.

If the device does not expose a supported current reading, power is shown as unavailable. Voltage, battery level, and temperature remain visible when available. Accuracy, update frequency, and vendor scaling vary by device. This version has not yet been tested on a physical device or emulator.

### Automatic chart modes

| Valid readings in the selected range | Display |
| --- | --- |
| Charging only | Teal charging-power magnitude, using the full chart height and a non-negative scale |
| Discharging only | Orange discharging-power magnitude, using the full chart height and a non-negative scale |
| Both charging and discharging | Signed net power: charging above zero and discharging below zero |

The mode is determined from valid raw samples inside the selected range, rather than from whether a charger is currently connected. Zero and missing readings do not count as an opposite direction. The mode is recalculated when the range changes or samples leave the window.

Chart titles, legends, scales, and inspection labels follow the mode. Long chart ranges retain positive charging peaks and negative discharging peaks.

### Statistics and integration

| Metric | Scope | Calculation |
| --- | --- | --- |
| Average power magnitude | Selected chart range | Integral of absolute power over valid consecutive sampling intervals, divided by their total valid duration |
| Peak power magnitude | Selected chart range | Maximum absolute power among valid raw samples |
| Cumulative charged/discharged energy | Entire monitoring session | Separate integration of the positive and negative portions of net power |

Energy is integrated using trapezoids and actual intervals from a monotonic clock. Intervals crossing zero are split into charging and discharging portions.

Missing readings and gaps longer than `max(5 seconds, 3 × sampling interval)` do not contribute energy or valid integration time. The average is time-weighted, rather than a simple mean of sample values. Samples outside the selected range do not contribute to its average or peak, and readings are not invented at the range boundaries.

Chart downsampling does not change the raw-sample statistics. Session energy totals are unaffected by changes to the chart range or by raw samples aging out of the six-hour history.

### Calibration

Defaults follow the Android convention: raw current in microamps, positive current for charging, and a voltage multiplier of 1×.

If you have verified that a vendor reports current in milliamps instead, adjust the current-unit setting. Reverse polarity only to correct a confirmed vendor-specific sign convention. Changing calibration resets the current samples and session statistics.

A dual-cell phone does not automatically require a 2× voltage multiplier. The vendor may already scale current or voltage for the battery pack. Use 2× only after confirming that the reported voltage is for one cell and the reported current corresponds to the series-connected pack. Without device-specific information, the app cannot guarantee absolute power accuracy for a dual-cell design.

### Background monitoring and retention

Sampling runs in a foreground service explicitly started by the user. The ongoing notification always provides a stop action, and sampling can continue after leaving the main screen.

WattScope does not acquire a wake lock. Deep sleep or vendor background restrictions may pause sampling. Missing time is not filled with estimated readings, so accumulated energy covers valid observed intervals rather than a complete audit of long screen-off periods.

Monitoring does not start automatically at boot or restart automatically after the process is terminated. Samples and session totals are stored in process memory. Starting a new monitoring session, restarting the process, or changing calibration clears them. Export a CSV before restarting or upgrading if you need to retain the records. Sampling and displaying readings also consume some battery power.

## Permissions and privacy

| Permission | Purpose |
| --- | --- |
| `FOREGROUND_SERVICE` | User-started background monitoring |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Foreground service type on Android 14 or later |
| `POST_NOTIFICATIONS` | Notification display on Android 13 or later |
| `SYSTEM_ALERT_WINDOW` | Displaying the floating overlay over other apps |

The app does not request `INTERNET`, `BATTERY_STATS`, storage, or accessibility permissions. Settings and overlay position stay on the device; samples remain in process memory. Files are written only when the user explicitly exports data to a selected location.

See [PRIVACY.md](PRIVACY.md) for the detailed privacy statement in Chinese.

## CSV export

- `time_utc`: UTC timestamp.
- `elapsed_ms`: monotonic milliseconds since device boot.
- `net_power_W`: signed power; positive for charging and negative for discharging.
- `absolute_power_W`: power magnitude.

Additional columns include current, voltage, battery level, temperature, reading source, and diagnostic information. Unavailable numerical values are exported as empty fields.

## Build from source

### Android Studio / Gradle

1. Clone [this repository](https://github.com/Refract-Eclipse/WattScope) and open the root directory containing `settings.gradle` in Android Studio.
2. Use JDK 17, install Android SDK Platform 36, and allow Gradle to synchronize dependencies.
3. Build a debug APK:

   Windows:

   ```powershell
   .\gradlew.bat assembleDebug
   ```

   macOS / Linux:

   ```sh
   ./gradlew assembleDebug
   ```

4. Install `app/build/outputs/apk/debug/app-debug.apk`.

The project uses Android Gradle Plugin 8.11.1 and Gradle 8.13. The app has no external runtime dependencies. The initial build still downloads Gradle and build plugins; full toolchains and signing keys are not included in the source tree.

### Standalone Windows build

Install JDK 17 or later, Android SDK Platform 36, and Android SDK Build Tools 36.0.0. Set `JAVA_HOME` and `ANDROID_HOME`, then run:

```powershell
.\build-apk.ps1
```

You can also provide explicit paths:

```powershell
.\build-apk.ps1 -JavaHomePath 'C:\your-jdk' `
    -PlatformJarPath 'C:\your-sdk\platforms\android-36\android.jar' `
    -BuildToolsPath 'C:\your-sdk\build-tools\36.0.0'
```

The script runs the core checks, compiles Java and resources, generates DEX, aligns the package, and signs a development APK.

Output:

```text
app/build/outputs/apk/debug/WattScope-1.1.3-debug.apk
```

Debug builds use a development signing key and enable debugging. Building on another computer or using a different build method may produce a different signing key, which can require uninstalling an existing version before installation. Export any records you need before uninstalling. Configure and securely retain your own release signing key for production distribution.

### Core checks and verification

Three standalone Java test programs cover 130 checks:

| Program | Coverage | Checks |
| --- | --- | --- |
| `PowerCoreTest.java` | Power conversion and session energy integration | 27 |
| `DisplaySettingsTest.java` | History, chart modes, polarity, and overlay scale | 63 |
| `RangeStatisticsTest.java` | Selected-range statistics and unchanged session energy accumulation | 40 |

These programs are in the separate `tests/` directory and use Java `main` entry points. Running `gradlew test` alone does not execute them. The standalone Windows build runs all three before packaging the APK. Instructions for running them independently are in [CONTRIBUTING.md](CONTRIBUTING.md).

The recorded validation includes all 130 checks, Android Java/resource compilation, DEX generation, APK alignment, and signature verification. Physical-device and emulator runtime validation remain pending. See [VERIFICATION.md](VERIFICATION.md) for the full record.

## Code structure

Production Java files are under `app/src/main/java/dev/wattscope/`.

| File | Responsibility |
| --- | --- |
| `BatteryReader.java` | Read Android battery APIs; handle units and unavailable data |
| `PowerMath.java` | Power conversion, polarity, and integration helpers |
| `SessionStats.java` | Accumulate session energy and valid observation time |
| `ChartData.java` | Select chart ranges, calculate range statistics, and downsample chart data |
| `SampleHistory.java` | Maintain the bounded sample history |
| `MonitorStore.java` / `Prefs.java` | In-memory monitoring state and local settings |
| `PowerMonitorService.java` | Foreground service, ongoing notification, and sampling |
| `OverlayController.java` | Floating overlay and gestures |
| `MainActivity.java` / `PowerChart.java` | Dashboard, permission guidance, chart interaction, and export |

Icons and themes are under `app/src/main/res/`. Build entry points are under `gradle/`, standalone checks under `tests/`, and third-party license text under `third_party/`.

## License and contributions

The project's own code, tests, documentation, and original resources are licensed under the [MIT License](LICENSE). Retain the applicable copyright and license notices when using or redistributing them.

The Gradle Wrapper and launcher scripts retain their Apache-2.0 license. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [third_party/gradle-LICENSE.txt](third_party/gradle-LICENSE.txt) for the upstream license text and component notices. Third-party terms apply to their respective components.

Bug reports and improvements are welcome through [Issues](https://github.com/Refract-Eclipse/WattScope/issues) and pull requests. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a contribution.

## Android API references

- [BatteryManager](https://developer.android.com/reference/android/os/BatteryManager)
- [Notification permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [TYPE_APPLICATION_OVERLAY](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
