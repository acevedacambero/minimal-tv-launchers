Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$script:Root=Split-Path $PSScriptRoot -Parent
$script:State=Join-Path $script:Root 'state'
$script:Factory='com.mitv.tvhome/.MainActivityUserMode'
$script:Launcher='local.minimal.tv/.MainActivity'
if(!$Adb){$Adb='adb'}
if(!$Device){throw 'Pass -Device with the TV ADB address (IP:port), or set TV_ADB_SERIAL.'}
New-Item -ItemType Directory -Force $script:State | Out-Null
function Invoke-Adb {
 $Arguments=@($args)
 $output=(& $Adb -s $Device @Arguments 2>&1 | Out-String).Trim()
 if($LASTEXITCODE -ne 0 -or $output -match '(?im)^Error:|^java\.lang\.SecurityException:|^Exception occurred while executing|^Failure\s*\['){throw "ADB failed: $($Arguments -join ' ')`n$output"}
 return $output
}
function Shell { Invoke-Adb shell @args }
function Write-Json($Path,$Value) {
 $temp="$Path.new"
 $Value | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath $temp -Encoding UTF8
 Move-Item -LiteralPath $temp -Destination $Path -Force
}
function Read-Json($Path){ Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | ConvertFrom-Json }
function Write-Event($Kind,$Data) {
 $item=[ordered]@{time=(Get-Date).ToString('o');kind=$Kind;data=$Data}
 $item | ConvertTo-Json -Compress -Depth 12 | Add-Content -LiteralPath "$script:State\operations.jsonl" -Encoding UTF8
}
function Assert-Device {
 if(!(Get-Command $Adb -CommandType Application -ErrorAction SilentlyContinue)){throw "ADB executable not found: $Adb"}
 $connected=(& $Adb connect $Device 2>&1 | Out-String)
 if($LASTEXITCODE -ne 0){throw $connected}
 if((Invoke-Adb get-state) -ne 'device'){throw 'Device is not authorized/online'}
 $model=Shell getprop ro.product.model
 $build=Shell getprop ro.build.display.id
 $api=Shell getprop ro.build.version.sdk
 if($model -ne 'MiTV-MFFU1' -or $build -ne 'OS3.0.109.0.UFFMAMN' -or $api -ne '34'){throw "Device mismatch: $model / $build / API $api. Re-audit required."}
 $script:Identity=[ordered]@{model=$model;build=$build;api=$api;serial=(Shell getprop ro.serialno);fingerprint=(Shell getprop ro.build.fingerprint)}
 if(Test-Path "$script:State\first-backup.json"){
   if(Test-Path "$script:State\first-backup.sha256"){
     $expected=(Get-Content "$script:State\first-backup.sha256" -Raw).Trim()
     if((Get-FileHash "$script:State\first-backup.json").Hash -ne $expected){throw 'First backup integrity check failed'}
   }
   $b=Read-Json "$script:State\first-backup.json"
   if($b.identity.serial -ne $script:Identity.serial -or $b.identity.fingerprint -ne $script:Identity.fingerprint){throw 'Backup belongs to another device or firmware'}
 }
}
function Get-Home {
 $h=Shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
 $lines=@($h -split '\r?\n' | Where-Object {$_ -match '^[a-zA-Z0-9_.]+/[a-zA-Z0-9_.$]+$'})
 if($lines.Count -ne 1){throw "Cannot resolve unique HOME: $h"}
 $lines[0]
}
function Get-PackageState([string]$Package){
 if($Package -notmatch '^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$'){throw 'Invalid exact package name'}
 $dump=Shell dumpsys package $Package
 $m=[regex]::Match($dump,'(?m)^\s*User 0:.*\benabled=(\d+)\b')
 if(!$m.Success){throw "Missing user-0 state: $Package"}
 [int]$m.Groups[1].Value
}
function Save-FirstBackup {
 $path="$script:State\first-backup.json"
 if(Test-Path $path){return (Read-Json $path)}
 $dump=Shell dumpsys package
 $dump | Set-Content "$script:State\first-packages-dump.txt" -Encoding UTF8
 $all=Shell pm list packages --user 0
 $states=[ordered]@{}
 # Take the active Packages section, excluding hidden factory versions at the end.
 foreach($part in [regex]::Split($dump,'(?m)^  Package \[') | Select-Object -Skip 1){
   $n=[regex]::Match($part,'^([^\]]+)\]')
   $m=[regex]::Match($part,'(?m)^\s*User 0:.*\benabled=(\d+)\b')
   if($n.Success -and $m.Success -and !$states.Contains($n.Groups[1].Value)){$states[$n.Groups[1].Value]=[int]$m.Groups[1].Value}
 }
 $names=@($all -split '\r?\n' | Where-Object {$_ -match '^package:'} | ForEach-Object {$_.Substring(8)})
 foreach($n in $names){if(!$states.Contains($n)){throw "Incomplete backup: $n"}}
 $disabled=Shell pm list packages -d --user 0
 $props=Shell getprop
 $props | Set-Content "$script:State\first-getprop.txt" -Encoding UTF8
 $originalHome=Get-Home
 $backup=[ordered]@{schema=1;time=(Get-Date).ToString('o');identity=$script:Identity;home=$originalHome;states=$states;disabled=@($disabled -split '\r?\n' | Where-Object {$_ -match '^package:'} | ForEach-Object {$_.Substring(8)})}
 # CreateNew prevents overwriting the first complete snapshot, including repeat runs.
 $stream=[IO.File]::Open($path,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
 try{$bytes=[Text.Encoding]::UTF8.GetBytes(($backup|ConvertTo-Json -Depth 15));$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
 (Get-FileHash $path).Hash|Set-Content "$script:State\first-backup.sha256" -Encoding ASCII
 Write-Event 'backup-created' @{path=$path}
 Read-Json $path
}
function Get-Events {
 if(Test-Path "$script:State\operations.jsonl") {Get-Content "$script:State\operations.jsonl" | Where-Object {$_} | ForEach-Object {$_|ConvertFrom-Json}}
}
function Restore-EnabledState([string]$Package,[int]$Original) {
 $current=Get-PackageState $Package
 if($current -eq $Original){return}
 switch($Original){
   0 {$null=Shell pm default-state --user 0 $Package}
   1 {$null=Shell pm enable --user 0 $Package}
   3 {$null=Shell pm disable-user --user 0 $Package}
   default {throw "Refusing unsupported original disabled state $Original for $Package; no approximation made"}
 }
 if((Get-PackageState $Package) -ne $Original){throw "Restore verification failed: $Package"}
 Write-Event 'package-restored' @{package=$Package;state=$Original}
}
function Set-Home([string]$Component){
 Write-Event 'home-planned' @{target=$Component;before=(Get-Home)}
 $result=Shell cmd package set-home-activity --user 0 $Component
 Write-Event 'home-command-success' @{target=$Component;output=$result}
 Start-Sleep -Seconds 2
 $effective=Get-Home
 $role=Shell cmd role get-role-holders --user 0 android.app.role.HOME
 Write-Json "$script:State\last-home-attempt.json" @{target=$Component;effective=$effective;role=$role;output=$result;time=(Get-Date).ToString('o')}
 if($effective -ne $Component){
   Copy-Item "$script:State\last-home-attempt.json" "$script:State\home-restriction.json" -Force
   Write-Event 'home-effective-failed' @{target=$Component;effective=$effective;role=$role;output=$result}
   throw 'System did not retain requested default HOME'
 }
 Write-Event 'home-success' @{target=$Component}
}
function Get-Top { Shell dumpsys activity activities }
function Assert-Top([string]$Package){
 $top=Get-Top
 if($top -notmatch "(?m)(topResumedActivity|ResumedActivity):?=.*$([regex]::Escape($Package))/" -and $top -notmatch "(?m)ResumedActivity:.*$([regex]::Escape($Package))/"){throw "Foreground check failed: $Package"}
}
