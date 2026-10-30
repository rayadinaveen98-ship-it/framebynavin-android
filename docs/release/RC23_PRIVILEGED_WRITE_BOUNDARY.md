# Backlot RC23 — restrict privileged Voice Idea writes (₹0 local Auth and Storage testing)

**Scope:** Security prototype and genuine local Supabase API tests on standard free public GitHub Actions runners. NOT deployed, NOT a production Android feature or a complete deletion service. User's strictly ₹0-only instruction applies.

## Root cause after RC22

RC22 demonstrated against real local GoTrue Auth, PostgREST and Supabase Storage APIs that the `service_role` API key bypasses ordinary RLS and can upload a new object for an account marked as deleting. This is documented behavior and cannot be fixed merely by adding another RLS policy. The production Backlot private `creator-voice-ideas` policies also lack a deletion-state check and remain unchanged.

## RC23 defense-in-depth prototype

- `rc23-local/storage_write_boundary.py` adds `UserJwtStorageWrites`, a **write-only** server-side interface with no service-role mutation method.
- Before every write, a trusted identity callback calls the real Auth `/auth/v1/user` endpoint to verify the bearer JWT, derives the owner UUID server-side, refuses the service-role key, rejects cross-account object paths and unsafe paths or oversized requests.
- Its actual Storage mutation calls (upload, upsert, replacement, copy and move) use the *verified user's JWT* and publishable/anon API key. Real Supabase Storage RLS therefore enforces any account-level deletion lock.
- The test suite first runs RC22's 36 real local integration checks including its **intentional direct admin bypass**, then performs additional checks of the user-JWT-only write boundary against real local Auth/Storage.
- All endpoints are loopback-only and the database fixtures refuse remote execution. No hosted project or other external data/service is touched.

## Important limitations — DO NOT claim full security resolved

**This is a proof of a safe calling pattern, not a guaranteed global service-role write block.** Anyone holding the underlying service-role key can still call Supabase APIs directly, bypass this wrapper, and write to fenced accounts. Every real backend worker and service key holder must be inventoried and denied unguarded writes by architecture, key placement, code review and production service boundaries.

Also pending: actual production authenticated endpoint integration; server-side write-fence implementation for existing Creator snapshot RPC and Storage bucket; race-safe handling for in-flight uploads, signed/resumable object writes, sessions and token refresh; privileged delete-only worker and durable deletion receipts; end-to-end authenticated account deletion; web deletion-request page and legal privacy text; Play declarations; owner device acceptance; production signing.

RC23 deliberately does **not** create a paid project, deploy a backend/Edge Function, modify any connected Supabase data/policy, upload an APK, change Build269 or merge to `main`. Keep draft until all gates are satisfied.

### Primary documentation

- https://supabase.com/docs/guides/database/postgres/row-level-security
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/guides/storage/management/delete-objects
