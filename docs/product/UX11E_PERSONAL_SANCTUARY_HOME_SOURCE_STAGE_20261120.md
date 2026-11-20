# Backlot UX11-E — Personal Sanctuary Home

Date: 2026-11-20
Status: SOURCE-STAGED ONLY. Draft/unmerged. No Kotlin compilation, JVM/lint/instrumentation, emulator, physical QA, signed APK or release.

## Source lineage
Protected main and stable/backlot-ux01-08-ux10-owner-accepted-20261120 are at abbe88984bac16cfafbd990d9cb00ed54cfdd07e.
UX11-E source is stacked on UX11-D draft PR #457 at 850a86dedd55592a0a27c5c322b0fdb830baa4cf, then UX11-C #456, B #455, A #454 and RX01-A..F #448-453. UX09 remains excluded.

## Founder request and rationale
The existing Personal Reminder Home is functional but reads as disconnected utility cards rather than a coherent private personal space. Preserve the real Today card/response, recurring reminder Rhythm, My Spaces, Challenges, Progress, Groups, settings, creation and unified agenda. Improve premium warmth, hierarchy, daily usefulness and meaningful empty state, without new data stores, fake reminders, fabricated streaks or a new notification engine.

## Data authority audit
- PersonalReminderStore.remindersFlow is the canonical local list; loading errors remain visible and never become a successful empty state.
- PersonalReminderStore.completionHistoryFlow and V288PersonalTodayPolicy.snapshot provide actual active reminder count, verified Done today, next reminder/title/due and attention count.
- PersonalDeliveryCoordinator.apply is the sole Done/Snooze writer; exact saved ID, pendingOccurrenceMillis, pendingToken and re-arm error handling remain unchanged.
- Phase1HabitStore and V337UnifiedPersonalAgenda provide separate Challenge records. Challenge alerts are not auto-confirmed by opening this screen.
- UX07BPersonalRhythm is a read-only view of existing recurring reminders with real edit/create/Challenge/Group links.
- V340HomeSpacesEntry is the existing read-only private Space observer. No new Room file or photo loading in the Sanctuary hero.
- UX07EPersonalProgressOverview is verified historical activity. Unavailable history must be labelled unavailable, not silently represented as zero.
- Existing full agenda, ALL/TODAY/7 DAYS/NEEDS ATTENTION scope navigator, selection/delete guards, deep-linked focus, Activity Center and permissions remain source-owned.

## Approved visual implementation
A distinctive warm sanctuary over the existing dark Backlot identity:
- Deep charcoal-plum fabric with subtle rose and amber light fields. Native static Compose Canvas with no media download, shader plugin or GPU animation loop.
- Eyebrow YOUR PERSONAL SPACE, locally timed greeting, locale-formatted date and calm copy.
- Explicit real Personal Active count, and Done Today count ONLY when completion history is available; otherwise display Unavailable.
- One immersive Up Next / Needs Attention area from authoritative V288 snapshot. Original Personal title and saved schedule appear; no false claim that Android showed an alert.
- Done/Snooze only when V288PersonalTodayPolicy.canRespond is true. Buttons call the unchanged source-owned PersonalDeliveryCoordinator through the existing perform block. Source response test tags are preserved; on narrow screens buttons stack vertically.
- Primary NEW REMINDER creates the original Personal draft/editor. MY SPACES opens the original Space overview. The previous detached My Spaces text button is replaced by the hero action, preserving its testTag.
- Hero renders even when no reminder has been created. The lower empty state becomes editorial rather than a second large Create button.

Existing sections are grouped below the hero with restrained typography:
- Your Rhythm (original UX07B screen, recurring reminders and Challenge/Groups).
- Your corners (original V340 Spaces preview and linked private journals/photos).
- Looking back (original UX07E verified seven-day progress).
- Your reminders (original unified scope and source-owned Personal + Challenge agenda).

The original top bar, Activity Center, Personal Settings, create dropdown, Creator/Personal switch and all backend action owners remain unchanged.

## Changed file scope
1. NEW app/src/main/java/com/framebynavin/app/ui/UX11EPersonalSanctuaryHome.kt — native warm hero and section headings; strict read-only projection with real response callbacks.
2. app/src/main/java/com/framebynavin/app/ui/V2232PersonalReminderHub.kt — connect the hero in place of the old V288 standalone Today card, move My Spaces primary link, keep original Rhythm, Spaces, Progress, Agenda and error/permission/selection logic.
3. This documentation.

The original V288PersonalTodayCard file and existing Personal/Challenge stores, schedulers, TTS/music, receivers, account/Google, manifest/permissions and UX11-F composer remain unchanged.

## Deferred acceptance and quality gates AFTER founder says ALL DONE (NOT RUN)
1. Kotlin/Compose compilation, lint and JVM/unit regressions for the full RX01 + UX11 stack; check Canvas and new component symbols.
2. Physical Android visual review at 320dp, 393dp, large font 200%, tablets, portrait/landscape and API26/Android15; readable contrast, 48dp+ controls, no text clipping.
3. Empty, one upcoming, multiple late, paused, archived, read-error and history-unavailable states show meaningful truthful copy.
4. Double response, stale token, exact Done/Snooze re-arm and duplicate attempts respect the same original PersonalDeliveryCoordinator; no fabricated success.
5. Spaces preview and overview, private photos/journals, Groups, Challenge and ALL/TODAY/7-day views still navigate correctly; no new private data reads or persistence.
6. Deep-linked reminder/challenge focus, app Back, notifications disabled, time changes, Progress vs Challenge separate histories and real daily statuses remain correct.
7. Screenshot compare with the founder-provided functional foundation, TalkBack focus/order and no excess gradient GPU overhead.
8. UX11-F personal reminder creation cleanup is separate and not implemented by this stage.
9. After explicit ALL DONE, reconcile all staged PRs excluding UX09, run unified regression/Android device certification, build a traceable QA APK for multi-day founder review, then request separate stable/release approval.

Do not claim that this source is already compiled, integrated, installed, device-tested or ready to publish.