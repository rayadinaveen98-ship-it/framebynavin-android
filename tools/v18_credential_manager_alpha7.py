from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
CLOUD = ROOT / 'app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


build = BUILD.read_text()
build = replace_once(build, 'versionCode = 46', 'versionCode = 47', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-foundation-alpha6"',
    'versionName = "1.8.0-foundation-alpha7"',
    'versionName',
)

dep_anchor = '    implementation("com.google.android.gms:play-services-auth:21.6.0")\n'
if 'androidx.credentials:credentials:' not in build:
    deps = (
        '    implementation("androidx.credentials:credentials:1.6.0")\n'
        '    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")\n'
        '    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")\n'
    )
    build = replace_once(build, dep_anchor, dep_anchor + deps, 'credential dependencies')
BUILD.write_text(build)

cloud = CLOUD.read_text()
old_imports = '''import androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.compose.setContent\nimport androidx.activity.enableEdgeToEdge\nimport androidx.activity.result.contract.ActivityResultContracts\n'''
new_imports = '''import android.content.MutableContextWrapper\nimport androidx.activity.compose.setContent\nimport androidx.activity.enableEdgeToEdge\nimport androidx.credentials.ClearCredentialStateRequest\nimport androidx.credentials.CredentialManager\nimport androidx.credentials.CustomCredential\nimport androidx.credentials.GetCredentialRequest\nimport androidx.credentials.exceptions.GetCredentialCancellationException\nimport androidx.credentials.exceptions.GetCredentialException\nimport androidx.credentials.exceptions.NoCredentialException\n'''
cloud = replace_once(cloud, old_imports, new_imports, 'activity/credential imports')

old_google_imports = '''import com.google.android.gms.auth.api.signin.GoogleSignIn\nimport com.google.android.gms.auth.api.signin.GoogleSignInOptions\nimport com.google.android.gms.common.api.ApiException\n'''
new_google_imports = '''import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption\nimport com.google.android.libraries.identity.googleid.GoogleIdTokenCredential\nimport com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException\n'''
cloud = replace_once(cloud, old_google_imports, new_google_imports, 'google identity imports')

manager_anchor = '    val manager = remember { CloudSyncManager(context.applicationContext) }\n'
manager_new = '''    val manager = remember { CloudSyncManager(context.applicationContext) }\n    val credentialManager = remember(context) { CredentialManager.create(context) }\n    val credentialContext = remember(context) { MutableContextWrapper(context) }\n'''
cloud = replace_once(cloud, manager_anchor, manager_new, 'credential manager setup')

old_google_block = '''    val googleOptions = remember {\n        if (CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) null\n        else GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)\n            .requestIdToken(CloudConfig.GOOGLE_WEB_CLIENT_ID)\n            .requestEmail()\n            .requestProfile()\n            .build()\n    }\n    val googleClient = remember(googleOptions) { googleOptions?.let { GoogleSignIn.getClient(context, it) } }\n    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->\n        val account = runCatching {\n            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)\n        }.getOrElse {\n            message = if (it is ApiException && it.statusCode == 12501) "Google sign-in cancelled" else "Google sign-in failed"\n            null\n        }\n        val idToken = account?.idToken\n        if (!idToken.isNullOrBlank()) runOperation { manager.completeGoogleSignIn(idToken) }\n        else if (account != null) message = "Google did not return an ID token"\n    }\n'''
new_google_block = '''    fun startGoogleSignIn() {\n        if (busy || CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return\n        busy = true\n        scope.launch {\n            try {\n                val googleOption = GetSignInWithGoogleOption.Builder(CloudConfig.GOOGLE_WEB_CLIENT_ID).build()\n                val request = GetCredentialRequest.Builder()\n                    .addCredentialOption(googleOption)\n                    .build()\n                val result = credentialManager.getCredential(\n                    context = credentialContext,\n                    request = request,\n                )\n                val credential = result.credential\n                val idToken = if (\n                    credential is CustomCredential &&\n                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL\n                ) {\n                    GoogleIdTokenCredential.createFrom(credential.data).idToken\n                } else {\n                    null\n                }\n\n                if (idToken.isNullOrBlank()) {\n                    message = "Google couldn't complete sign-in. Try again."\n                } else {\n                    val operation = manager.completeGoogleSignIn(idToken)\n                    message = when (operation) {\n                        is CloudOperationResult.Success -> operation.message\n                        is CloudOperationResult.Skipped -> operation.message\n                        is CloudOperationResult.Failure -> operation.message\n                    }\n                }\n            } catch (_: GetCredentialCancellationException) {\n                message = "Google sign-in cancelled"\n            } catch (_: NoCredentialException) {\n                message = "No Google account is available on this device."\n            } catch (_: GoogleIdTokenParsingException) {\n                message = "Google couldn't verify the sign-in response. Try again."\n            } catch (_: GetCredentialException) {\n                message = "Google sign-in failed. Try again."\n            } catch (_: Throwable) {\n                message = "Google sign-in failed. Try again."\n            } finally {\n                busy = false\n                reloadPoints()\n            }\n        }\n    }\n\n    fun signOut() {\n        if (busy) return\n        busy = true\n        scope.launch {\n            val operation = manager.signOut()\n            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }\n            message = when (operation) {\n                is CloudOperationResult.Success -> operation.message\n                is CloudOperationResult.Skipped -> operation.message\n                is CloudOperationResult.Failure -> operation.message\n            }\n            busy = false\n            reloadPoints()\n        }\n    }\n'''
cloud = replace_once(cloud, old_google_block, new_google_block, 'legacy google sign-in block')

cloud = replace_once(
    cloud,
    'onClick = { googleClient?.let { googleLauncher.launch(it.signInIntent) } },\n                        enabled = googleClient != null && !busy,',
    'onClick = ::startGoogleSignIn,\n                        enabled = CloudConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank() && !busy,',
    'google button',
)

cloud = replace_once(
    cloud,
    'CloudActionRow(Icons.Outlined.Logout, "Sign out", "Cloud backup turns off; phone data stays") { runOperation { manager.signOut() } }',
    'CloudActionRow(Icons.Outlined.Logout, "Sign out", "Cloud backup turns off; phone data stays") { signOut() }',
    'sign out action',
)

CLOUD.write_text(cloud)
print('Applied v1.8 Foundation Alpha7 Credential Manager migration')
