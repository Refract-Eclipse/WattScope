# 瓦特计 WattScope 1.1.3

[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84)](app/build.gradle)
[![Version 1.1.3](https://img.shields.io/badge/version-1.1.3-blue)](CHANGELOG.md)
[![License MIT](https://img.shields.io/badge/license-MIT-green)](LICENSE)

Android 原生电池功率监测应用，支持主界面、常驻通知和可拖动的跨应用悬浮窗。最低 Android 8.0（API 26），编译及目标 API 36。不依赖 Google 服务，不需要 Root，也不申请联网、无障碍或文件管理权限。

这是 Android 工程，监测与计算均在手机本地完成，无需部署后端服务。工程使用 Java 和 Android 原生界面。

[更新记录](CHANGELOG.md) · [贡献与测试](CONTRIBUTING.md) · [隐私说明](PRIVACY.md) · [验证记录](VERIFICATION.md) · [第三方许可](THIRD_PARTY_NOTICES.md)

本次发布提供完整源码和构建入口；可按下方步骤自行生成 APK。源码目录不包含预编译 APK 或签名私钥。

## 安装与开始使用

1. 按下方编译步骤生成 APK，传到安卓手机并打开安装。如果系统提示，在安装器中允许当前文件来源安装应用。
2. 打开“瓦特计”，点“开始监测”。Android 13 及以上请允许通知权限，以便在下拉通知栏看到实时数据。
3. 打开“屏幕悬浮窗”。首次使用会跳转到系统授权页面，允许“显示在其他应用上层”后返回应用。
4. 在“功率曲线”上方切换时间范围；点“自定义”可输入 1–360 分钟。按住曲线可查看某个时刻的读数，切换范围不会清除已记录的数据。
5. 在“屏幕悬浮窗”卡片中拖动“窗口大小”滑杆设置 70%–200%，即时生效；点“恢复 100%”可还原，点样式按钮切换标准/精简。位置、大小、样式和时间范围都会记住。
6. 悬浮窗支持拖动移动、轻点返回应用、长按隐藏；也可从通知中隐藏悬浮窗。
7. 在应用或通知中点“停止监测”即可停止后台采样并关闭悬浮窗。

需要查看通知时，下拉通知栏展开瓦特计通知。状态栏使用闪电图标，具体瓦数显示在通知内容和悬浮窗中。

## 本版更新

- 修正曲线下方统计口径：平均幅值和峰值幅值按当前所选时间范围计算，切换范围或旧记录滚动过期时同步更新。
- 平均幅值继续按有效采样时间加权，峰值取所选范围内全部有效原始记录的最大绝对值；图形压缩不会影响这两项统计。
- 累计放电、累计净充入及本次有效采样时间继续从本次开始记录时累计，不随图表范围变化。
- 无有效记录时显示“—”；只有一个孤立读数时可显示峰值，平均值等待有效的连续采样间隔。

- 根据所选时间范围自动决定显示方式：只有充电时用青绿色显示充电功率幅值；只有放电时用橙色显示放电功率幅值，均使用完整图高和非负刻度，隐藏另一方向的空白区域和图例。
- 只有范围内同时出现充电和放电，才使用带正负号的净功率：正值充电在零线上方，负值放电在零线下方。
- 按住曲线显示时间与方向：单向模式显示功率绝对值，混合模式显示带正负号的净功率。长时间范围继续保留正向充电峰值和负向放电峰值。
- 判断只使用选定范围内的有效原始记录，不按当前是否插着充电器判断。切换范围、旧记录滚动过期时自动重判；零值和缺失值不会构成另一种方向。

- 全新的深蓝仪表盘、大号功率数字、分层卡片、渐变曲线和统一图标。
- 可调整曲线时间范围，最近 6 小时原始采样用于显示与 CSV 导出。
- 悬浮窗大小滑杆即时调整，切换大小不会重置采样或位置；窗口接近屏幕边缘时会自动保持在可见范围内。
- 版本号升级为 1.1.3（versionCode 5）。使用相同签名的安装包可覆盖升级并保留设置；自行构建时需使用自己的签名。升级会重启进程，内存中的上一段监测记录需提前导出。

## 功能

- 实时电池净功率（W）、带符号电流（mA）、电压（V）、剩余电量、电池温度。
- 1、2、5 秒采样间隔可选。刷新频率不等于硬件传感器的更新频率。
- 曲线支持 1 分钟、5 分钟、15 分钟、1 小时、全部及自定义 1–360 分钟，保留最近 6 小时。缺失数据和明显采样中断不连线；长窗口保留峰谷，按住曲线可查看时间和功率。
- 所选时间范围的平均功率幅值和峰值；本次记录的累计放电能量和累计净充入能量。
- 标准和精简两种悬浮窗，窗口、字号及内边距支持 70%–200% 连续缩放。
- 导出最近 6 小时最多 21601 个采样点为 UTF-8 CSV，使用系统文件保存器选择位置。
- 电流单位、正负号、电压倍率的手动校准；修改校准会重置本次采样和统计。

## 读数的含义与边界

功率来自 Android 公共电池接口：

```text
带符号电流 mA = CURRENT_NOW 的 µA / 1000
带符号净功率 W = 电压 V × 带符号电流 mA / 1000
界面主数字 = 净功率的绝对值
```

正电流表示净电流流入电池，界面显示“电池净充入”；负电流表示从电池净流出，显示“电池净放出”。主界面的功率大数字虽然取绝对值，但旁边的方向标签会注明净充入或净放出。曲线按所选范围自动切换：仅有一种方向时显示该方向功率的绝对值，使用完整图高；同时有充电和放电时才采用正负表示，上半区为正值净充电、下半区为负值净放电。标题、图例、刻度和触摸提示同步切换。CSV 中的 `net_power_W` 同样保留正负号。

拔掉充电器时，电池净放出功率可用于近似观察整机电池侧负载。接着充电器时，本应用显示的是电池净充入或净放出的功率，不能直接当作整机耗电功率或充电头输入功率。本应用也不提供 CPU、GPU、单个应用的独立实际瓦数。

优先读取 `BATTERY_PROPERTY_CURRENT_NOW`；仅当接口返回不支持标志时，回退到 `CURRENT_AVERAGE` 并在界面标注“硬件平均电流”。系统返回的零电流保留为零，不会擅自改成其他读数。电流与电压无法保证由硬件在完全相同的时刻采集，所以功率属于估算值。

如果手机没有开放电流接口，显示“功率不可用”，同时仍显示可获取的电压、电量和温度。不同手机的精度、更新频率和厂商缩放可能不同；当前版本尚未进行真机或模拟器运行验证。

### 校准

默认设置采用 Android 标准：原始电流单位 µA，正值充入，电压倍率 1×。如果你核实厂商接口输出的是 mA，可在“读数校准”中修改。反转正负号只用于修正已确认的厂商方向差异。

双电芯手机不能仅凭型号或“电压约 4 V”就认定要乘 2：厂商可能已经对电流或电压进行折算。只有确认接口提供的是单节电压且当前电流对应串联电池包时，才使用 2×；否则保留默认值。软件无法在缺少硬件信息时保证双电芯的绝对功率精度。

### 统计与后台运行

能量按真实单调时钟间隔进行梯形积分，充电与放电分开累计；跨越零功率时分别积分正负区间。缺失读数或超过 `max(5 秒, 3 × 采样间隔)` 的时间缺口不计入能量。平均功率幅值只统计所选时间范围内的有效连续采样间隔，按实际有效积分时间加权，不按样本数量简单平均；跨零时分别积分正负功率的绝对幅值。范围边界外的记录不参与均值和峰值，不向边界补造读数。累计充入和累计放电仍覆盖整次监测，不受时间范围选择或旧原始记录移出 6 小时缓存影响。

后台采样由用户主动启动的前台服务运行，通知中始终提供停止入口。退出主界面后可继续采样；本应用不申请唤醒锁，深度休眠或厂商后台限制可能导致采样暂停。缺失时间不会补造读数，因此累计能耗是有效采样时段的估算，不是长时间息屏的完整电量审计。需要持续后台显示时，可检查系统是否限制了本应用的后台活动。

监测不会自动开机启动，进程被系统终止后也不会自动重启。最近最多 6 小时的采样和本次统计仅保存在内存，重新开始监测、进程重启或修改校准都会清零；需要保留时请先导出 CSV。显示与采样本身会消耗少量电量。

## 权限与数据

| 权限 | 用途 |
| --- | --- |
| FOREGROUND_SERVICE | 用户主动开启的后台监测 |
| FOREGROUND_SERVICE_SPECIAL_USE | Android 14+ 前台监测服务类型 |
| POST_NOTIFICATIONS | Android 13+ 通知栏显示 |
| SYSTEM_ALERT_WINDOW | 在其他应用上方显示悬浮窗 |

不申请 `INTERNET`、`BATTERY_STATS`、存储或无障碍权限。配置和悬浮窗位置留在手机本地，采样留在进程内存，只有用户主动导出才写入所选文件。

## CSV

`time_utc` 为 UTC 时间；`elapsed_ms` 为设备开机以来的单调毫秒。`net_power_W` 正值充电、负值放电，`absolute_power_W` 为幅值。其余列包含电流、电压、电量、温度、采样源和异常说明。不可用的数值导出为空值。

## 源码编译

### Android Studio / Gradle

1. 用 Android Studio 打开本目录（含 `settings.gradle`）。
2. 使用 JDK 17，安装 Android SDK Platform 36，让 Gradle 完成依赖同步。
3. 执行 `gradlew.bat assembleDebug`（macOS/Linux 使用 `./gradlew assembleDebug`，必要时先赋予执行权限）。
4. 安装 `app/build/outputs/apk/debug/app-debug.apk`。

工程使用 Android Gradle Plugin 8.11.1、Gradle 8.13，应用本身没有外部运行时依赖。首次构建仍需下载 Gradle 和构建插件，工具不包含在源码压缩包中。

### Windows 独立构建

安装 JDK 17+、Android SDK Platform 36 和 SDK Build Tools 36.0.0，设置好 `JAVA_HOME`、`ANDROID_HOME` 后：

```powershell
.\build-apk.ps1
```

也可指定工具路径：

```powershell
.\build-apk.ps1 -JavaHomePath 'C:\your-jdk' `
    -PlatformJarPath 'C:\your-sdk\platforms\android-36\android.jar' `
    -BuildToolsPath 'C:\your-sdk\build-tools\36.0.0'
```

脚本会执行核心数值检查、编译 Java 和资源、生成 DEX、对齐并签署开发版 APK。产物为 `app/build/outputs/apk/debug/WattScope-1.1.3-debug.apk`。

上述 debug 构建使用开发签名并开启调试属性，适合开发测试，不代表已通过应用商店审核。源码不包含私钥。在其他电脑或使用另一构建方式时可能生成不同的开发签名，覆盖安装可能要求先卸载旧版本；卸载前先导出需要保留的数据。正式分发需自行配置、妥善保管发布签名。

### 核心检查

三组纯 Java 检查覆盖功率与能量计算、曲线与显示设置、所选范围统计，共 130 项。它们位于独立的 `tests/` 目录，执行方法见 [贡献指南](CONTRIBUTING.md#运行核心检查)。单独运行 `gradlew test` 不会执行这些检查。

## 代码位置

| 文件 | 职责 |
| --- | --- |
| BatteryReader.java | 读取系统电池接口，处理单位和缺失数据 |
| PowerMath.java / SessionStats.java | 功率换算、方向和能耗积分 |
| ChartData.java / SampleHistory.java | 时间范围筛选、均值与峰值、曲线压缩及历史缓存 |
| MonitorStore.java / Prefs.java | 内存采样状态和本地配置 |
| PowerMonitorService.java | 前台服务、常驻通知、采样调度 |
| OverlayController.java | 悬浮窗和手势 |
| MainActivity.java / PowerChart.java | 主界面、权限引导、曲线和导出 |
| tests/PowerCoreTest.java | 27 项核心数值检查；DisplaySettingsTest.java 的 63 项图表与设置检查，RangeStatisticsTest.java 的 40 项范围统计和能量累计回归检查 |

生产 Java 代码均在 `app/src/main/java/dev/wattscope/`；`app/src/main/res/` 保存图标和主题，`gradle/` 保存构建入口，`third_party/` 保存第三方许可文本。

## 开源许可

本项目自身的代码、测试、文档和原创资源采用 [MIT License](LICENSE)，允许使用、修改和再分发，须保留相应版权与许可声明，具体条件以许可证原文为准。

Gradle Wrapper 和启动脚本保留 Apache-2.0 许可；完整上游许可文本与组件范围见 [第三方声明](THIRD_PARTY_NOTICES.md) 和 [Gradle 许可文件](third_party/gradle-LICENSE.txt)。第三方许可独立适用于对应组件。

反馈问题和提交改进请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 官方接口资料

- [BatteryManager](https://developer.android.com/reference/android/os/BatteryManager)
- [通知权限](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [前台服务类型](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [TYPE_APPLICATION_OVERLAY](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)

本次验证范围见 `VERIFICATION.md`。
