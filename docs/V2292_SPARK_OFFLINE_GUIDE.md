# V229.2 — Spark session-only offline conversation

The existing Spark Home portrait / suggestion sheet becomes the entry point for a small **local guided conversation**. No separate app tab, account, background worker, cloud inference, on-device AI model download, API key or new permission.

- Users can type short questions or choose chips: Next Step, My Ideas, Reminders.
- Pure `V2292SparkConversationPolicy` answers only from the *currently observable* CreatorTask / CreatorIdea lists. It never claims a task was saved/finished/published or that Android delivered a notification.
- Reply actions route to the existing Project, Idea Vault and Reminders screens. The Home integration rechecks that projects are active before navigation.
- Unknown questions receive an honest capability boundary: this is a deterministic offline guide, **not** an unrestricted AI chat.
- Up to 12 turns exist in Compose memory while the sheet is open. No chat transcript is written to preferences, exports, analytics, backups or logs. Dismissing the sheet discards the exchange. Up to 240 characters per input.
- The original Spark suggestion and "Not now" dismissal behavior stay intact. Existing guide selection and companion off switch remain authoritative.

Build259 is stacked on PR #169 Build258 and checks same package signer, unit policy cases, real Compose Android 11/15 panel and routing, plus all existing Personal/follow-ups/Workspace/creator-nudge suites. Physical-device accessibility, font scaling and keyboard overlap need manual QA. No stable merge.
