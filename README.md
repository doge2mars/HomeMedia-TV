# HomeMedia-TV

家庭影音 HomeMedia 的电视端应用：通用版（B）/ 专用版（C）加一个一键关机小应用。服务端在 [doge2mars/HomeMedia](https://github.com/doge2mars/HomeMedia)（Go + SQLite，自托管）。

## 目录

| 目录 | 应用 | 包名 | 当前版本 |
|---|---|---|---|
| `homemedia-tv-b/` | 通用版 B（直连版） | `org.moontechlab.homemedia.general` | 1.4.16（code 29） |
| `homemedia-tv-c/` | 专用版 C（代理版，双栈） | `org.moontechlab.homemedia` | 1.2.5（code 17） |
| `shutdown-app/` | 关机 | `org.moontechlab.shutdown` | 1.3（code 4） |
| `releases/` | 已编译发布的 APK 安装包 | - | B 1.4.16 / C 1.2.5 |

三个应用包名不同，可装在同一台盒子上共存。

## 已编译安装包（`releases/`）

| 文件 | 适用版本 | 架构 |
|---|---|---|
| `HomeMedia-dedicated-1.2.5-arm64-v8a.apk` | 专用版 C `1.2.5` | 64 位 Android TV / 电视盒（推荐新盒子） |
| `HomeMedia-dedicated-1.2.5-armeabi-v7a.apk` | 专用版 C `1.2.5` | 32 位老电视盒（如 Android 7/9） |
| `HomeMedia-generic-1.4.16-arm64-v8a.apk` | 通用版 B `1.4.16` | 64 位 Android TV / 电视盒 |
| `HomeMedia-generic-1.4.16-armeabi-v7a.apk` | 通用版 B `1.4.16` | 32 位老电视盒 |

## B 版与 C 版：有什么不一样

两个版本基于同一套 HomeMedia-TV 代码架构二次开发，区别主要在“片子与直播怎么播”：

| | B 版（通用版） | C 版（专用版） |
|---|---|---|
| 播放路径 | 电视直连源站 CDN | 默认走所连服务器的服务端代理（`/api/proxy/m3u8`），代理前缀自动跟服务器地址走，设置里不用填 |
| 去广告 | 没有（原样播） | 有，服务端过滤广告分片 |
| 分享页的源（如电影天堂） | 播不了（拿到的是 HTML 落地页） | 能播，服务端把分享页解析出真链再播 |
| 播放模式（设置 → 播放模式） | 只有直连 | 三档：**仅代理**（默认）、**代理优先**（代理失败自动切直连一次，位置不断，顶上提示“代理失败，已切换直连播放”）、**仅直连** |
| 电视直播与解码 | 基础直连直播 | 直播三段式回退（直连 → `/api/proxy/m3u8?live=1` → `/api/proxy/stream`）；内置 ExoPlayer + `media3-ffmpeg-decoder`（软硬解兼容 4K 与老设备） |
| 直播遥控交互 | 常规底部列表 | 左侧半透明双栏选台抽屉：按 **左键** 或 **OK 键** 呼出频道列表，再按 **左键** 切换至分组栏，**上下键** 快速换台（不影响点播时的 OK 暂停） |
| 连 LunaTV | 能用（搜、详情、直连播都正常） | 也能用：选“仅直连”或“代理优先”（代理接口 404 会自动切直连）；但去广告、分享页解析、货架都没有（那是 HomeMedia 才有的） |
| 连 HomeMedia | 能用 | 能用，全部功能 |
| 货架（按分类逛目录） | 有（服务端目录驱动） | 有（服务端目录驱动） |

一句话：**日常求稳用 B；要去广告、要播分享页源、要更好的电视直播与遥控选台体验，用 C。** C 的“代理优先”是折中：代理行就走代理，代理挂了自动切直连。

找片和播片是两回事，两个版本一样：
- **搜片名**：服务端实时去各源现搜（不是查库）。
- **货架**：查服务端目录库（每天凌晨同步），按分类、地区翻。
- **继续观看**：服务端存的播放记录；同一账号登录，B 和 C 的记录是同一份，B 上看到一半，C 上能接着看。
- 点开一部片拿集数：实时去源站现取（库里只有标题、年份、分类、海报，没有集数和播放地址）。

## 服务端要求

| 服务端 | 搜索 / 详情 / 集数 | 直连播放 | 代理播放（去广告、分享页、直播流） | 货架 |
|---|---|---|---|---|
| HomeMedia（`:8090`） | ✅ | ✅ | ✅ | ✅ |
| LunaTV（`:3000`） | ✅ | ✅ | ❌（没有代理接口） | ❌ |

## 构建

前置：JDK 17+、Android SDK（compileSdk 36）、Gradle 9.3.1。两个版本各有一个一键脚本（`homemedia-tv-b/build-apk-b.sh`、`homemedia-tv-c/build-apk-c.sh`，默认按本地路径写，可用环境变量改）：

```bash
cd homemedia-tv-c
export ANDROID_HOME=/opt/android-sdk   # 改成你的 SDK 路径
./build-apk-c.sh                       # 产物在 apk-out-c/
```

或直接用 Gradle（Android Studio 也行）：

```bash
./gradlew assembleRelease
```

签名：仓库里**没有**签名文件。发布包要自己建 keystore，在项目根目录放一个 `key.properties`（`storeFile` / `storePassword` / `keyAlias` / `keyPassword`），不要提交进仓库。`*.jks`、`key.properties`、`local.properties` 已在 `.gitignore` 里。

关机应用不用 Gradle，用 `shutdown-app/build-shutdown.sh`（javac + d8 + aapt2 + zipalign + apksigner 手工打包），同样要自己建 keystore（脚本首次运行会自动生成一个，口令只存本地 `keystore.pass`）。

## 安装

```bash
adb connect <盒子 IP>:5555
adb install -r releases/HomeMedia-dedicated-1.2.5-arm64-v8a.apk
# 老 32 位盒子请使用 armeabi-v7a 版本：
# adb install -r releases/HomeMedia-dedicated-1.2.5-armeabi-v7a.apk
```

装好后打开应用登录一次，把服务器设到你的 HomeMedia（或 LunaTV）地址。C 版默认仅代理，连 HomeMedia 即走代理；连 LunaTV 请把播放模式改成“代理优先”或“仅直连”。

## 关机应用

电视桌面上的“关机”磁贴，点开即关机，不用确认，老人一键用。

- 靠 root 执行 `reboot -p`；盒子没 root 时屏幕显示“关机失败：需要 root 权限”，不闪退。
- 不同盒子的 `su` 语法不一样（有的认 `su -c`，有的只认 `su <uid> <命令>`），应用会轮着试：`su -c reboot -p`、`su 0 reboot -p`、`su root reboot -p`，再加 `svc power shutdown` 的两种写法，最后兜底 `reboot -p`。
- 带 Leanback 横幅，电视桌面直接显示；也保留普通启动器入口。
- 注意：代码里不要用 API 26 才有的方法（比如 `Process.waitFor(timeout, unit)`），老盒子（安卓 7.1）上会直接崩——这就是 1.3 修的那个 bug。

## 说明与特性演进

本仓库为 HomeMedia-TV 客户端长期二次开发版本，配合 [doge2mars/HomeMedia](https://github.com/doge2mars/HomeMedia) 服务端使用：

- **B 版（`homemedia-tv-b/`）**：包名 `org.moontechlab.homemedia.general`，聚焦直连播放与通用兼容，设置组默认折叠、支持各首页栏目开关。
- **C 版（`homemedia-tv-c/`）**：包名 `org.moontechlab.homemedia`，深度适配 HomeMedia 服务端（自动代理前缀、HLS MIME 显式声明、仅代理 / 代理优先 / 仅直连三档播放模式、测速优选防闪退、直播三段式回退、ExoPlayer `media3-ffmpeg-decoder` 扩展解码器、左侧半透明双栏直播选台抽屉与遥控器左键/OK键交互）。
- **关机应用（`shutdown-app/`）**：独立轻量级一键关机工具。
