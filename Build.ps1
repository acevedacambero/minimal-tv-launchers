[CmdletBinding()]
param(
    [Parameter(Mandatory)] [ValidateSet('redmi','sony')] [string]$Variant,
    [string]$Sdk = $(if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { $env:ANDROID_HOME }),
    [string]$Jdk = $env:JAVA_HOME,
    [string]$BuildTools = '36.0.0',
    [string]$Platform = 'android-34'
)

$ErrorActionPreference = 'Stop'
if (!$Sdk -or !$Jdk) { throw 'Set ANDROID_HOME (or ANDROID_SDK_ROOT) and JAVA_HOME, or pass -Sdk and -Jdk.' }
$project = Join-Path $PSScriptRoot $Variant.ToLowerInvariant()
$app = Join-Path $project 'app'
$tools = Join-Path $Sdk "build-tools/$BuildTools"
$androidJar = Join-Path $Sdk "platforms/$Platform/android.jar"
$java = Join-Path $Jdk 'bin'
if (!(Test-Path -LiteralPath $androidJar) -or !(Test-Path -LiteralPath "$tools/aapt2.exe") -or
    !(Test-Path -LiteralPath "$java/javac.exe")) { throw 'Android SDK platform, Build Tools, or JDK is missing.' }
$env:JAVA_HOME = $Jdk
$manifest = Join-Path $app 'AndroidManifest.xml'
$sdkInfo = ([xml](Get-Content -LiteralPath $manifest -Raw)).manifest.'uses-sdk'
$minApi = $sdkInfo.GetAttribute('minSdkVersion','http://schemas.android.com/apk/res/android')
$apkName = if ($Variant -eq 'redmi') { 'minimal-tv.apk' } else { 'minimal-sony.apk' }
$dist = Join-Path $project 'dist'
$key = Join-Path $project 'signing/development.p12'
$buildRoot = Join-Path $project 'build'
$work = Join-Path $buildRoot ([Guid]::NewGuid().ToString('N'))
$apk = Join-Path $dist $apkName
New-Item -ItemType Directory -Force -Path $dist,(Split-Path $key),"$work/classes","$work/dex" | Out-Null

function Invoke-BuildTool([string]$Executable,[string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Build tool failed: $Executable" }
}

try {
    Invoke-BuildTool "$tools/aapt2.exe" @('compile','--dir',"$app/res",'-o',"$work/resources.zip")
    Invoke-BuildTool "$tools/aapt2.exe" @('link','-o',"$work/unsigned.apk",'--manifest',$manifest,'-I',$androidJar,"$work/resources.zip")
    $sources = @(Get-ChildItem -LiteralPath "$app/src" -Recurse -Filter '*.java' -File | ForEach-Object FullName)
    Invoke-BuildTool "$java/javac.exe" (@('-encoding','UTF-8','--release','8','-classpath',$androidJar,'-d',"$work/classes") + $sources)
    $classes = @(Get-ChildItem -LiteralPath "$work/classes" -Recurse -Filter '*.class' -File | ForEach-Object FullName)
    Invoke-BuildTool "$tools/d8.bat" (@('--lib',$androidJar,'--min-api',$minApi,'--output',"$work/dex") + $classes)
    Invoke-BuildTool "$java/jar.exe" @('uf',"$work/unsigned.apk",'-C',"$work/dex",'classes.dex')
    Invoke-BuildTool "$tools/zipalign.exe" @('-f','4',"$work/unsigned.apk","$work/aligned.apk")
    if (!(Test-Path -LiteralPath $key)) {
        Invoke-BuildTool "$java/keytool.exe" @('-genkeypair','-keystore',$key,'-storetype','PKCS12','-storepass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Minimal TV Development')
    }
    Invoke-BuildTool "$tools/apksigner.bat" @('sign','--ks',$key,'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--out',"$work/signed.apk","$work/aligned.apk")
    Invoke-BuildTool "$tools/apksigner.bat" @('verify','--verbose',"$work/signed.apk")
    Copy-Item -LiteralPath "$work/signed.apk" -Destination $apk -Force
    (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash | Set-Content -LiteralPath "$apk.sha256" -Encoding ASCII
    Get-Item -LiteralPath $apk
} finally {
    $resolvedWork = (Resolve-Path -LiteralPath $work).Path
    if ((Split-Path $resolvedWork -Parent) -eq (Resolve-Path -LiteralPath $buildRoot).Path -and
        (Split-Path $resolvedWork -Leaf) -match '^[0-9a-f]{32}$') {
        Remove-Item -LiteralPath $resolvedWork -Recurse
    }
}
