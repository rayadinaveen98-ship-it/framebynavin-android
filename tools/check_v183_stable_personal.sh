#!/usr/bin/env bash
set -euo pipefail
# Personal release: non-debuggable Android APK signed with the SAME public prototype
# certificate as existing Backlot debug previews. This is NOT production signing.
grep -Fq 'versionCode = 183' app/build.gradle.kts
grep -Fq 'versionName = "2.3.3-stable"' app/build.gradle.kts
grep -Fq 'applicationId = "com.framebynavin.app"' app/build.gradle.kts
grep -Fq 'create("personalStable") {' app/build.gradle.kts
grep -Fq 'initWith(getByName("release"))' app/build.gradle.kts
grep -Fq 'isDebuggable = false' app/build.gradle.kts
grep -Fq 'signingConfig = signingConfigs.getByName("prototypeStable")' app/build.gradle.kts
grep -Fq 'matchingFallbacks += listOf("release")' app/build.gradle.kts
test -s dev-signing/framebynavin-public-debug.jks
test -s app/src/personalStable/java/com/framebynavin/app/CreatorAppCheck.kt
grep -Fq 'DebugAppCheckProviderFactory.getInstance()' app/src/personalStable/java/com/framebynavin/app/CreatorAppCheck.kt
grep -Fq 'PlayIntegrityAppCheckProviderFactory.getInstance()' app/src/release/java/com/framebynavin/app/CreatorAppCheck.kt
test -f docs/qa/V183_STABLE_PERSONAL_RELEASE.md

# Avoid public-repo disclosure of a private production key.
! grep -R --include='*.yml' --include='*.md' --include='*.kt' --include='*.sh' -n 'FRAMEBYNAVIN_RELEASE_STORE_PASSWORD=.*' .github tools docs app

# Historic safety guards must keep accepting the stable release.
grep -Fq '182|183)' tools/check_v161_quick_actions_hub_capture.sh
grep -Fq '2.3.3-stable' tools/check_v176_release_acceptance.sh
echo "V183 stable-personal identity, signing and safety gates passed"
