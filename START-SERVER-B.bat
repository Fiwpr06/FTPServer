@echo off
setlocal
cd /d "%~dp0"
title SYNCVAULT - SERVER B (SECONDARY)
chcp 65001 >nul

if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_EXE=C:\Program Files\Java\jdk-21\bin\java.exe"
) else if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)

if not exist "target\classes" (
    echo [SYNCVAULT] Biên dịch dự án lần đầu...
    call .\mvnw.cmd compile
)

:: Tự động giải phóng cổng 2122 và 1100 nếu có phiên chạy ngầm cũ đang chiếm giữ
for /f "tokens=5" %%p in ('netstat -aon 2^>nul ^| findstr ":2122 "') do taskkill /f /pid %%p >nul 2>&1
for /f "tokens=5" %%p in ('netstat -aon 2^>nul ^| findstr ":1100 "') do taskkill /f /pid %%p >nul 2>&1

set "PEER_IP=%~1"
if "%PEER_IP%"=="" (
    echo ====================================================================
    echo   Nếu chạy Server B trên CÙNG 1 MÁY với Server A: Nhấn ENTER
    echo   Nếu chạy Server B trên MÁY KHÁC: Nhập IP của Máy A (VD: 192.168.1.10)
    echo ====================================================================
    set /p PEER_IP="Nhập địa chỉ IP Server A [Mặc định 127.0.0.1]: "
)
if "%PEER_IP%"=="" set "PEER_IP=127.0.0.1"

echo.
echo [SYNCVAULT] Đang khởi động Server B (Port 2122) kết nối tới Server A (%PEER_IP%:1099)...
"%JAVA_EXE%" "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" -cp "target\classes;target\lib\*" com.syncvault.server.ServerLauncher --secondary --peer=%PEER_IP%

pause
