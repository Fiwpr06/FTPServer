Option Explicit

Dim WshShell, fso, scriptDir, javaHome, javaExe, cmd

Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = scriptDir

javaHome = WshShell.Environment("PROCESS")("JAVA_HOME")
If javaHome = "" Then
    javaHome = WshShell.Environment("SYSTEM")("JAVA_HOME")
End If
If javaHome = "" Or Not fso.FileExists(javaHome & "\bin\javaw.exe") Then
    If fso.FileExists("C:\Program Files\Java\jdk-21\bin\javaw.exe") Then
        javaHome = "C:\Program Files\Java\jdk-21"
    End If
End If

If javaHome <> "" And fso.FileExists(javaHome & "\bin\javaw.exe") Then
    javaExe = """" & javaHome & "\bin\javaw.exe"""
Else
    javaExe = "javaw.exe"
End If

' Tu dong giai phong cong 2121 va 1099 neu co tien trinh cu
WshShell.Run "cmd /c for /f ""tokens=5"" %p in ('netstat -aon ^| findstr "":2121 ""') do taskkill /f /pid %p", 0, True
WshShell.Run "cmd /c for /f ""tokens=5"" %p in ('netstat -aon ^| findstr "":1099 ""') do taskkill /f /pid %p", 0, True

cmd = javaExe & " ""-Dfile.encoding=UTF-8"" -cp ""target\classes;target\lib\*"" com.syncvault.server.ServerLauncher --primary"

WshShell.Run cmd, 0, False
WshShell.Popup "Server A (Primary) đã khởi chạy ngầm thành công!" & vbCrLf & "- Cổng FTP: 2121" & vbCrLf & "- Cổng RMI: 1099" & vbCrLf & vbCrLf & "(Để dừng lại bất kỳ lúc nào: Nhấp đúp STOP-ALL.bat)", 3, "SyncVault - Server A Ready", 64
