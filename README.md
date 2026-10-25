# 瓦特计 WattScope 1.1.4

**简体中文** | [English](README.en.md)

[下载安装包](https://github.com/Refract-Eclipse/WattScope/releases/tag/v1.1.4) · [更新记录](CHANGELOG.md) · [后台设置引导](docs/background-guidance.md) · [隐私说明](PRIVACY.md) · [MIT License](LICENSE)

Android 原生电池侧净功率监测工具，提供主界面、常驻通知、可拖动悬浮窗和功率曲线。最低 Android 8.0（API 26），目标 API 36；Java 原生界面，无第三方运行时依赖、无联网权限，无需 Root 或服务器。

## 1.1.4 更新

- 新增通用后台运行引导：覆盖 vivo/iQOO、小米/Redmi/POCO、OPPO、一加、realme、华为、荣耀、三星、Google/Pixel；未知品牌使用通用步骤。
- 自动推荐品牌说明，也可手动切换并记住选择；提供应用设置、手机设置和无法直接打开时的回退。
- 首次开始记录时解释息屏采样用途及额外耗电，可申请授权或暂不授权；已有 Android 电池优化豁免也会展示厂商说明。
- 将 Android 可检测的豁免状态与需手动核对的厂商开关分开。打开设置页不会被误记为授权成功。
- 记录期间使用可续期的部分唤醒锁支持息屏采样，停止或服务销毁后释放。
- 简化主界面，将权限、采样来源等详细状态放入“读数说明与诊断”；保留必要的运行状态和异常提示。
- 版本参数统一由 `version.properties` 提供；核心检查与授权、服务检查接入 Gradle 测试。发布 APK 使用关闭调试的 Release 构建。

## 安装与升级

下载 Release 中的 **WattScope-1.1.4.apk**。源码归档不能直接安装。

| 已安装版本 | 安装 1.1.4 的行为 |
| --- | --- |
| 通用引导测试版 `1.1.3-guide-test1` | 沿用包名和签名，可覆盖升级，保留设置 |
| 早期 GitHub 1.1.3 / 1.1.2（`dev.wattscope`） | 包名不同，会并存；需在新应用中重新授权 |
| 其他后台授权/息屏测试包 | 若包名不同则并存；不自动导入旧包设置和历史 |

1. 安装前导出需要保留的 CSV。升级会重启进程，内存中的记录不会随升级保留。
2. 打开“瓦特计”，点“开始监测”，按引导核对 Android 电池优化与厂商后台管理。
3. Android 13+ 按需允许通知；开启悬浮窗时允许“显示在其他应用上层”。
4. 锁屏几分钟后检查曲线或 CSV 是否连续。可随时从“息屏记录设置”重新打开说明。
5. 比较不同版本时，只运行一个监测程序，避免额外采样干扰耗电。

当前安装包沿用通用引导版的 `dev.wattscope.guidetrial`，版本名 1.1.4、versionCode 9，应用名称为“瓦特计”。沿用该包名是为了保持最新测试版的升级兼容性，不表示所有厂商均已实测。

## 功能与统计口径

- 显示净功率（W）、带符号电流（mA）、电压（V）、电量与温度；采样间隔可选 1、2、5 秒。
- 曲线范围：1/5/15 分钟、1 小时、全部、自定义 1–360 分钟。最近 6 小时最多 21601 个采样点用于曲线和 CSV。
- 所选范围只有充电或放电时，显示该方向的功率幅值并使用完整图高；同时存在两个方向时显示正负曲线。青绿色表示充入，橙色曲线表示放出。
- **平均幅值、峰值幅值按所选范围计算；累计充入、累计放电始终从本次记录开始累计。**切换范围不清除记录，也不重算整次能量累计。
- 平均幅值按有效连续采样的绝对功率积分除以有效时长计算；峰值取窗口内原始记录最大幅值，图形压缩不改变统计。
- 缺失读数或超过 `max(5 秒, 3 × 采样间隔)` 的间隔打断积分和曲线，不补造数据。
- 悬浮窗支持拖动、轻点返回、长按隐藏，标准/精简样式及 70%–200% 缩放。
- CSV 通过系统文件保存器导出；时间列使用 UTC，`net_power_W` 保留正负号，不可用数值为空。

## 读数与后台运行边界

功率估算为 `电压 V × 电流 mA / 1000`。正电流表示流入电池，负电流表示流出电池。充电时读数不是整机耗电或充电头输入功率，也不是 CPU/GPU 的独立功耗。

优先读取 Android `CURRENT_NOW`；仅接口不支持时回退至 `CURRENT_AVERAGE`。硬件可能更新较慢、不提供电流，或使用不同单位与方向。可在“读数校准”中调整单位、符号与电压倍率；修改校准会重置记录。双电芯不能仅凭型号或约 4 V 电压猜测是否乘 2，只有确认接口含义后才调整。

息屏记录会保持 CPU 唤醒，**增加实际耗电并改变待机功耗**。厂商后台限制、省电模式或系统清理仍可能中断记录；Android 已豁免不代表厂商限制已解除。品牌说明是设置指引，不是全机型持续采样保证。

监测不随开机自动启动，也不在进程终止后自动重启。采样和统计仅在进程内存中；进程终止、重新开始或修改校准会清零。需要保留时请先导出。

## 权限与隐私

| 权限 | 用途 |
| --- | --- |
| FOREGROUND_SERVICE / FOREGROUND_SERVICE_SPECIAL_USE | 用户主动开启的前台监测服务 |
| POST_NOTIFICATIONS | Android 13+ 监测通知 |
| SYSTEM_ALERT_WINDOW | 可选悬浮窗 |
| WAKE_LOCK | 记录期间保持 CPU 唤醒，停止后释放 |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 用户主动申请本应用的电池优化豁免 |

无 INTERNET、存储、无障碍、定位权限，无账号、广告或遥测。品牌信息只在本地用于选择说明。详见 [隐私说明](PRIVACY.md)。

## 构建与验证

需要 JDK 17、Android SDK Platform 36、Build Tools 36.0.0。工程使用 AGP 8.11.1、Gradle 8.13。

```bash
./gradlew testDebugUnitTest assembleDebug
```

Windows 使用 `gradlew.bat`。Debug APK 在 `app/build/outputs/apk/debug/app-debug.apk`，用于开发调试。JUnit/Robolectric 仅为测试依赖，不进入 APK；测试也执行原有 27 + 63 + 40 = 130 项核心断言。

发布构建使用 `assembleRelease`。需要通过环境变量提供 `WATTSCOPE_KEYSTORE`（私钥文件的绝对路径）、`WATTSCOPE_STORE_PASSWORD`、`WATTSCOPE_KEY_ALIAS`、`WATTSCOPE_KEY_PASSWORD`。未提供密钥时生成未签名的 Release APK；发布私钥不在仓库或源码归档中。自行生成的签名通常不能覆盖官方安装包。

Windows 的 `build-apk.ps1` 保留为开发构建入口，读取同一份版本参数，输出 `app/build/outputs/apk/debug/WattScope-1.1.4-debug.apk`；本次未在 Windows 上执行该脚本。GitHub Release 附件来自本地验证过的非调试构建。

实际验证结果和限制见 [VERIFICATION.md](VERIFICATION.md)。贡献方法见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 代码位置

| 文件 | 职责 |
| --- | --- |
| BatteryReader / PowerSample | 电池读数与样本 |
| PowerMath / SessionStats | 功率换算及整次能量积分 |
| SampleHistory / ChartData / PowerChart | 历史、范围统计、图表 |
| PowerMonitorService / MonitorStore | 采样、通知、唤醒锁及内存状态 |
| OverlayController / OverlayStyle | 悬浮窗与尺寸 |
| MainActivity | 主界面、记录入口及导出 |
| BackgroundGuidance / BackgroundGuideDialog / BackgroundSettings | 品牌说明、引导界面、系统设置及状态查询 |

源码位于 `app/src/main/java/dev/wattscope/`；引导文案在 `app/src/main/res/values/background_guidance.xml`。项目自身采用 [MIT](LICENSE)，Gradle 等组件保留各自许可，见 [第三方声明](THIRD_PARTY_NOTICES.md)。
