from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"{label}: expected source contract not found; refusing broad rewrite")
    path.write_text(text.replace(old, new, 1))


# Restore the exact pre-v2 Today/Home route. Keep the v2 command-center implementation dormant
# so it can later be integrated additively instead of replacing the user's Home experience.
app = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
replace_once(
    app,
    '''                PTab.TODAY -> V20TodayCommandCenter(
                    creatorProfile = settings.creatorProfile,
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onCapture = { showQuickCapture = true },
                    onStart = vm::startTask,
                    onViewAllReminders = { showReminders = true },
                    onFocus = { focusTaskId = it },
                    onOpenProject = ::openProject,
                )''',
    '''                PTab.TODAY -> PTodayScreen(
                    creatorProfile = settings.creatorProfile,
                    tasks = vm.tasks,
                    onAdd = { openComposer() },
                    onStart = vm::startTask,
                    onAdvance = ::advanceWorkflowWithJourney,
                    onViewAllReminders = { showReminders = true },
                    onFocus = { focusTaskId = it },
                    onOpenProject = ::openProject,
                )''',
    "Home route",
)

# Unknown server lifecycle must never inherit an earlier writable lifecycle cache.
local = Path("app/src/main/java/com/framebynavin/app/cloud/CloudLocalStore.kt")
text = local.read_text()
marker = "    /** Legacy RC7 journal strings have no safe replay generation. */"
addition = '''    fun clearLifecycle() = synchronized(accountLock) {
        check(
            prefs.edit()
                .remove(KEY_LIFECYCLE)
                .remove(KEY_RECONCILED_USER)
                .putBoolean(KEY_ENABLED, false)
                .commit()
        ) { "Could not clear cloud lifecycle state" }
    }

'''
if addition.strip() not in text:
    if marker not in text:
        raise SystemExit("Cloud lifecycle insertion marker missing")
    local.write_text(text.replace(marker, addition + marker, 1))

# Authentication/session identity must persist before the optional cloud-safety RPC. A missing
# lifecycle service never authorizes upload because approval + cached lifecycle are cleared first.
manager = Path("app/src/main/java/com/framebynavin/app/cloud/CloudSyncManager.kt")
text = manager.read_text()
start_marker = "    suspend fun completeGoogleSignIn(idToken: String): CloudOperationResult {"
end_marker = "    suspend fun refreshCreatorProfile(): Result<CloudCreatorProfile?>"
start = text.index(start_marker)
end = text.index(end_marker, start)
replacement = '''    suspend fun completeGoogleSignIn(idToken: String): CloudOperationResult {
        return transitionResult { epoch ->
            runCatching {
                val session = api.signInWithGoogle(idToken)
                require(session.userId.isNotBlank()) { "Account identity missing" }
                accountGate.requireCurrent(epoch, local.generation())

                // Authentication is identity. Persist it before optional cloud-backup inspection.
                check(local.clearApproval()) { "Could not clear cloud backup approval" }
                local.clearLifecycle()
                local.saveSession(session)
                local.clearCreatorProfile()

                // Read-only lookup lets a returning creator recover identity without enabling sync.
                val profile = runCatching { api.fetchCreatorProfile(session) }
                    .onFailure { it.rethrowCancellation() }
                    .getOrNull()
                accountGate.requireCurrent(epoch, local.generation())
                require(profile == null || profile.userId == session.userId) { "Account profile mismatch" }
                profile?.let(local::saveCreatorProfile)

                val lifecycle = try {
                    api.cloudStatus(session)
                } catch (error: Throwable) {
                    error.rethrowCancellation()
                    if (isCloudSafetyServiceUnavailable(error)) {
                        val message = "Google account connected. Cloud backup safety is temporarily unavailable, so uploads remain off."
                        local.markError(message)
                        return@runCatching CloudOperationResult.Success(message)
                    }
                    throw error
                }

                accountGate.requireCurrent(epoch, local.generation())
                local.saveLifecycle(session.userId, lifecycle)
                val writable = lifecycle.active && !local.deletionPendingFor(session.userId)
                if (writable) {
                    api.upsertProfile(session, lifecycle.generation)
                    val refreshedProfile = api.fetchCreatorProfile(session)
                    accountGate.requireCurrent(epoch, local.generation())
                    require(refreshedProfile == null || refreshedProfile.userId == session.userId) { "Account profile mismatch" }
                    if (refreshedProfile != null) local.saveCreatorProfile(refreshedProfile)
                    api.upsertDevice(session, local.deviceKey(), deviceLabel(), BuildConfig.VERSION_NAME, lifecycle.generation)
                }
                val points = api.listRestorePoints(session)
                accountGate.requireCurrent(epoch, local.generation())

                if (!lifecycle.active) {
                    CloudOperationResult.Success("Account connected. Cloud creator data is deleted. Your phone data is safe. Review the account state before explicitly starting a new cloud history.")
                } else if (local.deletionPendingFor(session.userId)) {
                    CloudOperationResult.Success("Cloud data deletion is unfinished. Retry it from Account & Data. Your phone data is safe and uploads remain blocked.")
                } else if (points.isEmpty()) {
                    check(local.approveUser(session.userId)) { "Could not save cloud backup approval" }
                    CloudOperationResult.Success("Account connected. Your phone remains the main copy. Backups are manual.")
                } else {
                    CloudOperationResult.Success("Account connected. Review your existing cloud backups before creating a new one.")
                }
            }.getOrElse {
                it.rethrowCancellation()
                if (it is CloudAccountChanged) return@getOrElse CloudOperationResult.Skipped(it.message.orEmpty())
                val message = cloudMessage(it, "Couldn't connect Google account")
                local.markError(message)
                CloudOperationResult.Failure(message, retryable = it !is CloudHttpException || it.statusCode >= 500)
            }
        }
    }

    /** Read-only creator identity refresh. It never requires or grants cloud-backup authority. */
    suspend fun refreshCreatorIdentity(): Result<CloudCreatorProfile?> = runCatching {
        currentAccount { epoch ->
            val session = freshSession(local.loadSession() ?: error("Sign in with Google first"), epoch)
            val profile = api.fetchCreatorProfile(session)
            accountGate.requireCurrent(epoch, local.generation())
            require(profile == null || profile.userId == session.userId) { "Account profile mismatch" }
            if (profile == null) local.clearCreatorProfile() else local.saveCreatorProfile(profile)
            profile
        }
    }.onFailure { it.rethrowCancellation() }

'''
text = text[:start] + replacement + text[end:]
old_helper = '''    private fun cloudMessage(error: Throwable, fallback: String): String = when (error) {
        is CloudHttpException -> if (error.statusCode == 404 || error.message.contains("PGRST202"))
            "Cloud safety service is not available yet. Creator data remains on this phone; no upload was started."
        else error.message.ifBlank { fallback }
        else -> fallback
    }
'''
new_helper = '''    private fun cloudMessage(error: Throwable, fallback: String): String = when {
        isCloudSafetyServiceUnavailable(error) ->
            "Cloud backup safety service is not available yet. Creator data remains on this phone; no upload was started."
        error is CloudHttpException -> error.message.ifBlank { fallback }
        else -> fallback
    }
'''
if old_helper not in text:
    raise SystemExit("Cloud error helper contract changed; refusing broad rewrite")
