@echo off
setlocal
cd /d "%~dp0"

where py >nul 2>nul
if not errorlevel 1 (
  start "" /b py -m http.server 8080
  timeout /t 1 /nobreak >nul
  start "" "http://localhost:8080"
  echo H5 已启动：http://localhost:8080
  echo 关闭此窗口不会自动停止服务，如需停止请结束 python/py 进程。
  pause
  exit /b
)

where python >nul 2>nul
if not errorlevel 1 (
  start "" /b python -m http.server 8080
  timeout /t 1 /nobreak >nul
  start "" "http://localhost:8080"
  echo H5 已启动：http://localhost:8080
  echo 关闭此窗口不会自动停止服务，如需停止请结束 python 进程。
  pause
  exit /b
)

start "" "index.html"
echo 未找到 Python，已直接打开 index.html。
pause
