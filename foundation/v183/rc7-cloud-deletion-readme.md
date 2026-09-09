# RC7 cloud creator-data deletion recovery

Base: `b0f0776e9e140cced1f32f870b95429ac9dfaeb7` (verified RC6).

The reviewed local candidate adds an account-scoped durable deletion journal, explicit same-owner retry, server-empty verification across creator_backups/creator_devices/creator_profiles, and explicit abandonment that does not claim deletion completed. It blocks writes for an owner with an unfinished deletion and retains the marker across sign-out. Full Auth identity deletion and cross-device server-side deletion barriers remain separate work.

The candidate has local pure recovery smoke checks and isolated Android test sources. It has not passed Android CI and must not be promoted or installed as a release. No live creator data, account, or backend schema has been modified.

Patch SHA-256: `56bd7d744983b135d19f913fe531aba56af9436e594f26759b2aac8a0ff30cbb`.
Compressed patch SHA-256: `fb8726281bf2d5075ba067eff76039d00eb1dc56254bb65f2db42c876bcd9818`.
The exact patch is available in the conversation artifact. Use the immutable RC6 source and reverify the patch before staging the complete app source or running CI. Do not invent a successful RC7 build.
