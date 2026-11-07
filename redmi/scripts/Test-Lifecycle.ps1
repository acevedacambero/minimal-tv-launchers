param([ValidateSet('Home','Standby','Reboot')][string]$Mode='Home',[string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL,[switch]$UseFactoryFallback,[switch]$ResumeAfterPhysicalWake,[switch]$UseHomeKeyMapping)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$null=Save-FirstBackup
$expected=$script:Launcher
if($UseFactoryFallback -or $UseHomeKeyMapping){$expected=$script:Factory}
if((Get-Home) -ne $expected){throw 'Unexpected default HOME before lifecycle test'}
$folder="$script:State\lifecycle-$Mode-$(Get-Date -Format yyyyMMdd-HHmmss)"
New-Item -ItemType Directory $folder | Out-Null
$before=Get-Top
$before|Set-Content "$folder\before.txt" -Encoding UTF8
if($Mode -eq 'Standby'){
 if(!$ResumeAfterPhysicalWake){
 Write-Event 'standby-planned' @{}
 $null=Shell input keyevent 223
 Start-Sleep -Seconds 3
 try{$power=Shell dumpsys power}catch{
   $failure=@{passed=$false;mode='Standby';time=(Get-Date).ToString('o');reason='ADB disconnected after SLEEP; physical remote wake required';error=$_.Exception.Message}
   Write-Json "$folder\result.json" $failure
   Write-Event 'standby-needs-physical-wake' $failure
   throw 'ADB disconnected during standby. Wake using physical remote, then rerun with -Mode Standby -ResumeAfterPhysicalWake (and the same fallback option).'
 }
 $power|Set-Content "$folder\asleep-power.txt" -Encoding UTF8
 if($power -notmatch 'mWakefulness=(Asleep|Dozing)'){
   $null=Shell input keyevent 224
   throw 'Device did not enter software standby; physical standby remains unverified'
 }
 $null=Shell input keyevent 224
 Start-Sleep -Seconds 4
 }
 $awake=Shell dumpsys power
 $awake|Set-Content "$folder\awake-power.txt" -Encoding UTF8
 if($awake -notmatch 'mWakefulness=Awake'){throw 'Wake verification failed'}
}
if($Mode -eq 'Reboot'){
 Write-Event 'reboot-planned' @{}
 $bootBefore=Shell cat /proc/sys/kernel/random/boot_id
 Write-Json "$folder\boot-before.json" @{id=$bootBefore;time=(Get-Date).ToString('o')}
 $null=Invoke-Adb reboot
 Write-Event 'reboot-command-success' @{bootBefore=$bootBefore}
 Start-Sleep -Seconds 6
 $ready=$false
 for($i=0;$i -lt 60;$i++){
   try{
     & $Adb connect $Device 2>&1 | Out-Null
     if((Invoke-Adb get-state) -eq 'device' -and (Shell getprop sys.boot_completed) -eq '1'){
       $bootAfter=Shell cat /proc/sys/kernel/random/boot_id
       if($bootAfter -ne $bootBefore){$ready=$true;break}
     }
   }catch{}
   if($i % 5 -eq 0){Write-Host "Waiting for reboot: $($i*3+6)s"}
   Start-Sleep -Seconds 3
 }
 if(!$ready){throw 'Reboot verification timed out; do not continue dependent changes'}
 Start-Sleep -Seconds 10
 Assert-Device
 Write-Json "$folder\boot-id.json" @{before=$bootBefore;after=$bootAfter}
}
$natural=Get-Top
$natural|Set-Content "$folder\natural-foreground.txt" -Encoding UTF8
if((Get-Home) -ne $expected){throw 'Default HOME lost after lifecycle transition'}
$null=Shell input keyevent 3
Start-Sleep -Seconds 2
if($UseHomeKeyMapping){Assert-Top 'local.minimal.tv'}else{Assert-Top ($expected.Split('/')[0])}
$null=Shell screencap -p /sdcard/minimal-lifecycle.png
$null=Invoke-Adb pull /sdcard/minimal-lifecycle.png "$folder\home.png"
$result=@{passed=$true;mode=$Mode;homeKeyMapping=[bool]$UseHomeKeyMapping;physicalWakeResume=[bool]$ResumeAfterPhysicalWake;factoryFallback=[bool]$UseFactoryFallback;time=(Get-Date).ToString('o');home=(Get-Home);naturalForeground=@($natural -split '\r?\n'|Where-Object {$_ -match 'topResumedActivity|ResumedActivity:'});identity=$script:Identity}
Write-Json "$folder\result.json" $result
Write-Event 'lifecycle-success' $result
$result|ConvertTo-Json -Depth 8|Write-Host
