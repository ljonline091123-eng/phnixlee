# FiveLines iOS 版本

这是与 Android 版本规则一致的 SwiftUI 源码。项目使用 iOS 16 或更高版本、SwiftUI、Foundation 和 AVFoundation，不依赖第三方库。

## 在 Mac/Xcode 中运行

1. 在 Xcode 中新建 `iOS App`，界面选择 SwiftUI，最低部署版本选择 iOS 16 或更高。
2. 将本目录中的 `FiveLinesApp.swift`、`ContentView.swift`、`GameModel.swift` 加入 Xcode target，并删除 Xcode 自动生成的同名入口文件。
3. 运行 iPhone 模拟器即可测试；真机运行需要 Apple Developer 签名。
4. Windows 不能运行 Xcode，因此此处提供完整源码，`.ipa` 需要在 Mac 上由 Xcode 编译和签名生成。

## 已包含功能

- 9×9 棋盘、BFS 最短路径和逐格移动动画
- 白棋和炸药作为连线万能棋子
- 五连消除、炸药清除同色棋子、正确计分
- 随机棋子由小变大、连线缩小消失、炸药独立爆炸音效
- 难度、白棋概率、炸药概率、移动速度、音乐和音效设置
- 前十名高分榜、昵称录入、游戏结束重新开始
- UserDefaults 保存设置和高分榜；SwiftUI 状态在屏幕旋转时保留

## 应用图标

`AppIcon.svg` 是新的应用图标源文件。将它导入 Xcode 的 `Assets.xcassets/AppIcon.appiconset`，或用设计工具导出为 1024×1024 PNG 后作为 App Store 图标。

## 生成 iOS 安装包

Windows 无法运行 Xcode，也无法替 Apple 账号完成签名，因此这里不能直接生成可安装的 `.ipa`。在 Mac 上打开 Xcode 后，加入三个 Swift 文件和 `AppIcon.svg`，选择 Team，连接模拟器或真机运行；需要分发时选择 `Product > Archive`，再从 Organizer 导出 Ad Hoc、Development 或 TestFlight 包。
