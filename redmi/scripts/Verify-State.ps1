param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL,[switch]$ExpectRestored)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$baseline=Read-Json "$script:State\first-backup.json"
$dump=Shell dumpsys package
$current=@{}
foreach($part in [regex]::Split($dump,'(?m)^  Package \[')|Select-Object -Skip 1){
 $n=[regex]::Match($part,'^([^\]]+)\]')
 $m=[regex]::Match($part,'(?m)^\s*User 0:.*\benabled=(\d+)\b')
 if($n.Success -and $m.Success -and !$current.ContainsKey($n.Groups[1].Value)){$current[$n.Groups[1].Value]=[int]$m.Groups[1].Value}
}
$changes=@()
foreach($p in $baseline.states.PSObject.Properties){
 if(!$current.ContainsKey($p.Name)){throw "Missing baseline package: $($p.Name)"}
 if($current[$p.Name] -ne [int]$p.Value){$changes+=@{package=$p.Name;before=[int]$p.Value;after=$current[$p.Name]}}
}
$expectedCount=1
if($ExpectRestored){$expectedCount=0}
if($changes.Count -ne $expectedCount){throw "Unexpected package state change count: $($changes|ConvertTo-Json -Compress)"}
if(!$ExpectRestored -and ($changes[0].package -ne 'com.mitv.shoplugin' -or $changes[0].before -ne 0 -or $changes[0].after -ne 3)){throw 'Unexpected package change'}
foreach($p in $baseline.disabled){if($current[$p] -ne [int]$baseline.states.PSObject.Properties[$p].Value){throw "Historical disabled state changed: $p"}}
$result=@{passed=$true;time=(Get-Date).ToString('o');expectRestored=[bool]$ExpectRestored;baselinePackageCount=@($baseline.states.PSObject.Properties).Count;historicalDisabledCount=@($baseline.disabled).Count;changes=$changes;defaultHome=(Get-Home);role=(Shell cmd role get-role-holders --user 0 android.app.role.HOME);backupSha256=(Get-FileHash "$script:State\first-backup.json").Hash}
Write-Json "$script:State\state-verification-$(Get-Date -Format yyyyMMdd-HHmmss).json" $result
Write-Event 'state-verification-success' $result
$result|ConvertTo-Json -Depth 8|Write-Host
