# Backlot UX08A — Quick Actions / Progress / Rewards / Brief / Calendar protection matrix

**Date:** 2026-11-17. **Test status:** **0 EXECUTED**, this is a documentation-only source audit.
**Exact parent:** UX07F draft PR #416 `ad8f39ea57ed5ca11b2c359cf5abdc0938220138`.
**Protected stable:** V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`. Integration divergence 19 accepted P0 commits/22 paths still open.
**Scheduling:** Founder chose one consolidated UX01–UX10 build, tests and owner physical certification **after** source-staging all ten UX points; earlier source tests are authored but not executed.

| ID | Priority | Action/condition | Must prove after UX10 |
|---|---|---|---|
| A01 | P0 | Enter Quick Actions from actual Home floating control | Same original bottom-sheet navigation and dismissal; other overlays unchanged |
| A02 | P1 | New Project action | Existing wizard opens; no project/XP created until actual save |
| A03 | P1 | Quick Capture action | Original capture opens; cancellation writes no Idea Vault item or XP |
| A04 | P0 | Overdue +30 min | Original `publishLate` moves actual Creator project deadline, adjusts intended eligible alert safely; no fake Done |
| A05 | P0 | Overdue New time | Original date picker validation and `rescheduleDeadline` changes correct task once, rejects past time safely |
| A06 | P0 | Overdue Skip | Original `skipTask` updates exact project and cancels intended alert; no unrelated project changed |
| A07 | P1 | Daily Brief / Project focus / open calendar | Original overlay, exact project and calendar route; real source counts |
| A08 | P1 | Idea Vault / ready-ideas count | Actual Idea tab, no fabricated ready items |
| A09 | P1 | Content Calendar / weekly occurrence deep link | Original Calendar overlay; saved project vs weekly preview routes not swapped |
| A10 | P0 | Weekly Plan edit/save/delete/auto-plan | Original owner persistence and schedule/occurrence keys, no duplicated project |
| A11 | P0 | Automation and Trending Moment | Original editor and automation data owners, no surprising background write on browse |
| A12 | P0 | Reminders | Original Creator and Personal hubs, no duplicate schedule or source |
| A13 | P1 | Progress Journey / Creator Progress | Correct existing overlay and ledger/progress screen, no cross owner mutation |
| A14 | P1 | Settings | Original Account/permissions/backup controls remain reachable |
| A15 | P1 | All Quick Actions tags and narrow/large font rendering | No lost button, all >=48dp effective targets, no inaccessible off-sheet row |
| A16 | P0 | Progress Journey actual Creator idea/project/stage history | Stable source keys, deduped events, real timestamps and correct target navigation |
| A17 | P0 | Corrupted `CreatorWorkflowTimelineStore` entry | No false clean-history success; if current owner swallows decode errors, this must be tracked as a blocker until addressed |
| A18 | P1 | Timeline read throws IO failure or collector cancels | Explicit non-destructive unavailable state and safe exit, no reset |
| A19 | P1 | Week / Month / Journey and selected date | Exact day events, previous/next month, leap day, time zone, no future phantom |
| A20 | P0 | Progress Journey Personal overlay remains OFF until opt-in | No Personal titles/note/photos into Creator event list or analytics |
| A21 | P0 | Toggle Personal ON→OFF, background restore and process recreation | No lingering private events after OFF or unintended opt-in persistence |
| A22 | P0 | Creator reward original action and reconciled write | Only unique ledger eventKey earns XP; actual committed event feedback |
| A23 | P0 | Creator app screen reopened / renderer recompose / legacy backfill | No new event XP, no replayed companion celebration |
| A24 | P0 | Unknown serialized reward type / old imported ledger | Cannot masquerade as a newly confirmed stage completion; stage key/type gate truthful |
| A25 | P0 | Stage-completion current pulse / reward toast | Still only after owner transaction commit; no optimistic animation |
| A26 | P1 | Companion selection OFF/ON and motion reduction | Original selected character, no forced haptic/motion; future ~3sec celebration doesn't block reminder response |
| A27 | P0 | Personal Done, Challenge note, Space manual check-in | Distinct sources and private labels; no Creator XP unless explicitly approved |
| A28 | P1 | Daily Brief source fields and empty queue | Actual active/due/overdue/reminder counts, no invented publication |
| A29 | P1 | Content Calendar weekly plan represented by original saved project | Occurrence-key dedup; correct dates/timezones; no ghost duplicate |
| A30 | P1 | Calendar 14-day chips plus past-due projects | Original Calendar source split and project/weekly links, no surprise reschedule |
| A31 | P1 | Offline/no server/permissions degraded | Local projections remain safe and honest; never misclaim cloud sync or alert delivery |
| A32 | P1 | Android tablet/phone/200% font/keyboard/TalkBack | All actions reachable; no repeated thick-box overflow or inaccessible chips |
| A33 | P0 | Comprehensive combined V337 owner stable P0 integration | Ledger, restore/backup, Snooze actual Android 15 device protections survive |
| A34 | P0 | Backup/restore/merge with existing rewards and Creator timeline | Original event keys/receipts retained; no reconstructed achievements from viewing |
| A35 | P1 | Quick Actions with very long task/project titles and 1k+ ideas | Bounded list, no ANR, no wrong ID from truncation |
| A36 | P0 | Final source SHA tied to installed APK and owner signoff | Verified Git commit, CI build result, APK hash, owner physical accessibility/functional decision |

## Untested source-identified risks

- `CreatorWorkflowTimelineStore.load` catches decoder exceptions and returns `emptyList()`: an unreadable source can masquerade as zero. This must be treated as unresolved until a source-compatible typed-error/recovery contract is approved and implemented.
- `V282ProgressJourneyUi` lacks an explicit timeline-read-error branch, and its saved Personal opt-in requires revalidation across activity restoration. Do not declare this fixed by a UI-only copy change.
- `CreatorRewardStore.decode` maps unknown reward types to `PROJECT_STAGE_COMPLETED`; never infer an actual newly credited stage or celebration from imported history. Source-backed event gate required.
- `V161QuickActionsHub` renders up to four overdue project cards with real mutation buttons. Narrow screens and confirmation/undo need special physical review.
- `FrameByNavinV101BApp` dismisses reward feedback after ~2.2s; companion's owner-requested ~3s appearance is a FUTURE UX08C improvement, not yet implemented.
- `CreatorContentCalendarEngine` de-duplicates `scheduleOccurrenceKey` of weekly occurrences represented by saved projects, so rewrites and new planner UI must preserve this.

## No-go criteria

No merge, promotion, APK stable or release if: accepted V337 P0 fix lost; stage/project/Personal record duplicated; XP rewarded by navigation; old imported ledger triggers unverified celebration; Progress Journey corrupt read displayed as successful empty; Personal notes leak into Creator progress or keyguard; mutation buttons change a task without clear intent; new paid provider/backends used without founder approval.

**Current result:** 36 specified deferred UX08A protection cases, **0 run**. This document-only source audit creates no code/test executable changes; it does not certify any existing production UI behaviour.

**Next:** Separate stacked UX08B Progress Journey implementation, then UX08C achievements, UX08D Daily Brief, UX08E Content Calendar, UX08F Quick Actions integration. One post-UX10 campaign remains mandatory.
