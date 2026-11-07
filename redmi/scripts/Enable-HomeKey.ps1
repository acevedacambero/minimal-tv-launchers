param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$null=Save-FirstBackup
$pkg=Shell dumpsys package local.minimal.tv
if($pkg -notmatch 'HomeBootReceiver'){throw 'Install APK 1.4 or later before enabling boot recovery'}
$permission='android.permission.WRITE_SECURE_SETTINGS'
$wasGranted=$pkg -match 'android\.permission\.WRITE_SECURE_SETTINGS: granted=true'
$grantBackup="$script:State\home-boot-permission-backup.json"
if(!(Test-Path $grantBackup)){
 $snap=@{identity=$script:Identity;time=(Get-Date).ToString('o');permission=$permission;originallyGranted=$wasGranted}
 $stream=[IO.File]::Open($grantBackup,[IO.FileMode]::CreateNew)
 try{$bytes=[Text.Encoding]::UTF8.GetBytes(($snap|ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
}
$component='local.minimal.tv/local.minimal.tv.HomeKeyService'
$services=Shell settings get secure enabled_accessibility_services
$global=Shell settings get secure accessibility_enabled
$path="$script:State\home-key-backup.json"
if(!(Test-Path $path)){
 $snapshot=@{identity=$script:Identity;time=(Get-Date).ToString('o');services=$services;accessibilityEnabled=$global}
 $stream=[IO.File]::Open($path,[IO.FileMode]::CreateNew)
 try{$bytes=[Text.Encoding]::UTF8.GetBytes(($snapshot|ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
}
$entries=@($services -split ':' | Where-Object {$_ -and $_ -ne 'null'})
if($component -notin $entries -and 'local.minimal.tv/.HomeKeyService' -notin $entries){$entries+=,$component}
Write-Event 'home-key-enable-planned' @{before=$services;after=($entries -join ':');globalBefore=$global}
if(!$wasGranted){
 Write-Event 'home-boot-permission-planned' @{permission=$permission}
 $null=Shell pm grant --user 0 local.minimal.tv $permission
 if((Shell dumpsys package local.minimal.tv) -notmatch 'android\.permission\.WRITE_SECURE_SETTINGS: granted=true'){throw 'Boot recovery permission grant failed'}
 Write-Event 'home-boot-permission-success' @{permission=$permission}
}
$null=Shell am broadcast --user 0 -n local.minimal.tv/.HomeKeyConfigReceiver -a local.minimal.tv.CONFIG_HOME --ez enabled true
Write-Event 'home-boot-recovery-enabled' @{receiver='local.minimal.tv/.HomeBootReceiver'}
$null=Shell settings put secure enabled_accessibility_services ($entries -join ':')
$null=Shell settings put secure accessibility_enabled 1
Start-Sleep -Seconds 2
$dump=Shell dumpsys accessibility
$dump|Set-Content "$script:State\home-key-accessibility.txt" -Encoding UTF8
$bound=[regex]::Match($dump,'(?s)Bound services:(.*?)(?:Enabled services:|Binding services:)').Groups[1].Value
if($bound -notmatch 'Service\[label=极简桌面主页键'){throw 'HOME service did not bind; inspect accessibility state'}
Write-Event 'home-key-service-bound' @{component=$component}
# Test from settings so the launch cannot pass merely because the desktop was already open.
$null=Shell am start -W -a android.settings.SETTINGS
Start-Sleep -Seconds 1
Assert-Top 'com.android.tv.settings'
$null=Shell input keyevent 3
Start-Sleep -Seconds 2
Assert-Top 'local.minimal.tv'
$null=Shell sync
Write-Event 'home-key-mapping-success' @{factoryEnabled=(Get-PackageState 'com.mitv.tvhome');defaultHome=(Get-Home)}
Write-Host 'HOME key now opens the minimal desktop; factory launcher is retained.'
