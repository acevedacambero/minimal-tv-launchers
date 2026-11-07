param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$backup=Save-FirstBackup
if((Get-PackageState 'com.mitv.tvhome') -in 2,3,4){throw 'Factory recovery launcher is disabled; stop for state review'}
$apk="$script:Root\dist\minimal-tv.apk"
$hash=(Get-FileHash $apk -Algorithm SHA256).Hash
$expected=(Get-Content "$apk.sha256" -Raw).Trim()
if($hash -ne $expected){throw 'APK SHA256 mismatch'}
Write-Event 'install-planned' @{sha256=$hash}
$output=Invoke-Adb install --no-incremental --no-streaming -r --user 0 $apk
if($output -notmatch 'Success'){throw $output}
$null=Shell sync
$installedPath=Shell pm path local.minimal.tv
if($installedPath -notmatch '^package:(/data/app/[A-Za-z0-9_=/~.-]+/base\.apk)$'){throw 'Installed APK path was not registered'}
$remoteApk=$Matches[1]
$remoteDigest=(Shell sha256sum $remoteApk) -split '\s+' | Select-Object -First 1
if($remoteDigest -ne $hash){throw 'Installed APK SHA256 mismatch'}
Write-Event 'install-success' @{sha256=$hash;package='local.minimal.tv';mode='push';installedPath=$remoteApk}
$null=Shell am start -W -n $script:Launcher
Start-Sleep -Seconds 1
Assert-Top 'local.minimal.tv'
Write-Host 'Installed and opened. Default HOME unchanged. Run Test-Navigation.ps1 before Switch-Home.ps1.'
