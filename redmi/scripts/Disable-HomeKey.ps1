param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$path="$script:State\home-key-backup.json"
if(!(Test-Path $path)){return}
$before=Read-Json $path
if($before.identity.serial -ne $script:Identity.serial){throw 'HOME key backup belongs to another device'}
if((Shell dumpsys package local.minimal.tv) -match 'HomeBootReceiver'){
 $null=Shell am broadcast --user 0 -n local.minimal.tv/.HomeKeyConfigReceiver -a local.minimal.tv.CONFIG_HOME --ez enabled false
 Write-Event 'home-boot-recovery-disabled' @{}
}
$services=Shell settings get secure enabled_accessibility_services
$remaining=@($services -split ':' | Where-Object {$_ -and $_ -ne 'null' -and $_ -ne 'local.minimal.tv/local.minimal.tv.HomeKeyService' -and $_ -ne 'local.minimal.tv/.HomeKeyService'})
Write-Event 'home-key-disable-planned' @{before=$services;after=($remaining -join ':')}
if($remaining.Count){$null=Shell settings put secure enabled_accessibility_services ($remaining -join ':')}
elseif($before.services -eq 'null'){$null=Shell settings delete secure enabled_accessibility_services}
else{$null=Shell settings put secure enabled_accessibility_services '""'}
if(!$remaining.Count){
 if($before.accessibilityEnabled -eq 'null'){$null=Shell settings delete secure accessibility_enabled}
 else{$null=Shell settings put secure accessibility_enabled $before.accessibilityEnabled}
}
Start-Sleep -Seconds 1
if((Shell settings get secure enabled_accessibility_services) -match 'local\.minimal\.tv/'){throw 'HOME key mapping remains enabled'}
$grantBackup="$script:State\home-boot-permission-backup.json"
if(Test-Path $grantBackup){
 $grant=Read-Json $grantBackup
 if($grant.identity.serial -ne $script:Identity.serial){throw 'Permission backup identity mismatch'}
 if(!$grant.originallyGranted -and (Shell dumpsys package local.minimal.tv) -match 'android\.permission\.WRITE_SECURE_SETTINGS: granted=true'){
   $null=Shell pm revoke --user 0 local.minimal.tv android.permission.WRITE_SECURE_SETTINGS
   Write-Event 'home-boot-permission-restored' @{granted=$false}
 }
}
Write-Event 'home-key-disable-success' @{services=(Shell settings get secure enabled_accessibility_services)}
Write-Host 'HOME key mapping removed; other accessibility services preserved.'
