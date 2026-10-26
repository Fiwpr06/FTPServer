@echo off
title SYNCVAULT - SERVER A (PRIMARY)
chcp 65001 >nul
echo [SYNCVAULT] Đang khởi động Server A (Primary - Port 2121, RMI 1099)...
set JAVA_HOME=C:\Program Files\Java\jdk-21
call .\mvnw.cmd compile exec:java -Dexec.mainClass="com.syncvault.server.ServerLauncher" -Dexec.args="--primary"
pause
