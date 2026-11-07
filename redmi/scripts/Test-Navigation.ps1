param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$null=Save-FirstBackup
$validation="$script:State\navigation-verified.json"
# A failed rerun must invalidate an earlier pass.
Write-Json $validation @{passed=$false;time=(Get-Date).ToString('o')}
function Key([string]$Code){$null=Shell input keyevent $Code;Start-Sleep -Milliseconds 700}
function Snapshot([string]$Name){
 $null=Shell uiautomator dump /sdcard/minimal-window.xml
 $xml=Shell cat /sdcard/minimal-window.xml
 $xml|Set-Content "$script:State\$Name.xml" -Encoding UTF8
 $null=Shell screencap -p /sdcard/minimal-screen.png
 $null=Invoke-Adb pull /sdcard/minimal-screen.png "$script:State\$Name.png"
 return $xml
}
$null=Shell am force-stop local.minimal.tv
$null=Shell am start -W -n $script:Launcher
Start-Sleep -Seconds 1
Assert-Top 'local.minimal.tv'
$xml=Snapshot 'navigation-home'
if($xml -notmatch 'text="HDMI 1"[^>]*focused="true"'){throw 'Initial HDMI focus not verified'}
if($xml -notmatch 'text="显示器设置"'){throw 'Monitor settings shortcut missing'}
Key '22';Key '22';Key '22';Key '23'
Assert-Top 'local.minimal.tv'
$xml=Snapshot 'navigation-apps'
if($xml -notmatch '原厂桌面（恢复入口）'){throw 'Factory recovery entry missing'}
Key '4'
$xml=Snapshot 'navigation-back'
if($xml -notmatch 'text="应用列表"[^>]*focused="true"'){throw 'BACK focus restoration failed'}
Key '21';Key '23'
Assert-Top 'tv.own.owntv.captions.dev'
$null=Snapshot 'navigation-settings'
$null=Shell am start -W -n $script:Launcher
Start-Sleep -Seconds 1
Key '21';Key '23'
Assert-Top 'com.xiaomi.mitv.settings'
$null=Snapshot 'navigation-monitor-settings'
$null=Shell am start -W -n $script:Launcher
Start-Sleep -Seconds 1
Key '21';Key '21';Key '21';Key '23'
Assert-Top 'com.xiaomi.mitv.tvplayer'
$tv=Shell dumpsys tv_input
$tv|Set-Content "$script:State\navigation-hdmi-tvinput.txt" -Encoding UTF8
if($tv -notmatch '(?s)sessionStateMap:.*?inputId: com\.mediatek\.tvinput/\.hdmi\.HDMIInputService/HW5'){throw 'HDMI1 active TV input session not verified'}
$log=Shell logcat -d -s MinimalTV:I '*:S'
$log|Set-Content "$script:State\navigation-app-log.txt" -Encoding UTF8
if($log -notmatch 'START_OK uid=\d+.*EXTSRC_PLAY.*extras'){throw 'Ordinary app HDMI launch log missing'}
$null=Snapshot 'navigation-hdmi'
# Verify factory recovery via the actual menu button.
$null=Shell am force-stop local.minimal.tv
$null=Shell am start -W -n $script:Launcher
Start-Sleep -Seconds 1
Key '22';Key '22';Key '22';Key '23';Key '23'
Assert-Top 'com.mitv.tvhome'
$null=Shell am start -W -n $script:Launcher
$result=@{passed=$true;time=(Get-Date).ToString('o');identity=$script:Identity;sha256=(Get-FileHash "$script:Root\dist\minimal-tv.apk").Hash;checks=@('initial-focus','apps','back','settings','monitor-settings','HDMI1-app-uid','factory-recovery')}
Write-Json $validation $result
Write-Event 'navigation-success' $result
Write-Host 'Navigation and ordinary-app HDMI1 launch passed. Default HOME unchanged.'
