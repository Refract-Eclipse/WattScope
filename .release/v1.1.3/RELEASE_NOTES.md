## 瓦特计 WattScope 1.1.3

Android 原生电池功率监测工具，支持通知栏、可拖动悬浮窗、功率曲线和 CSV 导出。最低 Android 8.0（API 26），无需 Root。

### 本次更新
- 平均幅值与峰值幅值按当前所选时间范围计算，切换范围后同步更新。
- 平均幅值按有效采样时长加权；峰值取范围内有效原始记录的最大绝对值，图形压缩不影响统计。
- 累计充入与累计放电继续从本次记录开始累计，不随图表范围改变。
- 保留单向充放电完整图高、混合方向正负曲线以及悬浮窗 70%–200% 缩放。

### 下载与安装
- 安卓手机安装请下载 **WattScope-1.1.3.apk**；下方 Source code 是源码包。
- APK 使用与此前 1.1.2 相同的开发签名，包名为 `dev.wattscope`，versionCode 为 5。
- 使用该签名安装的旧版本可覆盖升级；升级前请导出需要保留的记录，历史采样仅保存在进程内存。
- 首次使用按提示允许通知；需要悬浮窗时允许“显示在其他应用上层”。

### 验证与适用范围
- 130 项核心检查通过，Android 编译、DEX、APK v2/v3 签名与 ZIP 对齐验证通过。
- 本 APK 为带调试属性的开发测试包，尚未进行真机或模拟器运行验证。
- 读数是电池侧净功率估算，不等于充电头输入功率或 CPU/GPU 单独功耗。
- 无联网权限，监测数据留在设备本地。

### 文件与许可
- `WattScope-1.1.3.apk`：已签名安装包，45,640 字节。
- `SHA256SUMS.txt`：安装包与许可证的 SHA-256 校验值。
- `LICENSE.txt`：MIT 许可证；源码中的 Gradle 组件保留各自许可。
- [完整说明](https://github.com/Refract-PKUHSC/WattScope-Android/tree/v1.1.3#readme) · [第三方声明](https://github.com/Refract-PKUHSC/WattScope-Android/blob/v1.1.3/THIRD_PARTY_NOTICES.md)

APK SHA-256：
```text
f7c5eb331beed75d62c83d94d01250674d04f0b88e2615fc3d8411ed2f02a38d
```
