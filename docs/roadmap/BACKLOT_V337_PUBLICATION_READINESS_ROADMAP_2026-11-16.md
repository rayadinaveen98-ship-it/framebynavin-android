# Backlot — V337 owner-stable to public Play publication
**Audited:** 2026-11-16 (India).  
**Decision:** **NO-GO for Google Play production today.** Stable for the founder's existing personal/sideload installation **yes**; full release validation **not yet**; closed/internal testing preparation **feasible after gate review**.

## Reference source of truth
- Promoted owner-stable: `stable/backlot-v337-p0-owner-accepted-20261116`, exact commit `815ba69775f75b6e5f25bb838c2c17b4f09c203e`. Frozen old V337 `678502f4626150ea1945b81eed1593a362377454` and main remain unchanged.
- Verified P0 source: [PR #389](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/389), [P0 Android 15 + JVM run #43855917050](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43855917050), success.
- Verified six-stage QA, *no product code changes*: [PR #391](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/391), [run #43909957313](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43909957313), 3/3 jobs success, 17/17 selected Android 15 instrumented tests passed.
- Exact owner APK SHA-256 `94fc7c0c9f9718378456571f439eea759824fc61ac990d43ff0b2b191a3309b2`, versionCode `337`, targetSdk `36`, public preview signer `f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0`. **Never upload this APK to Play as a production artifact**.
- Internal branch is a traceability snapshot, **not protected**, **not merged to main**, and **not a published release**. A branch named stable alone supplies no production signing, rollback or Play distribution.

## Release-readiness scorecard
| Department / gate | Evidence and truthful state | Severity | Decision |
| --- | --- | --- | --- |
| Core architecture/reliability | P0 10-minute actual Android 15 Snooze, missing-channel readiness, full JVM and all AndroidTest *compile* green | Strength | Accept CI at exact SHA; **physical/OEM not certified** |
| QA functional regression | 17 selected Android 15 tests across onboarding, widgets, scenes, companion and accessibility; full JVM green | Strength | Sufficient for next physical QA, **not production pass** |
| Android API policy | `targetSdk=36` satisfies current Google Play new/update submission requirement from Aug 31, 2026 | PASS | https://support.google.com/googleplay/android-developer/answer/11926878 |
| Production signing / distribution | `personalStable` uses **public** checked-in prototype signer. `verifyPrivateReleaseSigning` explicitly fails without externally provisioned private release credentials. No privately signed V337 Play AAB verified | **P0 BLOCKER** | Choose signing strategy, protected upload key, App Signing, AAB, provenance |
| Existing owner data migration | Public preview cert differs from future private Play signer; Android refuses normal same-ID in-place upgrade with mismatched signer. App disables system backup; Settings backup/restore entry points intentionally removed | **P0 BLOCKER** | Founder choose separate Play package ID vs same ID + separately approved verified migration, preserving existing install |
| App account deletion | Android `V269DataPrivacyPolicy.DELETION_STATUS` states **full account deletion is not available**. RC17–24 are offline/local staging proofs; no production endpoint/public web request page evidenced | **P0 BLOCKER** if account creation remains | Build complete authenticated, request/receipt, durable/race-safe deletion backend + in-app/web UI, staged tests |
| Privacy and Data Safety | `RC11_PRIVACY_POLICY_OWNER_REVIEW_DRAFT.md` marked not for publication; live HTTPS policy URL and Play Data safety declarations not evidenced | **P0 BLOCKER** | Owner-reviewed live URL, in-app link, SDK/network/data-type audit, Play Console declarations |
| Permissions/Play eligibility | Manifest uses `USE_FULL_SCREEN_INTENT`, `SCHEDULE_EXACT_ALARM`, foreground mediaPlayback, microphone/calendar. Full-screen auto-grant restricted to true alarm/call core; FGS requirements and exact-alarm fallback must be explained | **P0 POLICY RISK** | Eligibility + app behavior demonstration; Play Console forms, denied-grant fallbacks. https://support.google.com/googleplay/android-developer/answer/13392821 |
| Authentication/integrations | Google sign-in path exists; founder last reported "restricted due to exceed egress quota" when connecting. This error has **not been retested or fixed** here | **P1 functional risk** / P0 if required at launch | Verify current hosted quota, real test account login/logout/retry and OAuth client fingerprints; no live infrastructure changes without owner approval |
| UI/UX and accessible launch | Owner accepted P0 APK generally, but no complete screenshots/rubric for all six-stage features on multiple devices. Prior owner asked to simplify Settings, remove redundant scenes/Premiere, keep two themes, more icons | **P1 approval gate** | Reconcile newest approved requirements and decide V1 scope; verify touch targets, TalkBack, text scaling, design consistency |
| 21-day experience | Existing P0 reminders operational in emulator; V394 challenge visual **preview-only**, 21-day voice+background music parity **not implemented** | **Scope gap** | Not automatic publication blocker if explicitly deferred and no misleading feature claims; owner acceptance required |
| Performance/stability | No current documented V337 real low-end device, power/reboot, high-density alarm and battery matrix; no crash/ANR quality numbers for public cohort | **P1 functional gate** | Real phone/tablet + Android 11/14/15/16 targeted QA, 24–48h routine-use observation, failures documented |
| Play operations / store | No verified Play Console listing, current public privacy/deletion URLs, selected audience, support contact, country distribution, app screenshots, Content Rating, staged rollout plan or owner upload-key readiness | **P0 release ops blocker** | Complete Play preflight, internal/closed testing; submit only with explicit approval |
| Developer account access | Whether Play Console account qualifies as **new personal account created after Nov 13 2023** not known | **Conditional blocker** | If yes, minimum 12 testers opted in continuously 14 days, then production-access review. https://support.google.com/googleplay/android-developer/answer/14151465 |

## Exact product decision: stable !== published
1. **APPROVED/IMPLEMENTED:** Freeze 337 as **owner personal stable**, not Android production. Keep the exact tested APK and old 337 reference. No reinstall or app data clear.
2. **UNVERIFIED:** Physical version compatibility and complete six-stage user flow are not fully signed off just because 17 emulator tests passed.
3. **NOT APPROVED / NOT IMPLEMENTED:** Play private signing, complete deletion, public legal/Play listing and final UI scope are not complete, so **NO production publication** today.

## Execution roadmap — release-focused and staged

### R0 — Stable reference and evidence (DONE)
- Record source commit `815ba697`, owner artifact hash and certificate; create owner-stable branch at exact commit; retain previous reference.
- Preserve QA logs, P0 and six-stage tests. Document owner smoke acceptance honestly.
- No source edits, main merge, Play deployment, signing-key change or device mutation.

### R1 — Freeze minimum public V1 product (NEXT: founder review)
Product/UX/security decide one coherent launch promise: "offline-first Creator + Personal reminders and planning, with optional account" vs overloaded collection of unfinished systems.
- Inventory **every visible** Account, Settings, Reminder Scene, Voice, Companion, widget, first run, 21-day screen against founder's newest approved requirements.
- Distinguish P0 safety/function from P1 quality and **deferred** advanced effects. Do not represent experimental 21-day Premiere "DAY 08" preview as actual challenge progress.
- Either simplify unapproved Settings/Premiere UI in a separately scoped/approved next build or explicitly approve retaining the current experience for the MVP. If UI or code is changed, use **versionCode >337**; rerun full release matrix and new physical sign-off.
**Exit:** approved MVP feature list + acceptance criteria and design freeze, not another unbounded backlog.

### R2 — Non-negotiable security/privacy/signing engineering (BLOCKING)
- Choose and document existing preview-user migration strategy. Safest for preserving preview app is often **a separate Play package ID**, but this changes OAuth/links/provider identity; no strategy selected yet.
- Provision owner-held private upload signer **outside GitHub/repository**, enable Play App Signing and verify *release bundle* through private test pipeline. Keep preview signing unchanged for installed owner version.
- Build and stage authenticated account deletion through real Auth + Storage + Creator profile/snapshots, race-safe write fencing and idempotent queue; confirm sibling user isolation, upload-in-flight, resumption, revocation and receipts. No destructive QA on live data. Consider local-only release removing *all account creation* only through a new explicitly approved scope; Google Play deletion rules then must be reevaluated.
- Publish legal-reviewed privacy policy at public HTTPS URL, include in app, verify with Data Safety declarations and processor inventory.
- Show Play permissions eligibility and safe denied-permission behavior; prepare FGS + full-screen and any exact-alarm declarations with real behavior video.
**Exit:** reproducible secure implementation and legal/policy sign-off, not merely local mock tests.

### R3 — Real-device acceptance and release CI (BLOCKING)
- Disposable in-place upgrade from previous preview for **same-preview-signer testing only**; test synced/unsynced records, Spaces, challenges, audio, project assets, widgets/empty/error.
- Founder and additional testers: normal and locked reminders, exact alarms + denied permissions, cold start, reboot, deep idle, TTS/music, repeated notifications, 21-day real 10-min Snooze, correct Done receipts, app crash/ANR/performance, accessibility (TalkBack + 200% font + reduced motion), phone and tablet.
- Verify actual Google sign-in with two test accounts and recovery behavior. Fail launch if sign-in is promised but consistently unusable.
- Run **all required** JVM, Android instrumentation, lint, Android 16 targeting, network/security checks; build and inspect signed **AAB** on exact frozen release SHA; release certificate separate from public prototype.
**Exit:** zero launch-blocking functional defects with owner evidence and signed reproducible bundle.

### R4 — Play Console / testing operations
- Secure developer account access and support identity; package, signing, privacy and deletion URLs; app description, authentic screenshots, icon, feature graphic, content rating, countries and audience.
- Complete Data Safety, app access instructions, requested permissions, foreground service and full-screen intent declarations.
- If applicable, **12 opted-in testers for 14 continuous days** and production-access application; don't assume an account is exempt.
- Begin Google Play **internal testing** first when mandatory engineering/policy gates are complete, then **closed test**, then limited production staged rollout only after founder go/no-go.
**Exit:** owner explicitly approves a Play AAB submission; store's approval/review is not guaranteed.

### R5 — Launch and commercial stewardship (AFTER R4)
- Start free/low-cost MVP unless founder approves monetization. No paid service, subscription, analytics SDK or ad network added without cost/privacy scope.
- Establish support triage, crash monitoring with privacy disclosures, rollback strategy based on higher-version hotfix (not unsafe APK downgrade), release notes and analytics retention targets.
- First launch metrics: activation, successfully delivered reminder rates, first challenge completed, 7/30-day retention, crash-free sessions, project created; no fabricated claims.

## Ordered priorities / sequencing
**P0 stop-ship:** Account deletion if account creation exists; complete privacy URLs/Data Safety; private release signing + safe preview-to-Play migration decision; FSI/FGS policy declarations/eligibility; functional sign-in if advertised; signed AAB and Play setup.
**P1 pre-public quality:** Real-world device/accessibility/performance, user-flow consistency, newest Settings direction, companion and scene polish, correct three widgets.
**P2 may defer with honest listing:** fully live Challenge Premiere, 21-day music parity, extra icons/themes, advanced companion animations and nice-to-have personalization. Defer only with founder's MVP scope approval.
**Stage decision:** **R0 done; R1 next; R2 and R3 required before even calling the app Play-release-ready.** Do not mistake source-green owner preview for publication approval.

## References
**GitHub:** [Owner stable source](https://github.com/rayadinaveen98-ship-it/framebynavin-android/tree/stable/backlot-v337-p0-owner-accepted-20261116); [PR #389](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/389); [PR #391](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/391); [RC16 signing gate](../release/RC16_PRIVATE_PLAY_UPLOAD_SIGNING.md); [RC17 deletion prototype](../release/RC17_ACCOUNT_DELETION_ENGINEERING_GATE.md); [RC24 live-writer gap](../release/RC24_REAL_CLOUD_WRITER_FENCE_AUDIT.md); [V269 privacy page source](../../app/src/main/java/com/framebynavin/app/ui/V269DataPrivacyActivity.kt).
**Google:** [2026 target API](https://support.google.com/googleplay/android-developer/answer/11926878); [account deletion + user data](https://support.google.com/googleplay/android-developer/answer/10144311); [new personal developer closed testing](https://support.google.com/googleplay/android-developer/answer/14151465); [FSI/FGS declaration](https://support.google.com/googleplay/android-developer/answer/13392821).

## Founder authority
Stable owner preview decision executed. **No Play submission, production AAB, live deletion migration, service keys, changed package ID, protected branch modification, user-data removal, merge or public release is authorized by this document.** Every major release-design decision and any paid-service use require separate approval.
