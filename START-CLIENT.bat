@echo off
title SYNCVAULT - CLIENT YORHA OS
chcp 65001 >nul
echo [SYNCVAULT] Đang khởi chạy ứng dụng Client JavaFX...
set JAVA_HOME=C:\Program Files\Java\jdk-21
call .\mvnw.cmd javafx:run
pause
