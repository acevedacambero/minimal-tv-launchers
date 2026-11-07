param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL,[switch]$UseOriginalDefault)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$backup=Read-Json "$script:State\first-backup.json"
if(Test-Path "$script:State\home-key-backup.json"){
 & "$PSScriptRoot\Disable-HomeKey.ps1" -Adb $Adb -Device $Device
 if(!$?){throw 'Cannot remove HOME key mapping; recovery stopped'}
}
# Recover only packages touched by this project, including an interrupted operation.
$events=@(Get-Events)
$names=@($events | Where-Object {$_.kind -eq 'disable-planned'} | ForEach-Object {$_.data.package} | Sort-Object -Unique)
foreach($name in $names){
 $prop=$backup.states.PSObject.Properties[$name]
 if(!$prop){throw "No original state: $name"}
 Restore-EnabledState $name ([int]$prop.Value)
}
if((Get-PackageState 'com.mitv.tvhome') -in 2,3,4){throw 'Factory launcher disabled outside this project; no silent change to historical state'}
$target=$script:Factory
if($UseOriginalDefault){$target=$backup.home}
Set-Home $target
$null=Shell input keyevent 3
Start-Sleep -Seconds 1
Assert-Top ($target.Split('/')[0])
Write-Event 'restore-success' @{home=$target;packages=$names}
Write-Host 'Restored. Historical disabled apps unchanged. APK retained for optional future use.'
