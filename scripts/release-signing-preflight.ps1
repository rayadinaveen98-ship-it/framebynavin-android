$ErrorActionPreference = "Stop"

$required = @(
    "FRAMEBYNAVIN_RELEASE_STORE_FILE",
    "FRAMEBYNAVIN_RELEASE_STORE_PASSWORD",
    "FRAMEBYNAVIN_RELEASE_KEY_ALIAS",
    "FRAMEBYNAVIN_RELEASE_KEY_PASSWORD"
)

$missing = @()
foreach ($name in $required) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value)) { $missing += $name }
}

if ($missing.Count -gt 0) {
    Write-Host "Missing release-signing environment variables:" -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host "  - $_" }
    exit 2
}

$storeFile = [Environment]::GetEnvironmentVariable("FRAMEBYNAVIN_RELEASE_STORE_FILE")
$keyAlias = [Environment]::GetEnvironmentVariable("FRAMEBYNAVIN_RELEASE_KEY_ALIAS")

if (-not (Test-Path -LiteralPath $storeFile)) {
    throw "Keystore not found: $storeFile"
}

$keytool = Get-Command keytool -ErrorAction Stop
Write-Host "Release keystore: $storeFile"
Write-Host "Alias: $keyAlias"
Write-Host ""
Write-Host "Certificate fingerprints required by Google/Firebase:" -ForegroundColor Cyan

& $keytool.Source -list -v `
    -keystore $storeFile `
    -alias $keyAlias `
    -storepass:env FRAMEBYNAVIN_RELEASE_STORE_PASSWORD |
    Select-String -Pattern "SHA1:|SHA256:"

if ($LASTEXITCODE -ne 0) {
    throw "keytool could not inspect the release certificate."
}

Write-Host ""
Write-Host "Building production release APK + AAB..." -ForegroundColor Cyan
& .\gradlew.bat :app:assembleRelease :app:bundleRelease --stacktrace
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = "app\build\outputs\apk\release\app-release.apk"
$aab = "app\build\outputs\bundle\release\app-release.aab"

if (-not (Test-Path -LiteralPath $apk)) {
    throw "Signed release APK was not produced. Check the four FRAMEBYNAVIN_RELEASE_* variables."
}
if (-not (Test-Path -LiteralPath $aab)) {
    throw "Release AAB was not produced."
}

Write-Host ""
Write-Host "RC1 production-signing preflight passed." -ForegroundColor Green
Write-Host "APK: $apk"
Write-Host "AAB: $aab"
Write-Host "Register the SHA-1 and SHA-256 above in Google/Firebase before production OAuth/App Check validation."
