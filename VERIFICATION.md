# WattScope 1.1.4 验证记录

日期：2026-10-25。版本 1.1.4，versionCode 9，包名 `dev.wattscope.guidetrial`，应用名“瓦特计”。

## 构建与安装包

- JDK 17.0.20.1、Gradle 8.13、AGP 8.11.1、Android SDK Platform 36 / Build Tools 36.0.0。
- 执行 `:app:testDebugUnitTest :app:assembleRelease`，退出码 0，BUILD SUCCESSFUL；Release 必要 lint 检查（lintVitalRelease）通过。未执行完整 lint，不将此结果表述为全部 lint 无问题。
- 发布文件 `WattScope-1.1.4.apk`：58,824 字节。APK v2 签名验证通过；zipalign 检查退出码 0。
- aapt2 验证版本 1.1.4 / 9、minSdk 26、targetSdk 36、应用名及包名正确；安装包未启用 debuggable。
- 六项权限与最新通用引导测试版一致（相对早期 GitHub 版新增 WAKE_LOCK 与 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS）：前台服务、特殊用途前台服务、通知、悬浮窗、唤醒锁、电池优化豁免请求。没有 INTERNET 权限。

APK SHA-256：`a7a90552c6a0583a9a2397b323be6d6e3c792f16d92346b1c29753ae72b8bf0d`

签名证书 SHA-256：`caa0a2f5015fe14f36e4bdb766fd9bf0ac2cd49d7cf9a17c359cebbde8bb1fc1`

证书与 `1.1.3-guide-test1` 相同，包名相同、versionCode 从 8 增至 9，满足该版本覆盖升级的签名和版本条件。沿用原开发证书以保留兼容性；本次构建关闭了调试属性。发布私钥不在仓库或源码归档中。

## 自动化检查

44 次 JUnit/Robolectric 测试执行，0 失败、0 错误：

| 测试组 | 执行次数 | 内容 |
| --- | --- | --- |
| CoreTests | 3 | 原 27 + 63 + 40 = 130 项功率、能量、显示与范围统计断言 |
| RecordingRegressionTest | 8 | API 28/35，获取/续期/释放唤醒锁，重复开始、服务销毁、首次引导 |
| BackgroundFlowTest | 21 | API 28/35，首次提醒、取消/跳过、已有豁免、通知拒绝、旋转恢复、品牌选择与系统返回 |
| BackgroundGuidanceTest | 4 | 子品牌优先、制造商兜底、未知输入、区域大小写、无效偏好 |
| BackgroundSettingsTest | 7 | API 28，真实包名 URI、已有授权、缺失和受限设置入口回退 |
| GuideLayoutTest | 1 | API 35，320×480、1.5 倍字体，内容可滚动且操作可见 |

本次生产 Java 与资源源文件和最新通用引导版逐字节相同；发布准备仅调整版本、显示名称、Release 签名配置及发布文档。原统计口径没有修改。MIT License 与仓库原文件一致。

## 仍需设备验证

- 本次未在真实手机或完整 Android 模拟器上安装测试；Robolectric 不是各厂商系统的替代品。用户此前只确认过其手机开启允许后台高耗电后可继续记录。
- 未执行 Windows 手工构建脚本；已与 Gradle 共用版本参数。发布 APK 来自此次实际验证的 Gradle Release 构建。
- 厂商设置无法通过统一公开接口确认；引导不保证所有机型持续记录。保持 CPU 唤醒会增加耗电并影响待机功耗。
- 记录仍只在内存中，进程终止、重新开始或修改校准会清零；升级前应导出需要保留的记录。
- 引导打开状态和品牌选择在旋转后保留，长文滚动位置暂不恢复。
