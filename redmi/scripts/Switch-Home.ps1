param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$null=Save-FirstBackup
$v=Read-Json "$script:State\navigation-verified.json"
if(!$v.passed -or $v.identity.serial -ne $script:Identity.serial -or $v.identity.fingerprint -ne $script:Identity.fingerprint -or $v.sha256 -ne (Get-FileHash "$script:Root\dist\minimal-tv.apk").Hash){throw 'Matching successful navigation validation required'}
try {
 Set-Home $script:Launcher
 $null=Shell input keyevent 3
 Start-Sleep -Seconds 2
 Assert-Top 'local.minimal.tv'
 Write-Event 'home-key-success' @{component=$script:Launcher}
} catch {
 $failure=$_
 try { Set-Home $script:Factory; $null=Shell input keyevent 3 } catch {Write-Warning "Factory fallback failed: $_"}
 throw $failure
}
Write-Host 'Default HOME and HOME-key verified. Factory launcher remains enabled.'
