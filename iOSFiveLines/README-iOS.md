# FiveLines iOS 版本

这是按照 Android 当前版本重构的原生 SwiftUI 客户端。Android 的界面、规则、默认设置和音乐是产品基准；H5 是独立版本，不参与 iOS 构建。

## 在 Mac/Xcode 中运行

1. 用 Xcode 打开 `FiveLines.xcodeproj`；最低部署版本为 iOS 16。
2. 实际 target 源码位于 `Sources/FiveLinesApp.swift`、`Sources/CloudContentView.swift` 和 `Sources/CloudGameModel.swift`。
3. 选择模拟器或签名后的真机运行。仓库根目录的 `codemagic.yaml` 可生成 TestFlight IPA。
4. Windows 无法本地运行 Xcode，因此提交前要检查工程引用，并由 macOS/Codemagic 完成最终编译验证。

## 已包含功能

- 9×9 棋盘、BFS 最短路径和逐格移动动画
- 与 Android 一致的深色标题区、计分/预告栏、木色棋盘、红色菜单按钮和炸药造型
- 白棋和炸药作为连线万能棋子
- 五连消除、炸药清除同色棋子、正确计分
- 新棋子生成后自动检测连线；连线缩小消失，炸药使用独立爆炸音效
- 难度、白棋概率、炸药概率、移动速度、音乐和音效设置
- 前十名高分榜、昵称录入、游戏结束重新开始
- 分步骤聚焦真实棋盘的新手引导，不使用规则说明弹窗
- 按日期生成且跨 Android/iOS 一致的每日挑战、今日最佳分
- 最近 50 局历史战绩、八项持久化成就和解锁提示
- 每 12 次有效移动逐步增加生成压力；每局可撤销最近一步
- UserDefaults 保存设置和高分榜；SwiftUI 状态在屏幕旋转时保留

## 应用图标

`AppIcon.svg` 是应用图标源文件，可导出为 1024×1024 PNG 后加入 App Store 图标集。

## 生成 iOS 安装包

Windows 无法运行 Xcode，也无法替 Apple 账号完成签名。可在 Mac 上选择 Team 后执行 `Product > Archive`，或使用仓库已有的 Codemagic 工作流生成并提交 TestFlight 包。
