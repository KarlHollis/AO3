# AO3

基于 Android WebView 的个人 AO3 浏览器，专为在手机上流畅阅读 [Archive of Our Own](https://archiveofourown.org) 而设计。

> 本项目仅供个人使用，与 AO3 / OTW 无任何关联，也未对网站做任何形式的抓取或解析，仅通过 WebView 加载官方站点。

## 功能

- 全屏 WebView 加载 AO3 官方站点，支持 JavaScript / LocalStorage / Cookie，自动保持登录状态
- 浏览器操作：返回、前进、刷新、首页、搜索
- 多窗口支持（window.open / target=_blank）
- 长按链接菜单：打开、新窗口、复制、Chrome 打开、分享
- 文件下载：EPUB / PDF / HTML，保存到系统 Downloads，完成后通知
- 文件上传：支持网页 `input type=file`（相册 / 相机，Android 13+）
- 页面标题同步到顶部工具栏
- 深色模式：跟随系统自动适配夜间模式
- 全文翻译（简体中文）：按滚动位置逐段翻译，支持自动翻译新页面；使用 ML Kit 端侧翻译，首次使用需下载语言模型（约 30MB），之后可离线使用
- 最近浏览记录
- 加载进度条与网络错误提示

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- MVVM 架构
- minSdk 26 / targetSdk 37
- Gradle Kotlin DSL
- Google ML Kit（端侧翻译）

## 构建

环境要求：

- JDK 17+
- Android SDK Platform 37

```bash
./gradlew :app:assembleDebug
```

APK 输出在 `app/build/outputs/apk/debug/`。

## 安装

从 [Releases](https://github.com/KarlHollis/AO3/releases) 下载 APK 安装即可。

首次使用翻译功能时需要联网下载语言模型，其余功能均可离线使用。

## 版本历史

- **v1.2.3** — 当前版本：完整 WebView 浏览、翻译、下载、上传与深色模式
