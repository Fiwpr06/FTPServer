@echo off
setlocal
title SYNCVAULT - DỪNG TẤT CẢ SERVER & CLIENT
chcp 65001 >nul

echo ====================================================================
echo   SYNCVAULT // ĐANG GIẢI PHÓNG TOÀN BỘ CỔNG MẠNG VÀ DỪNG TIẾN TRÌNH
echo ====================================================================

for /f "tokens=5" %%p in ('netstat -aon 2^>nul ^| findstr ":2121 "') do (
    echo [DUNG] Server A (FTP: 2121) - PID %%p
    taskkill /f /pid %%p >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -aon 2^>nul ^| findstr ":1099 "') do (
    echo [DUNG] Server A (RMI: 1099) - PID %%p
    taskkill /f /pid %%p >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -aon 2^>nul ^| findstr ":2122 "') do (
    echo [DUNG] Server B (FTP: 2122) - PID %%p
    taskkill /f /pid %%p >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -aon 2^>nul ^| findstr ":1100 "') do (
    echo [DUNG] Server B (RMI: 1100) - PID %%p
    taskkill /f /pid %%p >nul 2>&1
)

echo.
echo [SYNCVAULT] Đã giải phóng toàn bộ dịch vụ và cổng mạng thành công!
timeout /t 2 >nul
