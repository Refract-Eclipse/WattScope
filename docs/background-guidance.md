# 通用后台运行引导

此版本使用同一套原生对话框提供首次记录与手动配置入口，适用于 Android APK 可运行的系统。配置说明是指引，不是机型认证或后台运行保证。

## 使用方法

1. 安装“瓦特计”1.1.4。旧版可保留；比较效果时不要同时运行两份监测。
2. 第一次点“开始监测”，阅读额外耗电说明，核对 Android 电池优化和厂商后台管理。
3. 品牌识别只推荐说明。若刷机、品牌识别不正确或菜单不匹配，点“选择手机品牌”，也可选通用说明。
4. “申请并开始”启动监测并申请 Android 豁免；需要时先申请通知权限。“暂不授权”也可开始。返回键取消不开始；已有 Android 豁免时按钮为“开始记录”和“取消”。
5. “打开应用设置”打开当前安装包的信息页；“打开手机设置”用于搜索说明中的关键词。找不到系统入口时会回退或提示手动操作。
6. 返回后会重新查询 Android 状态。厂商限制始终显示需手动核对，不会依据页面是否打开或系统豁免推断成功。
7. 配置后锁屏几分钟，观察曲线或导出的 CSV 是否连续。缺失样本不会补造。可从“息屏记录设置”随时重看。

## 说明覆盖

| 品牌 | 优先核对的选项 |
| --- | --- |
| vivo / iQOO | 后台耗电管理 → 允许后台高耗电；智能控制可能仍暂停采样 |
| 小米 / Redmi / POCO | 应用智能省电或省电策略 → 无限制 |
| OPPO | 应用耗电管理 → 允许后台活动 |
| 一加 | 应用电池管理 → 允许后台活动；兼顾 ColorOS / OxygenOS 菜单差异 |
| realme | 应用耗电管理 → 允许后台活动 |
| 华为 | 应用启动管理 → 手动管理 → 允许后台活动；仅指可运行 Android APK 的系统 |
| 荣耀 | 应用启动管理 → 手动管理 → 允许后台活动 |
| 三星 | 应用电池限制、休眠及深度休眠列表、从不休眠列表 |
| Google / Pixel | 应用电池用量 → 允许后台使用；如有不受限制选项，可选择 |
| 未识别品牌 | 应用信息和系统设置中搜索后台运行、电池优化、应用省电 |

最近任务加锁只作为减少一键清理影响的补充。本应用不添加开机自启、无障碍、精确闹钟或联网权限，不要求为所有应用全局关闭省电。

## 实现与维护

- `BackgroundGuidance`：先识别品牌，后以制造商兜底；未知值使用通用说明。字符串规范化使用 Locale.ROOT。
- `BackgroundGuideDialog`：自动/手动说明、滚动长内容、系统状态更新。品牌选择保存在本地。
- `BackgroundSettings`：PowerManager 查询和公开 Settings Intent；页面缺失或被设备策略拦截时依次回退。
- `MainActivity`：仅衔接一次性引导、通知权限和原有记录启动。旋转时恢复是否打开及首次待续状态；长文滚动位置暂不恢复。
- 文案位于 `res/values/background_guidance.xml`，新增厂商只需增加说明和识别映射；无需改采样或积分。

## 资料依据（2026-10-25 核对）

这些页面说明菜单或系统行为；有些针对网络/健康应用，这里只采用相关的后台设置，不照搬其自启动、联网或其他无关建议。厂商可随版本改变入口。

- [Android Doze / App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [Android Settings API](https://developer.android.com/reference/android/provider/Settings)
- [vivo 官方：后台耗电管理](https://kefu.vivo.com.cn/robot/imgmsgData/4ec81ca5ab19401687347b14173c9608/index_1.html?t=1)
- [小米官方：应用电池限制](https://www.mi.com/ph/support/faq/details/KA-538010/)
- [华为官方：OPPO 后台活动设置](https://consumer.huawei.com/en/support/content/en-us15848664/)
- [OxygenOS 14 用户手册](https://s3.eu-west-3.amazonaws.com/par-sow-cms-oppo-com/cms/default/1717410490532-3f874fba-7f9d-458b.pdf)
- [realme 官方：允许后台活动](https://www.realme.com/in/support/kw/doc/2068409)
- [华为官方：应用无法后台运行](https://consumer.huawei.com/cn/support/content/zh-cn00428704/)
- [荣耀官方：应用后台重新启动](https://www.honor.com/cn/support/content/zh-cn00406916/)
- [Samsung Developer：休眠应用管理](https://developer.samsung.com/mobile/app-management.html)
- [Google Pixel：应用后台电池使用](https://support.google.com/pixelphone/answer/6090599?hl=en)
