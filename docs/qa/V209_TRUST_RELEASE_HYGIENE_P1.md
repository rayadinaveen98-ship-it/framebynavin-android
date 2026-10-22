# Backlot V209 P1 — Trust, release authority and repository hygiene

**Status:** non-functional maintenance candidate; not an Android release.
**Base:** V208 exact commit `05a082d73696892764e77fa12edf3df07985d582`
**Production source changes:** none.
**Android package/version:** remains `com.framebynavin.app`, versionCode 232,
`2.8.2-v208-setup-backup-preview`. This phase must not create or
redistribute an APK as a new version.

## Findings and actions

| Finding | Evidence on V208 | V209 P1 action | Follow-up |
| --- | --- | --- | --- |
| Stable/candidate ambiguity | README and current-stable docs mentioned V183/V184 while active development reached V208 | Identify V183 as published *personal stable* baseline and V208 as verified *personal preview*; link exact CI and keep V208 PR as draft | Formal owner-approved stable promotion with completed acceptance |
| Generated files tracked | About 5,930 files under `app/build/` (historical Git tree) | Ignore future builds and check ignore behavior in CI | Controlled untracking in a separate reviewed commit (NO history rewrite; NO data deletion) |
| Numerous CI workflows | About 211 historical workflow definitions | Do not delete existing CI; use one small V209-specific release-contract gate | Inventory active vs historic; later consolidate with branch/release safety |
| Signing migration | `personalStable` uses public prototype certificate | Lock identity for in-place existing user updates and clearly document limitations | Commercial/private production signer and an explicit migration strategy; never assume a different signer updates an installed package |
| Portable backup security | Portable voice media + signed-JSON checksum checks, but export is not encrypted | Keep the existing flow untouched and document real limitations | Threat model and design authenticated/encrypted, user-recoverable export format before public rollout |
| Export coverage | Settings discloses exclusion of Google sign-in, tokens, phone Calendar connection, theme, icon and launch sound | Keep exclusions explicit; no false 'complete device clone' claim | Inventory slideshow images, assets, cloud recordings, settings and recovery files in tests |
| Public release prerequisites | Exact-alarm/full-screen permissions, account management and OAuth/YouTube/AI permissions | No permission or consent changes in this phase | Separate Google Play / data deletion / privacy / signing review |

## Current backup contract (important limitations)

- Portable backup includes supported creator projects, ideas, workflow information,
  reminder/planning settings and readable app-owned Voice Idea recordings. A
  checksum can detect accidental corruption, but an unkeyed checksum is not
  proof that the file was created by the user and does not encrypt it.
- Backup preview stages the full archive and validates voice media hashes and
  idea references. Restore rechecks independently and retains recovery copies.
- The app explicitly excludes Google sign-in/tokens, phone Calendar linkage,
  theme, app icon and launch sound. The full inclusion of user-provided Home
  slideshow images and externally referenced assets remains a verification item;
  do not claim these are backed up without evidence.
- V209 P1 changes **neither** backup format nor cloud/restore execution. No
  app data is read, reset, exported or changed in this maintenance phase.

## CI acceptance

- Run `python3 tools/check_v209_trust_hygiene.py --base origin/feature/backlot-v208-restore-setup-backup-validation` on the V209 branch.
- Confirm no production source/resource, APK version, manifest permissions,
  database schema, runtime behavior or signing identity differs from V208.
- Confirm future `app/build/`, any `**/build/`, IDE files and new private
  keys are ignored by Git. Existing tracked build files are counted/warned,
  *not deleted*.
- Confirm new branch changes are restricted to documents, CI, the audit script
  and the root ignore rules.
- The V208 exact-commit CI and verified personal preview remain the Android
  correctness evidence; a green V209 P1 job is **not** a new emulator/device pass.

## Remaining gates before public release

1. Complete the backup content/integrity matrix, including slideshow artwork and
   voice media on real devices. Test restore only on a disposable install.
2. Verify cold/warm startup, ANR and frame timing on device and enable useful
   privacy-respecting diagnostics.
3. Conduct full-screen-intent/exact-alarm and account-deletion/privacy policy review.
4. Design a private commercial signer and safe opt-in data-migration flow;
   the public prototype key must not be reused commercially.
5. Consolidate repository-generated artifacts in a dedicated, reviewed
   non-destructive current-tree cleanup; do not force-push or rewrite history.
6. Rehearse an existing-user in-place upgrade and a new installation
   separately before an owner-approved stable promotion.

**Stop ship:** Any changed app bytecode/source, version, app ID, signing identity,
manifest permissions, saved data behavior or unsupported 'stable' claim.
