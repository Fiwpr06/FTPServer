@echo off
setlocal
cd /d "%~dp0"
title SYNCVAULT - CLIENT
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

echo [SYNCVAULT] Đang khởi động Client JavaFX...
"%JAVA_EXE%" "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" --module-path target\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target\classes;target\lib\*" com.syncvault.client.Launcher
