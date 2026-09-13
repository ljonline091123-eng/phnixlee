# 重新生成 APK

在 Windows 电脑上双击 `rebuild-apk.bat`，脚本会执行：

1. `gradlew.bat clean`
2. `gradlew.bat assembleDebug`
3. 将 `app/build/outputs/apk/debug/app-debug.apk` 复制为项目根目录的 `FiveLines-debug.apk`

当前环境的 Windows 命令行进程无法启动，返回系统错误 `0xC0000142`，因此本次无法替你实际执行 Gradle。这个错误发生在 Gradle 启动之前，不是 APK 源码的编译错误。

如果脚本提示缺少 SDK，请在 Android Studio 的 SDK Manager 中确认 Android SDK、SDK Platform 和 Build Tools 已安装；如果提示 Java 错误，使用项目原先配置的 Android Studio/JDK 再运行一次脚本。
