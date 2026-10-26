@echo off
title SYNCVAULT - SERVER B (SECONDARY)
chcp 65001 >nul
echo [SYNCVAULT] Đang khởi động Server B (Secondary - Port 2122, RMI 1100)...
set JAVA_HOME=C:\Program Files\Java\jdk-21
call .\mvnw.cmd compile exec:java -Dexec.mainClass="com.syncvault.server.ServerLauncher" -Dexec.args="--secondary"
pause
