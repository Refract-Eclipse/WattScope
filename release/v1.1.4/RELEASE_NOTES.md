## WattScope 1.1.4：通用后台引导与息屏记录改进

本次更新重点改进不同品牌手机的后台设置引导，并精简主界面。

### 本次更新

- **通用后台引导**：覆盖 vivo/iQOO、小米/Redmi/POCO、OPPO、一加、realme、华为、荣耀、三星和 Google/Pixel。自动推荐品牌说明，也能手动切换；未知品牌提供通用步骤。
- **首次记录说明**：开始前解释息屏采样的用途与耗电影响，提供授权申请和暂不授权选项，后续可在“息屏记录设置”中重新配置。
- **授权状态更准确**：将 Android 电池优化豁免与厂商后台耗电限制分开。厂商开关需手动核对，打开设置页不会被误判为已授权。
- **改进息屏采样支持**：记录期间保持部分 CPU 唤醒并定期续期，停止监测后释放。仍需按手机说明允许相应后台活动。
- **页面更简洁**：详细权限、数据来源等状态默认收进“读数说明与诊断”，主页面保留必要数据和异常提示。
- **保留统计口径**：平均幅值和峰值幅值按所选范围计算；累计充入、累计放电从本次记录开始累计。

### 下载与升级

- 安卓手机请下载 **WattScope-1.1.4.apk**，最低 Android 8.0；源代码 ZIP 不能直接安装。
- 本版应用名为“瓦特计”，versionCode 为 **9**，包名沿用 `dev.wattscope.guidetrial`。
- **通用引导测试版 `1.1.3-guide-test1` 可覆盖升级并保留设置**，签名不变。
- **早期 GitHub 1.1.3 / 1.1.2（`dev.wattscope`）与本版并存**，需为新应用重新授权，不自动导入旧包设置。
- 升级会重启进程，请先导出需要保留的 CSV；历史采样只存内存。比较版本时只运行一个监测程序。

### 使用提示

持续息屏采样会增加耗电并影响待机功耗。Android 已豁免不代表厂商限制已解除，例如 vivo 的“智能控制”仍可能暂停记录，需要核对“允许后台高耗电”。

品牌说明是设置指引，不代表全部机型已经实测；省电策略、系统清理及传感器限制仍可能造成中断。缺失时段不会补造数据，进程被终止后不会自动恢复历史。

发布安装包关闭调试功能，无联网权限、广告或遥测。代码和原创资源继续采用 MIT License。

### 验证与文件

- 44 次 JUnit/Robolectric 测试执行通过，包含原有 130 项核心数值断言；Release 构建、必要 lint、APK 签名及 ZIP 对齐检查通过。
- 本次未在真实手机或完整 Android 模拟器上安装验证。
- `WattScope-1.1.4.apk`：已签名、关闭调试的安装包，58,824 字节。
- `WattScope-1.1.4-source.zip`：源码与测试，不包含私钥、SDK 或构建缓存。
- `SHA256SUMS.txt`：附件校验值；`LICENSE.txt`：MIT 许可证；`RELEASE_NOTES.md`：本公告。

APK SHA-256：
```text
a7a90552c6a0583a9a2397b323be6d6e3c792f16d92346b1c29753ae72b8bf0d
```

[完整说明](https://github.com/Refract-Eclipse/WattScope/tree/v1.1.4#readme) · [后台设置步骤](https://github.com/Refract-Eclipse/WattScope/blob/v1.1.4/docs/background-guidance.md) · [验证记录](https://github.com/Refract-Eclipse/WattScope/blob/v1.1.4/VERIFICATION.md)
