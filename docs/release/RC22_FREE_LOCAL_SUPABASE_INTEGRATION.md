# Backlot RC22 — ₹0 local Supabase Auth, Database and Storage integration

**Decision:** 30 October 2026. Strictly zero incremental cloud project charges. The public GitHub repository uses standard GitHub-hosted Ubuntu Linux runners, which GitHub documents as free for public repos. The CLI launches disposable local Auth/Postgres/PostgREST/Storage Docker containers; no Supabase managed project is created or modified.

## Why no paid service is necessary

- Supabase Free includes two active project allowances. Existing Backlot and CineRelay are active, and an inactive Movie Newsroom project is present. We do not repurpose any real app's cloud project.
- Supabase documents its local CLI development stack as free and independent of hosted project quota.
- GitHub Actions standard hosted runners are free for public repositories. We verified `rayadinaveen98-ship-it/framebynavin-android` is public at the time of this task.
- This workflow uses no Supabase account connection/token; it runs `supabase init` in an isolated runner temp directory, then starts only Auth, Postgres, PostgREST, Storage and gateway dependencies.

Official references: https://supabase.com/docs/guides/local-development ; https://supabase.com/docs/guides/platform/billing-on-supabase ; https://docs.github.com/en/billing/concepts/product-billing/github-actions ; https://supabase.com/docs/reference/cli/supabase-start

## Actual integration being tested

- Two synthetic email/password users are created by the **local Auth admin HTTP API**, and separately signed in to obtain real local JWTs. Tokens are never copied to GitHub artifacts.
- A private bucket `backlot-rc22-ci` is created through the **real local Storage HTTP API**, not directly in storage SQL metadata.
- `fixture.sql` creates an independent deletion-fence table in a private local-only schema, plus a synthetic Creator table and Storage bucket-scoped RLS policies that check `auth.uid()` and a blocked-account predicate.
- The Python runner checks actual PostgREST creator insert/update, Storage uploads, upload with `x-upsert`, update/replacement, copy and move, with A denied after fencing and B unaffected.
- The local service-role privilege bypass is intentionally tested and documented, so a later production privileged adapter cannot be assumed safe without a specific write-fence check.
- All HTTP and SQL endpoints must resolve to `127.0.0.1` or `localhost` and fixed local ports. Redirects are rejected. SQL requires an explicit ephemeral opt-in session variable. Hosted projects are rejected.

## Interpretation and remaining work

- This is **REAL local Supabase Auth and Storage**, not the RC21 fake HTTP server. It is still **not** a hosted Supabase staging project, a production deletion service, or a safety proof for ongoing signed JWT refresh sessions or concurrent in-flight Storage object writes.
- The production bucket uses different policies that presently restrict ownership but not deletion state. We have **not** modified those policies. No production DB migration, user deletion, Google Play upload, personal device removal or main-branch merge is authorized.
- The local RLS function is a staging proof; production will need a carefully reviewed transactional writer/worker strategy, privileged account scoping, rate limits, long-running uploads, audit logging, Auth deletion semantics, a public web deletion page, and two-account staging with real user deletion before release.
- The workflow is intentionally restricted to a public repo and standard hosted runner. If its repository becomes private, check the spending/runner entitlement before re-running.

**Never deploy `fixture.sql` to a hosted Supabase database.** These synthetic identities and files exist only in the disposable GitHub runner.