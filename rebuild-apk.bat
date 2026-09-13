@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo [FiveLines] checking Gradle wrapper...
if not exist "gradlew.bat" (
  echo ERROR: gradlew.bat was not found in %CD%
  pause
  exit /b 1
)

echo [FiveLines] cleaning previous Android build...
call gradlew.bat clean
if errorlevel 1 (
  echo ERROR: Gradle clean failed.
  pause
  exit /b 1
)

echo [FiveLines] assembling debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo ERROR: assembleDebug failed.
  pause
  exit /b 1
)

set "APK_SOURCE=%CD%\app\build\outputs\apk\debug\app-debug.apk"
set "APK_TARGET=%CD%\FiveLines-debug.apk"
if not exist "%APK_SOURCE%" (
  echo ERROR: APK was not generated at:
  echo %APK_SOURCE%
  pause
  exit /b 1
)

copy /Y "%APK_SOURCE%" "%APK_TARGET%" >nul
echo.
echo APK generated successfully:
echo %APK_TARGET%
echo.
pause