text = text.replace(old_helper, new_helper, 1)
if "internal fun isCloudSafetyServiceUnavailable" not in text:
    text += '''\ninternal fun isCloudSafetyServiceUnavailable(error: Throwable): Boolean =
    error is CloudHttpException &&
        (error.statusCode == 404 || error.message.contains("PGRST202", ignoreCase = true))
'''
manager.write_text(text)

# Use identity-only refresh in onboarding. Existing creator profiles return immediately. If the
# known safety RPC is absent, an authenticated user may enter the local app; uploads stay blocked.
onboarding = Path("app/src/main/java/com/framebynavin/app/ui/V23AccountOnboarding.kt")
replace_once(
    onboarding,
    '''                        is CloudOperationResult.Success -> {
                            manager.refreshCreatorProfile()
                            reload()
                        }''',
    '''                        is CloudOperationResult.Success -> {
                            manager.refreshCreatorIdentity()
                            reload()
                            val connectedSession = manager.localState().session
                            val connectedProfile = manager.cachedCreatorProfile()
                            when {
                                connectedSession != null && !connectedProfile?.username.isNullOrBlank() ->
                                    onComplete(connectedProfile!!.displayName.ifBlank { connectedSession.displayName })
                                connectedSession != null && resultState.message.startsWith("Google account connected.") ->
                                    onComplete(connectedSession.displayName)
                            }
                        }''',
    "Account sign-in success",
)
replace_once(
    onboarding,
    '''    LaunchedEffect(Unit) {
        if (session != null) {
            manager.refreshCreatorProfile()
            reload()
        }
    }''',
    '''    LaunchedEffect(Unit) {
        if (session != null) {
            manager.refreshCreatorIdentity()
            reload()
            val connectedSession = manager.localState().session
            val connectedProfile = manager.cachedCreatorProfile()
            if (connectedSession != null && !connectedProfile?.username.isNullOrBlank()) {
                onComplete(connectedProfile!!.displayName.ifBlank { connectedSession.displayName })
            }
        }
    }''',
    "Account onboarding refresh",
)

# New installable corrective identity; never silently replace versionCode 84.
gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 84' not in build or 'versionName = "2.0.0-today-alpha1"' not in build:
    raise SystemExit("Unexpected Alpha 1 build identity")
build = build.replace('versionCode = 84', 'versionCode = 85', 1)
build = build.replace('versionName = "2.0.0-today-alpha1"', 'versionName = "2.0.0-today-alpha1.1"', 1)
gradle.write_text(build)

# Small pure regression test for the concrete production failure class.
test = Path("app/src/test/java/com/framebynavin/app/cloud/CloudLoginAlpha11Test.kt")
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text('''package com.framebynavin.app.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudLoginAlpha11Test {
    @Test
    fun missingLifecycleRpc_isClassifiedAsSafetyServiceUnavailable() {
        assertTrue(isCloudSafetyServiceUnavailable(CloudHttpException(404, "not found")))
        assertTrue(isCloudSafetyServiceUnavailable(CloudHttpException(400, "PGRST202 function missing")))
    }

    @Test
    fun authAndOtherServerErrors_areNotMisclassified() {
        assertFalse(isCloudSafetyServiceUnavailable(CloudHttpException(401, "unauthorized")))
        assertFalse(isCloudSafetyServiceUnavailable(CloudHttpException(500, "server error")))
        assertFalse(isCloudSafetyServiceUnavailable(IllegalStateException("offline")))
    }
}
''')
