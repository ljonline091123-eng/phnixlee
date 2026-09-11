@echo off
setlocal EnableExtensions

rem Start both backend and frontend for Gemini Quant Agent.
set "ROOT=%~dp0.."
for %%I in ("%ROOT%") do set "ROOT=%%~fI"
set "BACKEND=%ROOT%\backend"
set "FRONTEND=%ROOT%\frontend"

if not exist "%BACKEND%\app\main.py" (
    echo Backend directory was not found:
    echo %BACKEND%
    pause
    exit /b 1
)

if not exist "%FRONTEND%\package.json" (
    echo Frontend directory was not found:
    echo %FRONTEND%
    pause
    exit /b 1
)

where python >nul 2>&1
if errorlevel 1 (
    echo Python was not found in PATH.
    pause
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo npm was not found in PATH.
    pause
    exit /b 1
)

echo Initializing local database...
pushd "%BACKEND%"
python -m scripts.init_database
if errorlevel 1 (
    popd
    echo Database initialization failed.
    pause
    exit /b 1
)
popd

echo Starting backend on http://127.0.0.1:8000 ...
start "Gemini Quant Agent - Backend" powershell.exe -NoLogo -NoExit -ExecutionPolicy Bypass -Command "Set-Location -LiteralPath '%BACKEND%'; python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload"

echo Starting frontend on http://127.0.0.1:5173 ...
start "Gemini Quant Agent - Frontend" powershell.exe -NoLogo -NoExit -ExecutionPolicy Bypass -Command "Set-Location -LiteralPath '%FRONTEND%'; npm run dev -- --host 127.0.0.1 --port 5173"

echo.
echo Gemini Quant Agent is starting.
echo Frontend: http://127.0.0.1:5173
echo Backend docs: http://127.0.0.1:8000/docs
echo Health check: http://127.0.0.1:8000/health
echo.
timeout /t 3 /nobreak >nul
start "" "http://127.0.0.1:5173"

endlocal
