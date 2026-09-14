param(
    [switch]$SkipIdentityRegistrationCheck
)

$ErrorActionPreference = "Stop"

function Require-Command([string]$Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $cmd) { throw "Required command not found: $Name" }
    return $cmd
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$required = @(
    "FRAMEBYNAVIN_RELEASE_STORE_FILE",
    "FRAMEBYNAVIN_RELEASE_STORE_PASSWORD",
    "FRAMEBYNAVIN_RELEASE_KEY_ALIAS",
    "FRAMEBYNAVIN_RELEASE_KEY_PASSWORD"
)

$missing = @()
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        $missing += $name
    }
}
if ($missing.Count -gt 0) {
    Write-Host "Production release gate is blocked until these secure environment variables are supplied:" -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host "  - $_" }
    exit 2
}

$storeFile = [Environment]::GetEnvironmentVariable("FRAMEBYNAVIN_RELEASE_STORE_FILE")
$keyAlias = [Environment]::GetEnvironmentVariable("FRAMEBYNAVIN_RELEASE_KEY_ALIAS")
if (-not (Test-Path -LiteralPath $storeFile)) { throw "Keystore not found: $storeFile" }

$keytool = Require-Command "keytool"
$python = Require-Command "python"

$verificationDir = Join-Path $repoRoot "verification\production"
New-Item -ItemType Directory -Force -Path $verificationDir | Out-Null

Write-Host "[1/6] Reading production certificate fingerprints..." -ForegroundColor Cyan
$keyOutput = & $keytool.Source -list -v `
    -keystore $storeFile `
    -alias $keyAlias `
    -storepass:env FRAMEBYNAVIN_RELEASE_STORE_PASSWORD 2>&1
if ($LASTEXITCODE -ne 0) { throw "keytool could not inspect the production certificate." }

$sha1Line = $keyOutput | Select-String -Pattern "SHA1:" | Select-Object -First 1
$sha256Line = $keyOutput | Select-String -Pattern "SHA256:" | Select-Object -First 1
if ($null -eq $sha1Line -or $null -eq $sha256Line) { throw "Could not read SHA-1/SHA-256 from the production certificate." }
$sha1 = ($sha1Line.ToString() -replace '^.*SHA1:\s*', '').Trim()
$sha256 = ($sha256Line.ToString() -replace '^.*SHA256:\s*', '').Trim()
Write-Host "SHA-1:   $sha1"
Write-Host "SHA-256: $sha256"

Write-Host "[2/6] Checking Google/Firebase repository identity configuration..." -ForegroundColor Cyan
$identityArgs = @("scripts/production_identity_readiness.py")
if (-not $SkipIdentityRegistrationCheck) { $identityArgs += "--require-android-oauth-client" }
& $python.Source @identityArgs
if ($LASTEXITCODE -ne 0) {
    if (-not $SkipIdentityRegistrationCheck) {
        Write-Host "Register the SHA-1 above with the Android OAuth/Firebase app, refresh app/google-services.json, then rerun." -ForegroundColor Yellow
    }
    exit $LASTEXITCODE
}

Write-Host "[3/6] Building production-signed release APK + AAB..." -ForegroundColor Cyan
& .\gradlew.bat :app:lintRelease :app:assembleRelease :app:bundleRelease --stacktrace
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Join-Path $repoRoot "app\build\outputs\apk\release\app-release.apk"
$aab = Join-Path $repoRoot "app\build\outputs\bundle\release\app-release.aab"
if (-not (Test-Path -LiteralPath $apk)) { throw "Signed release APK was not produced." }
if (-not (Test-Path -LiteralPath $aab)) { throw "Signed release AAB was not produced." }

Write-Host "[4/6] Verifying signed APK identity..." -ForegroundColor Cyan
$androidHome = [Environment]::GetEnvironmentVariable("ANDROID_HOME")
if ([string]::IsNullOrWhiteSpace($androidHome)) { $androidHome = [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT") }
if ([string]::IsNullOrWhiteSpace($androidHome)) { throw "ANDROID_HOME or ANDROID_SDK_ROOT is required to find apksigner." }
$apkSigner = Get-ChildItem -Path (Join-Path $androidHome "build-tools") -Filter "apksigner*" -Recurse -File |
    Sort-Object FullName -Descending |
    Select-Object -First 1
if ($null -eq $apkSigner) { throw "apksigner was not found under Android build-tools." }
$apkVerify = & $apkSigner.FullName verify --verbose --print-certs $apk 2>&1
if ($LASTEXITCODE -ne 0) { throw "apksigner rejected the production APK.`n$($apkVerify -join "`n")" }
$apkVerify | Tee-Object -FilePath (Join-Path $verificationDir "apk-signature.txt") | Out-Host

$apkSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk).Hash.ToLowerInvariant()
$aabSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $aab).Hash.ToLowerInvariant()

Write-Host "[5/6] Verifying bundle JAR signature..." -ForegroundColor Cyan
$jarsigner = Require-Command "jarsigner"
& $jarsigner.Source -verify -strict $aab | Tee-Object -FilePath (Join-Path $verificationDir "aab-signature.txt")
if ($LASTEXITCODE -ne 0) { throw "jarsigner rejected the release AAB." }

Write-Host "[6/6] Writing production verification manifest..." -ForegroundColor Cyan
$commit = (& git rev-parse HEAD).Trim()
$manifest = @(
    "sourceCommit=$commit",
    "versionName=2.0.0-rc2-guided-first-run",
    "versionCode=124",
    "packageName=com.framebynavin.app",
    "certificateSha1=$sha1",
    "certificateSha256=$sha256",
    "apkSha256=$apkSha256",
    "aabSha256=$aabSha256",
    "apkSignatureVerified=true",
    "aabSignatureVerified=true",
    "androidOAuthConfigChecked=$(-not $SkipIdentityRegistrationCheck)",
    "playIntegritySha256Registration=verify-in-firebase-console",
    "runtimeGoogleSignIn=still-required-on-device",
    "runtimeYouTubeAuthorization=still-required-on-device",
    "runtimeFirebaseAiAppCheck=still-required-on-device"
)
$manifest | Set-Content -Encoding UTF8 (Join-Path $verificationDir "production-release-manifest.txt")

Write-Host ""
Write-Host "Production signing/package gate passed." -ForegroundColor Green
Write-Host "APK: $apk"
Write-Host "AAB: $aab"
Write-Host "Verification: $verificationDir"
Write-Host ""
Write-Host "Still required before public release:" -ForegroundColor Yellow
Write-Host "  1. Confirm SHA-1 Android OAuth registration and SHA-256 Play Integrity/App Check registration."
Write-Host "  2. Install this exact production-signed build on a physical phone."
Write-Host "  3. Exercise Google sign-in, YouTube authorization, Firebase AI/App Check, reminders, reboot/background, and Supabase sync/restore."
