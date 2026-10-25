param(
    [string]$JavaHomePath = $env:JAVA_HOME,
    [string]$AndroidSdkRoot = $env:ANDROID_HOME,
    [string]$PlatformJarPath,
    [string]$BuildToolsPath
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$metadata = @{}
Get-Content -LiteralPath (Join-Path $projectRoot 'version.properties') -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $metadata[$matches[1].Trim()] = $matches[2].Trim() }
}
if (-not $AndroidSdkRoot) { $AndroidSdkRoot = $env:ANDROID_SDK_ROOT }
if (-not $PlatformJarPath -and $AndroidSdkRoot) { $PlatformJarPath = Join-Path $AndroidSdkRoot ('platforms\android-' + $metadata['compileSdk'] + '\android.jar') }
if (-not $BuildToolsPath -and $AndroidSdkRoot) { $BuildToolsPath = Join-Path $AndroidSdkRoot ('build-tools\' + $metadata['buildToolsVersion']) }
if (-not $JavaHomePath) { throw 'Set JAVA_HOME or pass -JavaHomePath (JDK 17+).' }
$java = Join-Path $JavaHomePath 'bin\java.exe'
$javac = Join-Path $JavaHomePath 'bin\javac.exe'
$jar = Join-Path $JavaHomePath 'bin\jar.exe'
$keytool = Join-Path $JavaHomePath 'bin\keytool.exe'
$aapt = Join-Path $BuildToolsPath 'aapt2.exe'
$zipalign = Join-Path $BuildToolsPath 'zipalign.exe'
$d8 = Join-Path $BuildToolsPath 'lib\d8.jar'
$signer = Join-Path $BuildToolsPath 'lib\apksigner.jar'
foreach ($required in @($java, $javac, $jar, $keytool, $aapt, $zipalign, $d8, $signer, $PlatformJarPath)) {
    if (-not $required -or -not (Test-Path -LiteralPath $required)) { throw "Missing build dependency: $required" }
}
function Run-Checked([string]$exe, [string[]]$arguments) {
    & $exe @arguments
    if ($LASTEXITCODE -ne 0) { throw "Build command failed ($LASTEXITCODE): $exe" }
}
$utf8 = New-Object System.Text.UTF8Encoding($false)
$work = Join-Path $projectRoot ('app\build\manual\' + [Guid]::NewGuid().ToString('N'))
$classes = Join-Path $work 'classes'
$generated = Join-Path $work 'generated'
$dex = Join-Path $work 'dex'
$testClasses = Join-Path $work 'tests'
$output = Join-Path $projectRoot 'app\build\outputs\apk\debug'
foreach ($directory in @($work, $classes, $generated, $dex, $testClasses, $output)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}

Write-Output 'Running numerical and missing-data checks...'
$source = Join-Path $projectRoot 'app\src\main\java\dev\wattscope'
$testSources = @((Join-Path $source 'PowerMath.java'), (Join-Path $source 'PowerSample.java'), (Join-Path $source 'SessionStats.java'), (Join-Path $source 'SampleHistory.java'), (Join-Path $source 'ChartData.java'), (Join-Path $source 'OverlayStyle.java'), (Join-Path $projectRoot 'tests\PowerCoreTest.java'), (Join-Path $projectRoot 'tests\DisplaySettingsTest.java'), (Join-Path $projectRoot 'tests\RangeStatisticsTest.java'))
Run-Checked $javac (@('-encoding', 'UTF-8', '-source', '8', '-target', '8', '-d', $testClasses) + $testSources)
Run-Checked $java @('-cp', $testClasses, 'dev.wattscope.PowerCoreTest')
Run-Checked $java @('-cp', $testClasses, 'dev.wattscope.DisplaySettingsTest')
Run-Checked $java @('-cp', $testClasses, 'dev.wattscope.RangeStatisticsTest')

Write-Output 'Compiling Android resources...'
$resources = Join-Path $work 'resources.zip'
Run-Checked $aapt @('compile', '--dir', (Join-Path $projectRoot 'app\src\main\res'), '-o', $resources)
[xml]$manifest = Get-Content -LiteralPath (Join-Path $projectRoot 'app\src\main\AndroidManifest.xml') -Raw -Encoding UTF8
$manifest.DocumentElement.SetAttribute('package', $metadata['applicationId'])
$application = $manifest.manifest.application
$application.SetAttribute('label', 'http://schemas.android.com/apk/res/android', $metadata['appLabel']) | Out-Null
$application.SetAttribute('debuggable', 'http://schemas.android.com/apk/res/android', 'true') | Out-Null
$manifestPath = Join-Path $work 'AndroidManifest.xml'
$manifest.Save($manifestPath)
$unsigned = Join-Path $work 'unsigned.apk'
Run-Checked $aapt @('link', '-o', $unsigned, '-I', $PlatformJarPath, '--manifest', $manifestPath,
    '--custom-package', $metadata['namespace'], '--java', $generated, '--min-sdk-version', $metadata['minSdk'], '--target-sdk-version', $metadata['targetSdk'],
    '--version-code', $metadata['versionCode'], '--version-name', $metadata['versionName'], $resources)

Write-Output 'Compiling native Android Java sources...'
$javaSources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'app\src\main\java'), $generated -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
$sourcesFile = Join-Path $work 'sources.txt'
[System.IO.File]::WriteAllLines($sourcesFile, @($javaSources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }), $utf8)
$bootClasspath = $PlatformJarPath + [System.IO.Path]::PathSeparator + (Join-Path $BuildToolsPath 'core-lambda-stubs.jar')
Run-Checked $javac @('-encoding', 'UTF-8', '-d', $testClasses, (Join-Path $projectRoot 'tools\CompileDriver.java'))
Run-Checked $java @('-cp', $testClasses, 'CompileDriver', $bootClasspath, $classes, $sourcesFile)
$classJar = Join-Path $work 'classes.jar'
Run-Checked $jar @('--create', '--file', $classJar, '-C', $classes, '.')
Run-Checked $java @('-cp', $d8, 'com.android.tools.r8.D8', '--min-api', $metadata['minSdk'], '--lib', $PlatformJarPath, '--output', $dex, $classJar)

Write-Output 'Assembling and signing a development APK...'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open($unsigned, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    Get-ChildItem -LiteralPath $dex -Filter '*.dex' | ForEach-Object {
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $_.FullName, $_.Name, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $archive.Dispose() }
$aligned = Join-Path $work 'aligned.apk'
Run-Checked $zipalign @('-f', '-p', '4', $unsigned, $aligned)
$keyDirectory = Join-Path $projectRoot '.toolchain'
New-Item -ItemType Directory -Path $keyDirectory -Force | Out-Null
$keystore = Join-Path $keyDirectory 'guidetrial-debug.keystore'
if (-not (Test-Path -LiteralPath $keystore)) {
    Run-Checked $keytool @('-genkeypair', '-keystore', $keystore, '-storepass', 'android', '-keypass', 'android',
        '-alias', 'androiddebugkey', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000',
        '-dname', 'CN=WattScope Development,O=Local Development,C=CN')
}
$apk = Join-Path $output ('WattScope-' + $metadata['versionName'] + '-debug.apk')
Run-Checked $java @('-jar', $signer, 'sign', '--ks', $keystore, '--ks-key-alias', 'androiddebugkey',
    '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', $apk, $aligned)
Run-Checked $java @('-jar', $signer, 'verify', '--verbose', '--print-certs', $apk)
Run-Checked $zipalign @('-c', '-p', '4', $apk)
Get-FileHash -LiteralPath $apk -Algorithm SHA256 | Format-List
Write-Output "APK: $apk"
