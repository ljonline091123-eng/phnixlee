# FiveLines iOS 云构建

这个项目使用 Codemagic 在 macOS 云端运行 Xcode，生成 IPA 并上传到 TestFlight。

## 需要准备

1. 一个 Apple Developer Program 账号。
2. 一个 Codemagic 账号。
3. 将 `C:\Android\FiveLines` 推送到 GitHub、GitLab 或 Bitbucket 私有仓库。仓库根目录必须保留 `codemagic.yaml` 和 `iOSFiveLines/FiveLines.xcodeproj`。

## 首次配置

1. 打开 <https://codemagic.io/>，使用代码仓库账号登录。
2. 添加 FiveLines 仓库。
3. 在项目设置中添加 Apple Developer Portal integration，并将该集成命名为 `codemagic`。这个名称需要和 `codemagic.yaml` 中的 `app_store_connect: codemagic` 一致。
4. 确认 Bundle ID 为 `com.example.fivelines`，如果项目中的 Bundle ID 不同，要把 `codemagic.yaml` 里的值改成相同值。
5. 确认 Codemagic 已关联 App Store Connect integration，然后启动 `ios-testflight` workflow。

`codemagic.yaml` 会自动执行 Xcode Archive/Export，并把 IPA 提交到 TestFlight。

## 在 iPhone 上安装

构建成功后，Codemagic 会把版本上传到 App Store Connect。打开 TestFlight，接受测试邀请，安装 FiveLines。

## 重要说明

- Apple 账号密码和私钥不要发给任何人；在 Codemagic 的授权页面完成登录即可。
- 当前配置默认使用 App Store 分发，因此适合 TestFlight。
- Bundle ID 已暂定为 `com.example.fivelines`。如果该 ID 已被占用，请在 Apple Developer 后台创建一个你自己的唯一 ID，并同时修改 `codemagic.yaml` 与 Xcode 工程的 Bundle ID。
- Codemagic 会在云端执行 Xcode，不需要在 Windows 电脑安装 Xcode 或模拟器。
