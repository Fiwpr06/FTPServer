Option Explicit

Dim WshShell, fso, scriptDir

Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Lấy đường dẫn thư mục hiện tại
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = scriptDir

' Chạy Server B ẩn hoàn toàn cửa sổ console đen
WshShell.Run "cmd.exe /c START-SERVER-B.bat", 0, False
