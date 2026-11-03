# Quick Launch

Quick Launch 是一个轻量的 Android 快捷启动器。可以保存应用链接和网页链接，从应用列表、桌面小组件或单独的桌面图标打开。入口和设置存储在设备本地。

## 使用

1. 点「添加链接」，填写名称并粘贴 URL，保存后点击名称试运行。
2. 想固定某一项，点该入口旁的「桌面」。也可以在首页点「添加桌面小组件」，把列表中的前三个入口放到桌面。
3. 在「管理」中编辑或删除入口，小组件会同步更新。

应用预置了哔哩哔哩的「我的收藏」。保存 `https://space.bilibili.com/<用户 ID>/favlist?fid=<收藏夹 ID>` 格式的网址时，Quick Launch 会在启动时根据收藏夹 ID 转为哔哩哔哩 App 的播放列表链接。跳转效果取决于目标应用的版本和账号权限，桌面固定功能取决于手机的桌面启动器。

启动流程参考 [Anywhere- 的 Normal 模式](https://zhaobozhen.github.io/Anywhere-Docs/guide/URL-Scheme-Usage.html)。Quick Launch 使用 Android 的 `ACTION_VIEW` 打开链接。

## 构建

应用支持 Android 8.0 及以上。构建需要 JDK 17、Android SDK Platform 35、Android Build Tools 35.0.0 和 `zip`。在 macOS 或 Linux 上运行：

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
./build.sh
```

APK 生成在 `build/QuickLaunch.apk`。首次构建会在 `build/local-signing.jks` 生成本地测试签名密钥。后续为同一台设备构建更新时，请沿用此前的签名密钥。

## 许可证

[MIT](LICENSE)
