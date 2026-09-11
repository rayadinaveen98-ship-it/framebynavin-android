from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"{label}: expected source contract not found; refusing broad rewrite")
    path.write_text(text.replace(old, new, 1))


app = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
replace_once(
    app,
    '''    val settingsStore = remember { CreatorOsSettingsStore(context.applicationContext) }
    var settings by remember { mutableStateOf(settingsStore.snapshot()) }
    var permissions by remember { mutableStateOf(pPermissions(context)) }''',
    '''    val settingsStore = remember { CreatorOsSettingsStore(context.applicationContext) }
    val driveVaultLocalStore = remember { DriveVaultLocalStore(context.applicationContext) }
    var driveRecoveryRevision by rememberSaveable { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(settingsStore.snapshot()) }
    var permissions by remember { mutableStateOf(pPermissions(context)) }''',
    "Drive recovery observable state",
)

replace_once(
    app,
    '''    } else if ((!settings.onboardingComplete || !settings.creatorProfile.isComplete) &&
        CloudSyncManager(context.applicationContext).localState().session?.let { session ->
            !DriveVaultLocalStore(context.applicationContext).recoveryReviewed(session.email)
        } == true
    ) {
        val connected = CloudSyncManager(context.applicationContext).localState().session!!
        V20DriveRecoveryGate(
            session = connected,
            onRecovered = { settings = settingsStore.snapshot() },
            onStartNew = { settings = settingsStore.snapshot() },
        )''',
    '''    } else if ((!settings.onboardingComplete || !settings.creatorProfile.isComplete) &&
        CloudSyncManager(context.applicationContext).localState().session?.let { session ->
            // Read revision so marking recovery reviewed causes this gate to be re-evaluated immediately.
            driveRecoveryRevision
            !driveVaultLocalStore.recoveryReviewed(session.email)
        } == true
    ) {
        val connected = CloudSyncManager(context.applicationContext).localState().session!!
        V20DriveRecoveryGate(
            session = connected,
            onRecovered = {
                driveRecoveryRevision += 1
                settings = settingsStore.snapshot()
            },
            onStartNew = {
                driveRecoveryRevision += 1
                settings = settingsStore.snapshot()
            },
        )''',
    "Drive recovery gate transition",
)

# New installable hotfix. Alpha 2 will move to the next versionCode after this storage fix is accepted.
gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 88' not in build or 'versionName = "2.0.0-alpha1.2b-drive-vault"' not in build:
    raise SystemExit("Unexpected Drive-vault build identity")
build = build.replace('versionCode = 88', 'versionCode = 89', 1)
build = build.replace('versionName = "2.0.0-alpha1.2b-drive-vault"', 'versionName = "2.0.0-alpha1.2b.1-drive-gate-hotfix"', 1)
gradle.write_text(build)
