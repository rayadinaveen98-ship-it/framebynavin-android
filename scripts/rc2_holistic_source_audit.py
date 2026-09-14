from pathlib import Path
import re

ROOT = Path('.')


def read(path: str) -> str:
    p = ROOT / path
    if not p.exists():
        raise SystemExit(f"missing required file: {path}")
    return p.read_text(encoding='utf-8')


def require(path: str, needle: str, label: str) -> None:
    text = read(path)
    if needle not in text:
        raise SystemExit(f"{label}: expected text not found in {path}: {needle}")
    print(f"PASS {label}")


def forbid_tree(root: str, pattern: str, label: str) -> None:
    regex = re.compile(pattern, re.IGNORECASE)
    hits = []
    for p in (ROOT / root).rglob('*'):
        if p.is_file() and p.suffix in {'.kt', '.kts', '.xml', '.json', '.properties'}:
            try:
                text = p.read_text(encoding='utf-8')
            except UnicodeDecodeError:
                continue
            if regex.search(text):
                hits.append(str(p))
    if hits:
        raise SystemExit(f"{label}: forbidden pattern found in: {', '.join(hits)}")
    print(f"PASS {label}")


# Release identity and Android platform safety.
require('app/build.gradle.kts', 'versionCode = 124', 'v124 version code')
require('app/build.gradle.kts', 'versionName = "2.0.0-rc2-guided-first-run"', 'v124 version name')
require('app/build.gradle.kts', 'productionSigningConfigured', 'production signing is explicit/conditional')
require('app/build.gradle.kts', 'create("productionRelease")', 'dedicated production signing config exists')
require('app/src/main/AndroidManifest.xml', 'android:allowBackup="false"', 'Android auto backup is disabled')
require('app/src/main/AndroidManifest.xml', 'android:usesCleartextTraffic="false"', 'cleartext traffic is disabled')

# App Check separation.
require('app/src/release/java/com/framebynavin/app/CreatorAppCheck.kt', 'PlayIntegrityAppCheckProviderFactory', 'release App Check uses Play Integrity')
require('app/src/debug/java/com/framebynavin/app/CreatorAppCheck.kt', 'DebugAppCheckProviderFactory', 'debug App Check uses debug provider')
require('app/src/main/java/com/framebynavin/app/MainActivity.kt', 'CreatorAppCheck.install(applicationContext)', 'App Check installs before optional AI use')

# Account/cloud contract.
require('app/src/main/java/com/framebynavin/app/cloud/CreatorCloudSyncManager.kt', 'two-device/account divergence', 'cloud conflict safety is documented in implementation')
require('app/src/main/java/com/framebynavin/app/cloud/CreatorCloudSyncManager.kt', 'expectedContentSha256', 'cloud push uses compare-and-swap expectation')
require('app/src/main/java/com/framebynavin/app/cloud/CreatorCloudSyncWorker.kt', 'NetworkType.CONNECTED', 'background cloud sync requires network')
require('supabase/migrations/20260914174737_creator_snapshot_sync_v123.sql', 'enable row level security', 'creator sync RLS migration exists')
require('supabase/migrations/20260914174737_creator_snapshot_sync_v123.sql', 'auth.uid()', 'creator sync ownership derives from authenticated user')
require('app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt', 'Google Drive is no longer the automatic backup system', 'Drive is demoted to optional manual copy')

# Guided first run and product journey.
require('app/src/main/java/com/framebynavin/app/data/CreatorGuidedTour.kt', 'TODAY,', 'guided tour starts at Today')
require('app/src/main/java/com/framebynavin/app/data/CreatorGuidedTour.kt', 'CONTROL,', 'guided tour reaches Control')
require('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt', 'onReplayTour', 'guided tour can be replayed')
require('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt', 'externalStudioId = savedTaskId', 'new project routes directly into its workspace')

# Insights V2 evidence/interaction must remain present through later slices.
require('app/src/main/java/com/framebynavin/app/ui/V20InsightsDrilldownUi.kt', '24H', '24H Insights evidence surface remains present')
require('app/src/main/java/com/framebynavin/app/ui/V20InsightsDrilldownUi.kt', 'Daily Views', 'Daily Views drill-down remains present')

# Secrets: public client IDs/publishable keys are allowed; privileged server keys are not.
forbid_tree('app/src', r'\bservice[_-]?role\b|sb_secret_|SUPABASE_SERVICE', 'no privileged Supabase credential in Android source')

print('RC2 holistic source audit passed')
