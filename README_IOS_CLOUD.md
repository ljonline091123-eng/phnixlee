# 五彩连线 iOS 云构建

## 文件位置

- Xcode 工程：`iOSFiveLines/FiveLines.xcodeproj`
- Codemagic 工作流：`codemagic.yaml`
- 详细说明：`IOS_CLOUD_BUILD.md`

## 最少操作步骤

1. 在浏览器打开 GitHub，新建一个私有仓库，例如 `FiveLines`。
2. 进入仓库的 `Add file` -> `Upload files`，把 `C:\Android\FiveLines` 中的项目文件上传。
3. 打开 <https://codemagic.io/>，使用 GitHub 登录并添加这个仓库。
4. 在 Codemagic 的 Team integrations 中添加 App Store Connect integration，名称填写 `codemagic`。
5. 在 Apple Developer/App Store Connect 中创建应用，Bundle ID 使用：

   `com.example.fivelines`

   如果这个 ID 已被占用，需要改成自己的唯一 ID，并同步修改 `codemagic.yaml` 与 Xcode 工程中的 Bundle ID。

6. 运行 `ios-testflight` workflow。
7. 构建成功后，在 App Store Connect 的 TestFlight 页面接受测试邀请，并在 iPhone 上安装 TestFlight。

Apple 账号授权在 Codemagic 页面完成，不要把 Apple 密码写进仓库或发送给任何人。
