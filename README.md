# ACG-Faka 原生 Android 客户端 Starter v0.2.0

这是本地 Kotlin + Jetpack Compose 客户端，不是 WebView。UI 和程序资源随 APK 安装，业务数据通过 HTTPS 服务端 API 获取。

## 本版实现
- 原生模块入口：插件、订单、商品/卡密、用户、网站设置、日志。
- 服务器根地址输入。
- HTTPS 版本策略检查：客户端版本、服务器最新版本、最低支持版本、强制更新开关。
- 公告弹窗：普通公告可关闭；强制公告不可通过返回/点外部关闭，只能确认。
- GitHub Actions 云编译 Debug APK。

## 重要限制
`GET /mobile-api/v1/bootstrap` 是**本项目提出的新增 API 协议**，不是已经核实存在于 ACG-Faka 中的端点。必须先在你的服务器实现该端点，否则版本/公告检查会显示连接失败。
其他业务模块目前仍是原生入口和占位内容。要实现真实数据操作，必须检查你部署的 ACG-Faka 源码，核对后台登录、权限、控制器路由和插件生命周期，然后实现服务端适配层。
当前公告“已知晓”没有跨启动持久化；这版是功能骨架，不是生产就绪版本。
强制更新只会阻止旧客户端继续使用并引导到下载页，不会绕过 Android 的安装确认。

## 云编译
1. 上传整个目录到 GitHub 仓库根目录。
2. 进入 Actions → Build Android APK → Run workflow。
3. 完成后从 Artifacts 下载 `acg-faka-mobile-debug-apk`。
4. 解压后安装 `app-debug.apk`。
5. 若构建失败，请提供 Actions 日志中首条编译错误。

## 修改应用版本
修改 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`，并同步修改 `MainActivity.kt` 的 `APP_VERSION_CODE` 与 `APP_VERSION_NAME`。`versionCode` 必须递增。

## 服务器接口
参阅 `server-adapter/BOOTSTRAP-API.md`。这是拟定协议，部署端需实际实现和保护。业务接口映射见 `server-adapter/API-MAPPING-TEMPLATE.md`。
