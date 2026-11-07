param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL,[switch]$UseFactoryFallback)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$backup=Save-FirstBackup
$v=Read-Json "$script:State\navigation-verified.json"
if(!$v.passed -or $v.identity.serial -ne $script:Identity.serial -or $v.sha256 -ne (Get-FileHash "$script:Root\dist\minimal-tv.apk").Hash){throw 'Successful matching launcher navigation test required'}
$expected=$script:Launcher
if($UseFactoryFallback){$expected=$script:Factory}
if((Get-Home) -ne $expected){throw 'Unexpected HOME; stop for state review'}
# Hard-coded upper bound: editing JSON alone cannot expand executable package scope.
$approved=@('com.mitv.shoplugin','com.miui.tv.analytics','com.duokan.videodaily','com.xiaomi.mitv.tvpush.tvpushservice','com.xm.webcontent','com.xiaomi.gamecenter.sdk.service.mibox')
$protected=@(Read-Json "$PSScriptRoot\protected-packages.json")
$items=@(Read-Json "$PSScriptRoot\disable-whitelist.json")
$items|Format-Table package,reason -Wrap|Out-Host
foreach($item in $items){
 $p=[string]$item.package
 if($p -notin $approved -or $p -in $protected -or $p -match '^(android|com\.android\.|com\.mediatek\.|mediatek\.)'){throw "Protected or unapproved exact package: $p"}
 $prop=$backup.states.PSObject.Properties[$p]
 if(!$prop){throw "Package absent from baseline: $p"}
 $original=[int]$prop.Value
 $current=Get-PackageState $p
 if($original -in 2,3,4){Write-Host "Historical disabled item preserved: $p ($original)";continue}
 $touched=@(Get-Events|Where-Object {$_.kind -eq 'disable-planned' -and $_.data.package -eq $p}).Count -gt 0
 if($current -eq 3 -and $touched){Write-Host "Already applied: $p";continue}
 if($current -ne $original){throw "State changed outside this project: $p. Stop; original backup retained."}
 Write-Event 'disable-planned' @{package=$p;before=$original;target=3;reason=$item.reason}
 $null=Shell pm disable-user --user 0 $p
 if((Get-PackageState $p) -ne 3){throw "Disable verification failed: $p"}
 Write-Event 'disable-success' @{package=$p;before=$original;after=3}
}
Write-Host 'Exact whitelist processed. Shared source/media services preserved.'
