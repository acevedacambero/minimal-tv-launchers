param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$backup=Save-FirstBackup
$folder="$script:State\audit-$(Get-Date -Format yyyyMMdd-HHmmss)"
New-Item -ItemType Directory $folder | Out-Null
$whitelist=@(Read-Json "$PSScriptRoot\disable-whitelist.json")
$protected=@(Read-Json "$PSScriptRoot\protected-packages.json")
$packages=Shell pm list packages --user 0
$packages|Set-Content "$folder\packages.txt" -Encoding UTF8
(Shell dumpsys tv_input)|Set-Content "$folder\tv-input.txt" -Encoding UTF8
(Shell dumpsys activity services)|Set-Content "$folder\services.txt" -Encoding UTF8
$rows=@(foreach($item in $whitelist){
 $p=$item.package
 $dump=Shell dumpsys package $p
 $dump|Set-Content "$folder\$p.txt" -Encoding UTF8
 [ordered]@{package=$p;current=(Get-PackageState $p);baseline=$backup.states.PSObject.Properties[$p].Value;protected=($p -in $protected);reason=$item.reason}
})
Write-Json "$folder\candidates.json" $rows
$rows|ForEach-Object {[pscustomobject]$_}|Format-Table -AutoSize|Out-Host
Write-Host "Audit saved: $folder. Audit makes no package changes."
