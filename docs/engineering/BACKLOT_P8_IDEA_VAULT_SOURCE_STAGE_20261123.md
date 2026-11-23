# Backlot Point 8 — Idea Vault Presentation Source Stage

**Owner approval:** "yeah ok bro approved go ahead" after Point 7a Reminder Hub tab-order correction, authorizing next scheduled Point 8. Existing Vault must remain sole idea source of truth: no duplicate inbox, destructive migration, cloud service, new analytics scoring or expanded content-workspace features.

**Status:** SOURCE-STAGED CANDIDATE / UNCOMPILED / UNTESTED / NOT DEVICE ACCEPTED

## Verified repository
- repo: rayadinaveen98-ship-it/framebynavin-android
- cumulative branch: feature/backlot-v337-point08-idea-vault-source-after-p7a-20261123
- Point 7a parent: a7c1b084c93edbb6832d707b06fefb50679c0b55
- accepted stable V337: 815ba69775f75b6e5f25bb838c2c17b4f09c203e, unchanged
- main: unchanged; no merge, CI, Gradle, tests, APK or release.

## Source audit before changes
- Existing V09IdeaVaultScreen offers real active and Recently Deleted ideas; search title/topic/notes/transcript; Inbox/Ready/Converted status chips and detailed status/category/capture filters.
- Existing source already uses the YouTubeOpportunityEngine's actual 24h report matches, while no matched idea is created automatically.
- Edit and New Idea use the existing V09IdeaEditor and root-owned V09NewIdeaCleanEditor; voice original recording and drafts retain their historical stores.
- Delete prompts reversible Recently Deleted move; restore acts on existing idea ID. Archive and Converted status are different from deletion.
- V09ConvertIdeaDialog remains the existing durable idea-to-project path with retained original idea, parent project link and destination routing.
- Current LazyColumn uses keyed idea IDs and current query/filter/sort order; no need for another store or infinite-scroll rewrite.

## Staged changes
1. New P8IdeaVaultPresentation.kt contains pure P8IdeaVaultSummary from the real CreatorIdea records: Inbox, Exploring (Worth Exploring + Researching), Ready, Converted, Archived. No fake scores, trending labels, AI interpretations or analytics calls.
2. Compact editorial overview appears only on the normal, unfiltered nonempty vault: "Capture it. Shape it. Make it." Status counts are based on real saved ideas; it disappears while searching/filtering or reviewing deleted items to prioritize scanning.
3. Replace unclear empty-state text with three honest cases: no saved ideas, no search/filter matches, or no deleted ideas; provide existing New Idea, clear search/filters and return to active ideas actions. No data changes happen on empty-state navigation.
4. Fix the deleted-view search edge case: deleted items that exist but don't match search show "no results" instead of falsely "no deleted ideas".
5. Keep category, status, capture-type and opportunity labels, now wrapping via a FlowRow on compact devices instead of squeezing a single Row. Opportunity match label is only shown for already matched IDs from the existing engine.
6. Idea title and topic preserve the FULL text in TalkBack semantics even if visually truncated. Platform/format metadata has a separate line so project-conversion and archive actions no longer compete for horizontal space. Actions keep the same callbacks and item IDs.
7. Fix existing Compose snapshot invalidation for updated idea lists: derive opportunity matches, status/voice counts and filtered/sorted list with the updated `ideas` and `deletedIdeas` as dependencies. The match engine, sorting priorities and persistent records are unchanged; no duplicate opportunity ideas are created.
8. Existing keyed lazy list, filter behavior, active/deleted states, add/editor, voice recording, project conversion, source records, analytics, archives and recovery remain unchanged.
9. P8IdeaVaultPresentationTest.kt source staged for the later consolidated QA campaign; no execution.

## Deferred QA / regression acceptance
- Kotlin/Compose compilation and FlowRow/semantics API compatibility; stage does not claim compile readiness.
- Fresh empty vault: CTA opens existing New Idea, save/dismiss retains current durable transaction semantics.
- 50–100 ideas, all six statuses and multiple categories, long emoji/Telugu/English titles and font scale 1.0–1.5: no clipping, no crash, suitable performance.
- Filter chips and detailed filters, keyword search, no results clear action, deleted search no-results, recently deleted restore, archive distinct from deletion.
- Voice original recording playback and interrupted recording unchanged; no accidental re-capture on navigation.
- Real opportunity match label only if source report has a matched idea; no fabricated matches.
- Convert an existing idea to project exactly once, durable ID/link, correct Studio routing, original idea retained and status converted.
- Accessibility: selected chip state, keyboard/TalkBack full title, unambiguous button names, contrast and tap sizes.
- Account/Creator Home/Personal/Reminders/Spaces/guided tour/nav remain stable from staged P1–P7a; no onboarding re-entry.
- Physical APK certification and all targeted tests ONLY after founder's explicit consolidated QA gate.

**Point 9 — Settings and Appearance cleanup — not started.**
