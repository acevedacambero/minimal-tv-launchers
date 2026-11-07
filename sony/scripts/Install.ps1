param([string]$Adb = 'adb', [string]$Device = $env:TV_ADB_SERIAL)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$null = Save-FirstBackup
$apk = Join-Path $script:Root 'dist\minimal-sony.apk'
$digest = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
if ($digest -ne (Get-Content -LiteralPath "$apk.sha256" -Raw).Trim()) { throw 'Local APK SHA-256 mismatch' }
$result = Invoke-Adb install --no-streaming -r --user 0 $apk
if ($result -notmatch '(?m)^Success\s*$') { throw $result }
$null = Shell sync
$path = Shell pm path local.minimal.sony
if ($path -notmatch '^package:(/data/app/[A-Za-z0-9_=/~.+-]+/base\.apk)$') { throw "Invalid APK path: $path" }
$remoteDigest = ((Shell sha256sum $Matches[1]) -split '\s+')[0]
if ($remoteDigest -ne $digest) { throw 'Installed APK SHA-256 mismatch' }
Write-Event 'install-success' @{ sha256 = $digest }
$null = Shell am start -W -n $script:Launcher
Assert-Top 'local.minimal.sony'
Write-Host 'Sony launcher installed and opened.'
