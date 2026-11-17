# Backlot UX09-01D — signed hosted Lab QA accounts: fail-closed acceptance gate

**Date:** 2026-11-17 · **STATUS: NO-NETWORK HARNESS CI PASSED (9/9); HOSTED SIGNED-USER TESTS NOT YET RUN.**

## Baselines and last verified evidence
- Original V337 accepted P0: `815ba69775f75b6e5f25bb838c2c17b4f09c203e` (protected).
- Owner-tested isolated Lab V337: `4ba1263124d118d3917f988228aec2248068466f` (protected).
- Backlot Lab `vasleafrmsovdbdlrcsw` only; versioned UX09-01 migration installed once as `20261117141908`.
- Prior hosted public negative HTTP CI [run #44104089711](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44104089711) passed: private schema header HTTP 406/PGRST106; anonymous pull HTTP 401/42501; invalid Bearer refused.
- Read-only connected Supabase preflight on 17 Nov counted **zero** designated UX09 disposable Auth identities with email matching `ux09-qa-%@example.invalid`. No accounts were added by this stage.
- Founder's Dashboard screenshot and confirmation: exposed schemas only `public`, `graphql_public`; two new functions selected; internal `backlot_sync_internal` not exposed. Must preserve.

## Implemented source-only artifacts
1. `tools/qa/backlot_ux09_01d_hosted_signed_readonly.py` is an allowlisted Lab-only HTTP harness. Requires **two already-provisioned** Auth accounts, distinct passwords, and an exact environment-mode acknowledgement; refuses all other target hosts, email identities or API paths. Only API routes: password sign-in, signed user identity read, **read-only** Project/Idea delta pull, private-schema deny, Auth session sign-out.
2. `tools/qa/test_backlot_ux09_01d_hosted_signed_readonly.py` exercises identity, account-batch fencing, private schema, emptiness, network allowlist, simulated signed-user success and session-cleanup failure without network calls.
3. `.github/workflows/backlot-ux09-01d-signed-qa-safety.yml` runs only no-network tests. It must not receive hosted passwords, admin/service-role API keys, or the original production credentials.

## Executed harness CI evidence (NO hosted credentials)
- [GitHub Actions run #44106940351](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44106940351): **completed / success**. Python compilation and **nine** no-network unit tests passed on source PR #434, exact tested commit `62be158cbbb0247093628e34d2bad08de36fc66c` (including fail-closed handling of null/non-string Auth emails). Earlier run [#44106327263](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/44106327263) also passed.
- Tests cover exact Lab key/hostname, QA batch identity fencing, reject anonymous or mismatched users, refuse nonempty sync pulls, private-schema denial, prevent signup/admin/delete/app-write routes, require explicit mode, simulated signed two-user flow and logout failure.
- **No real hosted JWT or Auth sign-in was used in this CI**. It must not be described as hosted two-account acceptance.
- Fresh read-only Supabase preflight after CI: zero designated QA Auth users, zero rows in all four new sync tables, exactly one installed UX09 migration. No Lab user data modified by this stage.

## Bounded hosted identities and cleanup plan — NOT EXECUTED

**Do not create accounts until the account-deletion/cleanup mechanism is independently verified and the owner intentionally begins this QA operation.** The connected Supabase tools expose no Auth admin user-create/delete action; writing to `auth.users` directly to fabricate JWTs or forcing email confirmations off are NOT acceptable shortcuts.

Proposed safe protocol:
1. Confirm a reversible, owner-controlled **Backlot Lab Authentication > Users** workflow exists to create confirmed synthetic email/password users and delete them by their exact IDs. Do not alter production Google OAuth, Auth providers or general email confirmation.
2. From the Lab admin Dashboard create exactly **two** dedicated users with same test batch suffix, names `ux09-qa-a-<8-16 lowercase hex>@example.invalid` and `ux09-qa-b-<same suffix>@example.invalid`. This reserved domain is deliberately non-routable. If the provider rejects it or cannot create confirmed accounts without sending email, **STOP** and review another safe supported admin flow. Never use the founder's Google account, real email, preexisting QA account or personal information.
3. Use distinct disposable strong passwords, entered only as local temporary environment variables in a trusted runner. Never send secrets in chat, place them in a PR, GitHub issue, workflow logs or checked-in files, or use service-role keys in Android.
4. Before running, check exact project hostname and cloud config in source. Execute only the reviewed harness; validate identity A/B, read-only empty pull, and that signed private-schema access still returns 404/406. It must stop on unexpected existing data or exposure. Password sign-in creates temporary **Auth session state**; this is not a strictly read-only Auth operation.
5. Harness attempts to sign out both sessions in `finally`; if sign-out cannot be confirmed, mark QA **FAILED** and manually revoke sessions. It creates no Creator Project/Idea rows or snapshots.
6. Owner deletes only those two exact disposable Auth users via authenticated Dashboard admin controls, then a separate read-only inspection verifies **zero** matching QA identities, zero associated sync rows, and no accidental changes to other Lab records. Deleting Auth users may not invalidate already issued JWTs instantly; wait for expiry or explicitly revoke sessions.
7. If removal is blocked, stop all further hosted testing and document leftover IDs privately (not in public PR).
8. Real signed-user CAS writes, revision races, tombstones, replay/idempotency and cross-account data isolation are a **separate UX09-01E gate**, with a guaranteed cleanup path and its own exact approval. A successful 01D pull does not certify them.

## Physical Windows QA execution — BLOCKED at test-account sign-in (2026-11-17)
- Owner ran the Windows PowerShell native read-only runner from the supplied ZIP. It reached the target Auth endpoint, but stopped at `QA A sign-in: HTTP 400`; no A/B signed API acceptance, Project/Idea pull or private-schema signed check executed. Script reported `NOT CERTIFIED`. No new sync data was written.
- Supabase Lab `auth_logs` at `2026-11-17T15:20:54Z` reported `error_code=invalid_credentials`, HTTP 400 for the attempted password sign-in. Two earlier requests at ~15:09Z reported `email_address_invalid`; those are separate and do **not** prove the current Windows run failed for the same reason.
- Non-secret connected read-only `auth.users` metadata for *both* exact QA identities: email confirmed, encrypted password configured, provider `email`, not anonymous, not SSO, not banned, not soft-deleted. No password or hash was retrieved or changed.
- This evidence indicates a mismatch between credentials entered and expected by Auth **or** another credential-validation issue; neither the precise password nor the mechanism can be determined without a subsequent controlled successful sign-in.
- The screenshot shows a visually different count of masked characters for A and B, even though the owner intended identical passwords. This is a possible input typo, not a verified root cause.
- **Recommended single safe retry:** re-open the existing Windows runner, carefully enter the original disposable passwords for both exact accounts, avoiding leading/trailing spaces. Do not use or share the owner's Google password. If same `invalid_credentials` repeats, STOP and review disposable-account reset/replacement **only** after a separate cleanup check; do not disable Auth security, change original account, or write directly into `auth.users`.
- The intentionally reserved `@example.invalid` test domain may also require separate compatibility investigation; do not replace it with real-person email addresses without explicit reviewed plan.
- **NO SECURITY ACCEPTANCE:** previous anonymous API denial and database checks remain valid; hosted signed-JWT acceptance is still blocked. Existing test accounts should not be deleted before review.

## Certification / risk matrix
| Case | Acceptance | Current |
|---|---|---|
| D01 | Exact Lab-only hostname / publishable key | Source guard verified by 9 passing no-network tests |
| D02 | QA email domain, a/b batch, distinct passwords | Source guard verified by 9 passing no-network tests |
| D03 | No network/API mutation outside allowlist | Source guard verified by 9 passing no-network tests |
| D04 | Non-anonymous, signed valid A/B Auth JWTs verified | NOT RUN hosted |
| D05 | A/B distinct Auth IDs and signed empty deltas | NOT RUN hosted |
| D06 | A/B forbidden `backlot_sync_internal` HTTP access | NOT RUN hosted |
| D07 | A/B Auth sessions logout reported and verified | NOT RUN hosted |
| D08 | Admin QA identity removal + zero residuals | NOT RUN |
| D09 | Live CAS create/update/duplicate/tombstone and RLS | Separate gate NOT RUN |
| D10 | Android outbox, 2-device sync, Personal/Backup/Restore/Delete | Not implemented |

**No Android code, app database migrations, hosted SQL or Auth settings mutation, new paid resource, merge, owner-data deletion, APK or release in this stage. Keep draft.**
