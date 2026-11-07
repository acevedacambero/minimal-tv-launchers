param([string]$Adb='adb',[string]$Device=$env:TV_ADB_SERIAL,[switch]$ListOnly)
. "$PSScriptRoot\Common.ps1"
Assert-Device
$null=Save-FirstBackup
$list=Shell pm list packages -d --user 0
$names=@($list -split '\r?\n' | Where-Object {$_ -match '^package:'} | ForEach-Object {$_.Substring(8)})
if($names.Count -eq 0){Write-Host 'No disabled apps.';return}
$plan=@(foreach($name in $names){[pscustomobject][ordered]@{package=$name;before=(Get-PackageState $name)}})
$plan | Format-Table package,before -AutoSize | Out-Host
if($ListOnly){Write-Host 'Preview only; no changes.';return}
Write-Host 'This independent option also enables historical disabled apps. The normal Restore script does not undo this option.'
if((Read-Host 'Type ENABLE ALL to proceed') -cne 'ENABLE ALL'){Write-Host 'Cancelled';return}
Write-Json "$script:State\enable-all-plan-$(Get-Date -Format yyyyMMdd-HHmmss).json" $plan
foreach($item in $plan){
 Write-Event 'enable-all-planned' $item
 $null=Shell pm enable --user 0 $item.package
 if((Get-PackageState $item.package) -ne 1){throw "Enable failed: $($item.package)"}
 Write-Event 'enable-all-success' $item
}
if((Shell pm list packages -d --user 0) -match '^package:'){throw 'Disabled apps remain'}
Write-Host 'All previously disabled user-0 apps enabled.'
