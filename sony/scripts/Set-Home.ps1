param([string]$Adb = 'adb', [string]$Device = $env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$backup = Read-Backup
$null = Shell am start -W -n $script:Launcher
Assert-Top 'local.minimal.sony'
try {
    Set-VerifiedHome $script:Launcher
    $null = Shell am start -W -a android.settings.SETTINGS
    Assert-Top 'com.android.tv.settings'
    $null = Shell input keyevent 3
    Start-Sleep -Seconds 2
    Assert-Top 'local.minimal.sony'
    $null = Shell sync
    Write-Event 'home-key-success' @{ component = $script:Launcher }
} catch {
    Set-VerifiedHome $backup.home
    throw
}
Write-Host 'Sony default HOME and HOME key verified.'
