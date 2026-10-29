Option Explicit

Dim WshShell, fso, scriptDir, javaHome, javaExe, cmd, peerIp

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

peerIp = InputBox("Nhập địa chỉ IP của Server A (Primary):" & vbCrLf & "(Nếu chạy trên cùng 1 máy thì giữ nguyên 127.0.0.1)", "SyncVault - Khởi động Server B", "127.0.0.1")
If peerIp = "" Then peerIp = "127.0.0.1"

' Tu dong giai phong cong 2122 va 1100 neu co tien trinh cu
WshShell.Run "cmd /c for /f ""tokens=5"" %p in ('netstat -aon ^| findstr "":2122 ""') do taskkill /f /pid %p", 0, True
WshShell.Run "cmd /c for /f ""tokens=5"" %p in ('netstat -aon ^| findstr "":1100 ""') do taskkill /f /pid %p", 0, True

cmd = javaExe & " ""-Dfile.encoding=UTF-8"" -cp ""target\classes;target\lib\*"" com.syncvault.server.ServerLauncher --secondary --peer=" & peerIp

WshShell.Run cmd, 0, False
WshShell.Popup "Server B (Secondary) đã khởi chạy ngầm thành công!" & vbCrLf & "- Cổng FTP: 2122" & vbCrLf & "- Cổng RMI: 1100" & vbCrLf & "- Server đối tác A: " & peerIp & ":1099" & vbCrLf & vbCrLf & "(Để dừng lại bất kỳ lúc nào: Nhấp đúp STOP-ALL.bat)", 3, "SyncVault - Server B Ready", 64
