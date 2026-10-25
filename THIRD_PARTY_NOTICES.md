# 第三方组件与许可声明

本清单对应 WattScope Android 1.1.4。项目自身的代码、测试、文档和原创资源采用根目录的 [MIT License](LICENSE)；第三方文件保留各自许可，MIT 不替代这些许可。

## 随源码分发的组件

| 组件 / 文件 | 来源 | 许可与说明 |
| --- | --- | --- |
| `gradle/wrapper/gradle-wrapper.jar` | [Gradle v8.13.0](https://github.com/gradle/gradle/tree/v8.13.0) | Apache-2.0；用于启动构建，不作为应用运行时库打入 APK |
| `gradlew`、`gradlew.bat` | Gradle 启动脚本 | Apache-2.0；保留原始版权和许可头 |
| `third_party/gradle-LICENSE.txt` | [Gradle v8.13.0 LICENSE](https://github.com/gradle/gradle/blob/v8.13.0/LICENSE) | 完整保留上游发布的许可文本及其组件声明 |

Gradle Wrapper JAR 经核对与上游 v8.13.0 文件逐字节一致，SHA-256：

```text
81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f
```

Gradle 上游许可汇总包含其构建工具依赖的 Apache、MIT、BSD、EPL、LGPL 等声明；这些条目不表示 WattScope 应用本身使用了相应运行时依赖，也不改变项目自身的 MIT 许可。

## 使用但不随本仓库分发的构建工具

- Gradle 8.13 完整发行版由 Wrapper 按配置下载，校验值保存在 `gradle-wrapper.properties`。
- Android Gradle Plugin 8.11.1 由 Gradle 从 Google Maven 下载。
- JDK、Android SDK Platform 36、Android SDK Build Tools 由开发者自行安装，各工具及其依赖适用各自的许可条款。

仓库不包含完整工具链、SDK、签名私钥或这些工具的缓存。应用代码仅调用 Android/Java 平台 API，当前 `app/build.gradle` 没有声明外部运行时库。

## 分发与贡献

分发项目源码时请保留根目录 `LICENSE`、本文件、`third_party/gradle-LICENSE.txt` 以及第三方文件原有许可头。新增第三方代码、字体、图标或其他资源时，应同时补充来源、版本、许可和必要的版权声明；修改第三方文件时标明修改内容。


## 仅测试依赖（1.1.4）

Gradle 测试使用 JUnit 4.13.2（Eclipse Public License 1.0）及 Robolectric 4.14.1（MIT），其许可信息以对应 Maven POM 和上游项目为准。测试依赖及其传递依赖通过 Gradle 获取，不作为运行时依赖打入 APK，也不把其二进制打包进本源码归档。AndroidX 仅由 Robolectric 测试依赖引入。
