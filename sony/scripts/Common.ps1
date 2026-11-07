Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:Root = Split-Path $PSScriptRoot -Parent
$script:State = Join-Path $script:Root 'state'
$script:Launcher = 'local.minimal.sony/.MainActivity'

function Invoke-Adb {
    $arguments = @($args)
    $output = (& $Adb -s $Device @arguments 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $output -match '(?im)^Error:|^Exception occurred while executing|^Failure\s*\[') {
        throw "ADB failed: $($arguments -join ' ')`n$output"
    }
    return $output
}
function Shell { Invoke-Adb shell @args }

function Assert-Device {
    if (!$Device) { throw 'Pass -Device with the TV ADB address (IP:port), or set TV_ADB_SERIAL.' }
    if (!(Get-Command $Adb -CommandType Application -ErrorAction SilentlyContinue)) { throw "ADB executable not found: $Adb" }
    $null = & $Adb connect $Device
    if ($LASTEXITCODE -ne 0 -or (Invoke-Adb get-state) -ne 'device') { throw 'Sony ADB is offline' }
    $script:Identity = [ordered]@{
        manufacturer = (Shell getprop ro.product.manufacturer)
        model = (Shell getprop ro.product.model)
        build = (Shell getprop ro.build.display.id)
        sdk = (Shell getprop ro.build.version.sdk)
        serial = (Shell getprop ro.serialno)
        fingerprint = (Shell getprop ro.build.fingerprint)
    }
    if ($script:Identity.manufacturer -ne 'Sony' -or $script:Identity.model -ne 'BRAVIA VH1' -or
        $script:Identity.build -ne 'BRAVIA_VH1_CN-user 10 QTG3.200305.006.S387 622661 release-keys' -or
        $script:Identity.sdk -ne '29') { throw 'Sony model or firmware differs from the verified device' }
}

function Get-Home {
    $output = Shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
    $component = @($output -split '\r?\n' | Where-Object { $_ -match '^[\w.]+/[\w.$]+$' })
    if ($component.Count -ne 1 -or $component[0] -match '/.*ResolverActivity$') { throw "No unique default HOME: $output" }
    return $component[0]
}

function Read-Backup {
    $backup = Get-Content "$script:State\first-backup.json" -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($backup.identity.serial -ne $script:Identity.serial -or $backup.identity.fingerprint -ne $script:Identity.fingerprint) {
        throw 'Backup belongs to another device or firmware'
    }
    return $backup
}

function Save-FirstBackup {
    New-Item -ItemType Directory -Force -Path $script:State | Out-Null
    $path = Join-Path $script:State 'first-backup.json'
    if (Test-Path -LiteralPath $path) { return (Read-Backup) }
    $states = [ordered]@{}
    $installed = Shell pm list packages --user 0
    $dump = Shell dumpsys package
    foreach ($entry in [regex]::Split($dump, '(?m)^  Package \[') | Select-Object -Skip 1) {
        $name = [regex]::Match($entry, '^([^\]]+)\]')
        $state = [regex]::Match($entry, '(?m)^\s*User 0:.*\benabled=(\d+)\b')
        if ($name.Success -and $state.Success -and !$states.Contains($name.Groups[1].Value)) {
            $states[$name.Groups[1].Value] = [int]$state.Groups[1].Value
        }
    }
    foreach ($line in ($installed -split '\r?\n')) {
        if ($line.StartsWith('package:') -and !$states.Contains($line.Substring(8))) { throw "Missing original state for $line" }
    }
    $originalComponent = Get-Home
    if ($originalComponent -eq $script:Launcher) { throw 'Cannot create an original backup after switching HOME' }
    $backup = [ordered]@{
        time = (Get-Date).ToString('o')
        identity = $script:Identity
        home = $originalComponent
        states = $states
        accessibilityServices = (Shell settings get secure enabled_accessibility_services)
        accessibilityEnabled = (Shell settings get secure accessibility_enabled)
    }
    $stream = [IO.File]::Open($path, [IO.FileMode]::CreateNew)
    try {
        $bytes = [Text.Encoding]::UTF8.GetBytes(($backup | ConvertTo-Json -Depth 8))
        $stream.Write($bytes, 0, $bytes.Length)
    } finally { $stream.Dispose() }
    return (Read-Backup)
}

function Assert-Top([string]$Package) {
    $output = Shell dumpsys activity activities
    if ($output -notmatch "(?m)^\s*(?:topResumedActivity|mResumedActivity)[:=].*\b$([regex]::Escape($Package))/") {
        throw "Foreground activity is not $Package"
    }
}

function Write-Event([string]$Kind, $Data) {
    [ordered]@{ time = (Get-Date).ToString('o'); kind = $Kind; data = $Data } |
        ConvertTo-Json -Depth 8 -Compress | Add-Content "$script:State\operations.jsonl" -Encoding UTF8
}

function Set-VerifiedHome([string]$Component) {
    $null = Shell cmd package set-home-activity --user 0 $Component
    Start-Sleep -Seconds 1
    if ((Get-Home) -ne $Component) { throw "Firmware did not set HOME to $Component" }
    Write-Event 'home-set' @{ component = $Component }
}
