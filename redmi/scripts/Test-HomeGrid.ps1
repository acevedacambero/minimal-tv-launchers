param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$null=Save-FirstBackup
$folder="$script:State\grid-$(Get-Date -Format yyyyMMdd-HHmmss)"
New-Item -ItemType Directory $folder|Out-Null
function Key([string]$Code){$null=Shell input keyevent $Code;Start-Sleep -Milliseconds 500}
function Ui([string]$Name){
 $null=Shell uiautomator dump /sdcard/minimal-window.xml
 $xml=Shell cat /sdcard/minimal-window.xml
 $xml|Set-Content "$folder\$Name.xml" -Encoding UTF8
 [xml]$xml
}
function Focus([string]$Name,[string]$Label){
 $tree=Ui $Name
 $node=$tree.SelectSingleNode('//node[@focused="true"]')
 if(!$node -or $node.text -ne $Label){throw "Unexpected focus; expected $Label"}
}
function Start-Home {
 $null=Shell am start -W -n $script:Launcher
 Start-Sleep -Milliseconds 500
 Key '4'
 Assert-Top 'local.minimal.tv'
}
Start-Home
$tree=Ui 'layout'
$buttons=@($tree.SelectNodes('//node[@class="android.widget.Button"]'))
$labels=@('HDMI 1','显示器设置','OwnTV 字幕开发版','应用列表','BT','Clash Meta','SmartTube','Televizo')
if($buttons.Count -ne 8){throw "Expected eight home buttons, got $($buttons.Count)"}
$positions=@()
foreach($label in $labels){
 $node=@($buttons|Where-Object {$_.text -eq $label})
 if($node.Count -ne 1){throw "Missing or duplicated shortcut: $label"}
 $m=[regex]::Match($node[0].bounds,'^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
 if(!$m.Success){throw 'Unexpected UI bounds'}
 $positions+=@{label=$label;x=[int]$m.Groups[1].Value;y=[int]$m.Groups[2].Value}
}
if(@($positions[0..3].y|Sort-Object -Unique).Count -ne 1 -or @($positions[4..7].y|Sort-Object -Unique).Count -ne 1 -or $positions[4].y -le $positions[0].y){throw 'Shortcuts are not in two rows of four'}
for($i=0;$i -lt 4;$i++){if($positions[$i].x -ne $positions[$i+4].x){throw 'Grid columns misaligned'}}
$null=Shell screencap -p /sdcard/minimal-screen.png
$null=Invoke-Adb pull /sdcard/minimal-screen.png "$script:State\final-home.png"
Key '20';Focus 'down-to-bt' 'BT'
Key '19';Focus 'up-to-hdmi' 'HDMI 1'
$packages=@('com.chinasoul.bt','com.github.metacubex.clash.meta','com.teamsmart.videomanager.tv','com.ottplay.ottplay')
for($i=0;$i -lt 4;$i++){
 Start-Home
 Key '20'
 for($j=0;$j -lt $i;$j++){Key '22'}
 Focus "shortcut-$i" $labels[$i+4]
 Key '23'
 Start-Sleep -Seconds 2
 Assert-Top $packages[$i]
 (Get-Top)|Set-Content "$folder\launched-$i.txt" -Encoding UTF8
 Write-Host "Opened $($labels[$i+4])"
}
Start-Home
Key '20';Key '22';Key '22';Key '22';Key '19'
Focus 'up-to-apps' '应用列表'
# Verify HOME after leaving the launcher; do not change any VPN, media or app configuration.
$null=Shell am start -W -a android.settings.SETTINGS
Start-Sleep -Seconds 1
Key '3'
Assert-Top 'local.minimal.tv'
Key '4'
$result=@{passed=$true;time=(Get-Date).ToString('o');sha256=(Get-FileHash "$script:Root\dist\minimal-tv.apk").Hash;positions=$positions;packages=$packages;checks=@('two-rows-four-columns','up-down-focus','bottom-row-launches','home-return');identity=$script:Identity}
Write-Json "$folder\result.json" $result
Write-Event 'home-grid-success' $result
Write-Host 'Eight shortcuts verified; HOME mapping remains active.'
