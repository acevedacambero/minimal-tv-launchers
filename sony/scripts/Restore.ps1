param([string]$Adb = 'adb', [string]$Device = $env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$backup = Read-Backup
if ((Get-Home) -ne $backup.home) { Set-VerifiedHome $backup.home }
$null = Shell input keyevent 3
Start-Sleep -Seconds 2
Assert-Top ($backup.home.Split('/')[0])
$null = Shell sync
Write-Event 'original-home-restored' @{ component = $backup.home }
Write-Host "Restored original HOME: $($backup.home)"
