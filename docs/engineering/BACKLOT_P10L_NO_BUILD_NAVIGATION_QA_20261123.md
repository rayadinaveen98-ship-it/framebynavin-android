# Backlot P10L — no-build live Android navigation after P10J false negative

## Verified root-cause evidence
- P10J run [45182330346](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45182330346) compiled app+AndroidTest successfully and timed out in the one test waiting for `v219-reminder-home-entry` Compose test tag.
- P10K [run 45197511085](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/45197511085) **successfully captured live screenshots before test teardown** using the **same original P10J APKs**. Live images at 8, 18 and 32 seconds show real Creator Home, greeting, **REMINDER HUB**, My Spaces and bottom navigation. The UIAutomator hierarchy independently confirms the rendered **REMINDER HUB** text. Thus the previous claim that setup could not reach Creator Home is not supported; the root problem is the Android Compose test's internal tag selector not finding the visible Home card. A root UI failure or crash is not established.
- P10K diagnostic runner force-stopped the app after evidence collection, so its unchanged JUnit test stream reports interruption; this is **not** a product crash or a passing first-run test.

## P10L targeted no-build shell UI gate
- Use the unchanged P10J debug & test APKs from run 45182330346, with **no Gradle or source changes**.
- Re-execute real initial Account Choice -> local-only Companion -> five-page Creator setup via existing test, then while JUnit is waiting for its unreachable internal tag, use Android OS tap events and hierarchy assertions to check Creator Home -> Reminder Hub -> Personal -> Creator.
- Require real visible user text, screenshot and no onboarding screen at each step. This is independent of Compose's internal `testTag` lookup.
- Evidence is Android 15 emulator-only. This doesn't certify all Google auth/cloud, OS permissions, motion, audio/alarm delivery or physical devices.
- If successful, separately correct P10J Compose test locator with visible text or unmerged semantics and only then re-run its focused revised test if necessary. No owner APK or stable release yet.
- Scope remains isolated QA branch, no main merge, no hosted data, no credentials, no full suite or production changes.

**Stage entry:** no-build shell UI verification requested. Result pending until actual job evidence.
