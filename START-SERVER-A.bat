@echo off
setlocal
cd /d "%~dp0"
title SYNCVAULT - SERVER A (PRIMARY)
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

:: Tự động giải phóng cổng 2121 và 1099 nếu có phiên chạy ngầm cũ đang chiếm giữ
for /f "tokens=5" %%p in ('netstat -aon 2^>nul ^| findstr ":2121 "') do taskkill /f /pid %%p >nul 2>&1
for /f "tokens=5" %%p in ('netstat -aon 2^>nul ^| findstr ":1099 "') do taskkill /f /pid %%p >nul 2>&1

echo [SYNCVAULT] Đang khởi động Server A (Primary - Port 2121, RMI 1099)...
"%JAVA_EXE%" "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" -cp "target\classes;target\lib\*" com.syncvault.server.ServerLauncher --primary

pause
