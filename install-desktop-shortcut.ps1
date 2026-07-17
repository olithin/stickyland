# Builds Stickyland, copies it to AppData, and creates a desktop shortcut.
$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Write-Host "Building Stickyland..." -ForegroundColor Cyan
& "$ProjectRoot\gradlew.bat" clean createDistributable --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed."
    exit 1
}

$BuildAppDir = Get-ChildItem -Path "$ProjectRoot\build\compose\binaries\main\app" -Directory | Select-Object -First 1
if (-not $BuildAppDir) {
    Write-Error "App folder not found after build."
    exit 1
}

$InstallDir = Join-Path $env:LOCALAPPDATA "Stickyland\App"
Write-Host "Installing to $InstallDir ..." -ForegroundColor Cyan

Get-Process -Name "Stickyland", "MyNotesNotion" -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 1

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null

$SourceDir = $BuildAppDir.FullName
foreach ($sub in @("app", "runtime")) {
    $src = Join-Path $SourceDir $sub
    $dst = Join-Path $InstallDir $sub
    if (Test-Path $src) {
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        & robocopy $src $dst /MIR /R:2 /W:1 /NFL /NDL /NJH /NJS | Out-Null
    }
}

$ExeName = "Stickyland.exe"
Copy-Item (Join-Path $SourceDir $ExeName) (Join-Path $InstallDir $ExeName) -Force -ErrorAction SilentlyContinue

$ExePath = Join-Path $InstallDir $ExeName
if (-not (Test-Path $ExePath)) {
    $ExePath = Get-ChildItem -Path $InstallDir -Filter "*.exe" -Recurse | Select-Object -First 1 -ExpandProperty FullName
}

if (-not $ExePath -or -not (Test-Path $ExePath)) {
    Write-Error "Executable not found in $InstallDir"
    exit 1
}

$ExeDir = Split-Path -Parent $ExePath
$IconPath = Join-Path $ProjectRoot "src\main\resources\icon.ico"

$Desktop = [Environment]::GetFolderPath("Desktop")
$ShortcutPath = Join-Path $Desktop "Stickyland.lnk"

# Remove old shortcut if present
$OldShortcut = Join-Path $Desktop "My Notes.lnk"
if (Test-Path $OldShortcut) { Remove-Item $OldShortcut -Force }

$WshShell = New-Object -ComObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut($ShortcutPath)
$Shortcut.TargetPath = $ExePath
$Shortcut.WorkingDirectory = $ExeDir
$Shortcut.IconLocation = "$IconPath,0"
$Shortcut.Description = "Stickyland — local notes"
$Shortcut.Save()

Write-Host ""
Write-Host "Done!" -ForegroundColor Green
Write-Host "Desktop shortcut: $ShortcutPath"
Write-Host "Installed app:    $ExePath"
Write-Host ""
Write-Host "Double-click 'Stickyland' on your desktop to launch."
