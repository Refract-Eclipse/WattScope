# 贡献指南

欢迎通过 [Issues](https://github.com/Refract-PKUHSC/WattScope-Android/issues) 反馈问题，或通过 Pull Request 提交改进。

## 问题反馈

请提供应用版本、手机型号、Android 版本、采样间隔、校准设置、所选图表范围和复现步骤。功率问题请说明充电器连接状态以及预期与实际读数。截图、CSV 和日志请先检查并移除不希望公开的信息。

## 开发环境

使用 JDK 17、Android SDK Platform 36、Gradle Wrapper 8.13。构建步骤见 [README](README.md#源码编译)。应用为原生 Java，实现位于 `app/src/main/java/dev/wattscope/`，无外部运行时库。

## 运行核心检查

`tests/` 中三个检查程序使用 Java `main` 入口，不依赖 JUnit 或 Android 运行环境；普通 `gradlew test` 不会自动执行这些独立程序。

在仓库根目录打开 PowerShell，确认 JDK 17 的 `javac`、`java` 在 PATH 中：

```powershell
$source = 'app/src/main/java/dev/wattscope'
$core = @('PowerMath', 'PowerSample', 'SessionStats', 'SampleHistory', 'ChartData', 'OverlayStyle') |
    ForEach-Object { "$source/$_.java" }
$tests = @('tests/PowerCoreTest.java', 'tests/DisplaySettingsTest.java', 'tests/RangeStatisticsTest.java')
New-Item -ItemType Directory -Path 'build/core-tests' -Force | Out-Null
javac -encoding UTF-8 -source 8 -target 8 -d build/core-tests @core @tests
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
foreach ($suite in @('PowerCoreTest', 'DisplaySettingsTest', 'RangeStatisticsTest')) {
    java -cp build/core-tests "dev.wattscope.$suite"
    if ($LASTEXITCODE -ne 0) { throw "Test failed: $suite" }
}
```

macOS / Linux：

```sh
(
  set -e
  mkdir -p build/core-tests
  javac -encoding UTF-8 -source 8 -target 8 -d build/core-tests \
    app/src/main/java/dev/wattscope/PowerMath.java \
    app/src/main/java/dev/wattscope/PowerSample.java \
    app/src/main/java/dev/wattscope/SessionStats.java \
    app/src/main/java/dev/wattscope/SampleHistory.java \
    app/src/main/java/dev/wattscope/ChartData.java \
    app/src/main/java/dev/wattscope/OverlayStyle.java \
    tests/PowerCoreTest.java tests/DisplaySettingsTest.java tests/RangeStatisticsTest.java
  for suite in PowerCoreTest DisplaySettingsTest RangeStatisticsTest; do
    java -cp build/core-tests "dev.wattscope.$suite"
  done
)
```

当前预期结果分别为 27、63、40 项通过。Windows 独立构建脚本 `build-apk.ps1` 也会在构建 APK 前执行这三组检查。

## 修改时需要保持的行为

- 电池净充入为正、净放出为负；UI 幅值与 CSV 带符号净功率保持明确区分。
- 平均幅值与峰值跟随所选时间范围，累计充入/放电跟随整次监测。
- 缺失读数与长采样间隙不补造能量，不把无效值当作零。
- 原始历史最多保留 6 小时；显示抽样不改变原始记录统计。
- 停止监测后及时停止服务和悬浮窗；不无故新增权限。

涉及计算的修改请运行上述检查；涉及界面、权限或后台服务的修改请补充真机/模拟器验证，并在 PR 中说明实际验证范围。

## 提交与许可

保持每个 PR 聚焦一个问题，说明原因、行为变化和验证结果。不要提交签名密钥、令牌、本机 SDK 路径、缓存、生成的 APK 或包含个人信息的采样数据。

提交贡献即表示你有权提供该内容，并同意项目原创贡献按 [MIT License](LICENSE) 发布。新增第三方内容须在 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 中记录来源和适用许可，保留必要声明；本项目不要求签署额外 CLA。
