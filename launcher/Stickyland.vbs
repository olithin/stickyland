Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
exePath = scriptDir & "\Stickyland.exe"

If Not fso.FileExists(exePath) Then
    MsgBox "Stickyland executable not found." & vbCrLf & exePath, vbCritical, "Stickyland"
    WScript.Quit 1
End If

shell.CurrentDirectory = scriptDir
shell.Run """" & exePath & """", 1, False
