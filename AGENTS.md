# FiveLines working rules

- Before the first source edit on each calendar day, run `powershell -ExecutionPolicy Bypass -File scripts/pre-change-backup.ps1` and confirm that it pushed the dated backup branch to `origin`.
- The Android implementation in `app/src/main` is the product baseline. Keep the iOS implementation behavior, visuals, defaults, and audio aligned with it unless the user explicitly requests a platform-specific difference.
- The active iOS Xcode target uses `iOSFiveLines/Sources`. Do not reintroduce duplicate Swift entry files at the root of `iOSFiveLines`.
- Do not modify `h5` unless the user explicitly asks for an H5 change.
