param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$null=Save-FirstBackup
$null=Shell am start -W -n $script:Launcher
Start-Sleep -Seconds 1
Assert-Top 'local.minimal.tv'
Write-Event 'fallback-open-success' @{component=$script:Launcher}
Write-Host 'Minimal desktop opened. Factory default HOME preserved.'
