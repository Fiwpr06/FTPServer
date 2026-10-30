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

cmd = javaExe & " --module-path target\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp ""target\classes;target\lib\*"" com.syncvault.client.Launcher"

WshShell.Run cmd, 1, False
