#!/usr/bin/env python3
"""Fail-closed RC8c source gate. Runs only in the canonical GitHub Actions checkout."""
import hashlib, lzma, os, pathlib, subprocess
BASE='7e6913321726f2221054cd03b3e4df2483e4a7cb'
P='foundation/v183/rc8c-client.patch.xz'
VISIBILITY='foundation/v183/rc8c-visibility.patch'
EXPECTED=set('''app/build.gradle.kts
app/src/main/java/com/framebynavin/app/cloud/CloudApiClient.kt
app/src/main/java/com/framebynavin/app/cloud/CloudDeletionRecovery.kt
app/src/main/java/com/framebynavin/app/cloud/CloudLocalStore.kt
app/src/main/java/com/framebynavin/app/cloud/CloudModels.kt
app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt
app/src/main/java/com/framebynavin/app/cloud/CloudSyncManager.kt
app/src/test/java/com/framebynavin/app/cloud/CloudDeletionRecoveryTest.kt
app/src/androidTest/java/com/framebynavin/app/cloud/CloudDeletionJournalV183Test.kt'''.splitlines())
def git(*args):return subprocess.check_output(['git',*args],text=True).strip()
def sha(data):return hashlib.sha256(data).hexdigest()
def blob(data):return hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
def text(path):return pathlib.Path(path).read_text()
assert os.environ['GITHUB_REPOSITORY']=='rayadinaveen98-ship-it/framebynavin-android'
assert os.environ['GITHUB_REF']=='refs/heads/fix/v1.8.3-server-lifecycle-client'
assert git('rev-parse','HEAD')==os.environ['GITHUB_SHA']
subprocess.run(['git','merge-base','--is-ancestor',BASE,'HEAD'],check=True)
stage={P,VISIBILITY,'foundation/v183/rc8c-client-contract.md','foundation/v183/rc8c-verify-source.py','.github/workflows/v183-server-lifecycle-client.yml'}
actual=set(git('diff','--name-only',BASE,'HEAD').splitlines())
assert actual==stage,(actual,stage)
subprocess.run(['git','diff','--exit-code',BASE,'HEAD','--','app'],check=True)
packed=pathlib.Path(P).read_bytes()
assert blob(packed)=='f3d11cdb969a15626407b168ee8e491e5699ecfc'
assert sha(packed)=='8322e1c6f55d67b861efd3e651906ac59696fe7c0df9dcae74e77196e88c1aa1'
patch=lzma.decompress(packed)
assert sha(patch)=='43bdf8ecb5ec7f836ad5f18e88537c611855d9faeeceecad646a80754a860849'
pathlib.Path('/tmp/fbn183-rc8c.patch').write_bytes(patch)
subprocess.run(['git','apply','--check','/tmp/fbn183-rc8c.patch'],check=True)
subprocess.run(['git','apply','/tmp/fbn183-rc8c.patch'],check=True)
# The original candidate compiled with a public API exposing an internal value type.
# Apply exactly the reviewed one-line visibility correction, without changing behavior.
visibility=pathlib.Path(VISIBILITY).read_bytes()
assert blob(visibility)=='6dda6392f46a47bcde8584f60ab75ce0d0b5bed3'
assert sha(visibility)=='ce77c76891e078625c4ceec777f889ce65369960b5739c656ae61dd4072afddb'
assert visibility.count(b'-internal data class CloudLifecycleState')==1
assert visibility.count(b'+data class CloudLifecycleState')==1
subprocess.run(['git','apply','--check',VISIBILITY],check=True)
subprocess.run(['git','apply',VISIBILITY],check=True)
subprocess.run(['git','diff','--check'],check=True)
actual=set(git('diff','--name-only').splitlines())|set(git('ls-files','--others','--exclude-standard','app').splitlines())
assert actual==EXPECTED,(actual,EXPECTED)
assert not any(pathlib.Path(p).is_symlink() for p in EXPECTED)
gradle=text('app/build.gradle.kts')
assert 'versionCode = 73' in gradle and 'versionName = "1.8.3-foundation-rc8c"' in gradle
assert 'const val SCHEMA_VERSION = 5' in text('app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt')
api=text('app/src/main/java/com/framebynavin/app/cloud/CloudApiClient.kt')
assert api.count('writeEpoch = writeEpoch,')==4
assert 'x-creator-write-epoch' in api
assert 'creator_delete_owned_data' in api and 'creator_resume_owned_data' in api
assert '"DELETE", "/rest/v1/creator_backups' not in api
recovery=text('app/src/main/java/com/framebynavin/app/cloud/CloudDeletionRecovery.kt')
assert 'data class CloudLifecycleState(val phase: String, val generation: Long)' in recovery
assert 'internal data class CloudLifecycleState' not in recovery
assert 'journal.pendingGeneration()' in recovery
assert 'if (pending == null) journal.begin(userId, expected)' in recovery
assert 'val deleted = remote.deleteOwnedData(userId, expected)' in recovery
assert 'if (remote.hasOwnedData(userId)) throw CloudDeletionVerificationFailed()' in recovery
assert recovery.count('journal.clear(userId)')==2 # already-deleted and newly-deleted paths
store=text('app/src/main/java/com/framebynavin/app/cloud/CloudLocalStore.kt')
assert 'pending_cloud_deletion_v183' in store and 'cloud_lifecycle_v183' in store
assert 'fun requireWritable(userId: String)' in store
manager=text('app/src/main/java/com/framebynavin/app/cloud/CloudSyncManager.kt')
assert 'CloudDeletionRecovery(local, remote)' in manager
assert 'private suspend fun requireActiveLifecycle' in manager
assert 'suspend fun resumeCloudData()' in manager and manager.count('api.resumeCloudData(')==1
assert 'api.hasCloudData(session)' in manager
subprocess.run(['git','add','--',*sorted(EXPECTED)],check=True)
subprocess.run(['git','diff','--cached','--check'],check=True)
pathlib.Path('foundation/verification').mkdir(parents=True,exist_ok=True)
pathlib.Path('foundation/verification/rc8c-payload.sha256').write_text(sha(packed)+'  rc8c-client.patch.xz\n'+sha(patch)+'  rc8c-client.patch\n'+sha(visibility)+'  rc8c-visibility.patch\n')
print('Exact RC8c source, visibility correction, nine-file scope, version, and lifecycle contracts verified')
