# Backlot V179 — Reminder redesign Phase 3: Bounded unanswered follow-ups

**Stack:** V179 on V178 on V177. **State:** Draft preview, no stable/main merge. No TaskStore field additions or migrations.

## New behavior
- When a normal one-shot notification is posted, Backlot schedules **one deferrable no-response observation** for approximately 20 minutes later.
- When voice playback completes or an alarm ends without a response, a **quiet follow-up** is scheduled based on the creator's reminder priority.
- There can be at most **two follow-up notifications**, never endless ringing or new voice playback.
- Every follow-up uses the **original authorized occurrence token**, existing Snooze/Started/Stage Done/Dismiss buttons, current TaskStore snapshot and the discreet low-importance notification channel.
- Initial Smart SOFT/VOICE/ALARM stages continue their existing schedule unmodified. Only the *final* Smart stage enters the bounded follow-up path. The terminal session is closed without invalidating its unanswered token.
- WorkManager scheduling is **deferrable, not exact**. OS background restrictions, battery management and offline/stopped states may delay a check-in or prevent it entirely. We don't claim guaranteed notification arrival.

## Default limits (proposal implemented; dedicated per-creator controls are in Phase 4)
| Priority | First after unanswered voice/alarm | Second after first | Approximate total |
|---|---:|---:|---:|
| Normal | 20 min | 30 min | 50 min |
| Important | 10 min | 25 min | 35 min |
| Critical | 5 min | 15 min | 20 min |

**Ordinary posted notifications:** first check-in after 20 minutes regardless of priority to allow a reasonable acknowledgement grace. After a first follow-up, the second delay uses the priority-specific interval.

## Safety and data protections
- Never call a voice/alarm service or create a new occurrence during a follow-up.
- WorkManager unique work is per task+token+attempt; `KEEP` avoids duplicate enqueues.
- An additional private two-bit claim record per task/token/generation prevents duplicate notification postings if Android restarts a worker. It does not store titles, notes or message contents.
- Worker loads a fresh authoritative task inside the creator data gate and checks the original occurrence fingerprint/token, generation, unfinished status, active working pause and the latest acknowledgement state. Invalid or responded-to tasks do nothing.
- A notification still POSTED after 20 minutes becomes observed-unanswered, not "read". A quiet follow-up is attempted only in an UNANSWERED state.
- Accepted Snooze, Started, Stage Done, Dismiss, Pause or Reschedule cancels follow-up jobs and clears attempt claims **after the authoritative TaskStore save**.
- Missing notification permissions, blocked channel or WorkManager failures do not trigger a louder fallback or repeated retries.
- For Smart, the terminal session must not invalidate the occurrence's unanswered notification.
- Project edits/reminder replacement invalidate stale occurrence fingerprints; scheduled workers fail closed.
- Quiet channel is forced for follow-ups even if original delivery preference was Voice or Alarm.
- Never silently complete a creator's project merely because an alert expired.

## Automated CI gates
1. Inherited V161–V178 source regression checks and V179 bounded/quiet guard.
2. Full JVM tests including policy eligibility, exact upper bound on two attempts, timings and token matching.
3. Android lint, androidTest compilation, APK assembly and upload/checksum.
4. Android 11 emulator: V179 durable duplicate claims plus V178/V177 token and fallback checks. Compiling tests does not count as executing them.

## Real Android phone acceptance
- Back up valuable Backlot data; verify signing before any in-place upgrade and **do not uninstall** a data-bearing installation to test a preview.
- Notification: ignore 20 minutes, confirm quiet follow-up with original actions; respond after first; confirm no second.
- Voice: allow configured sentence/repeats to end, wait relevant interval, confirm quiet follow-up without TTS replay.
- Alarm: allow hardware timeout, confirm sound stops, follow-up stays quiet and original action buttons work.
- Smart: ignore each configured stage, verify no duplicate gentle follow-ups before terminal; after terminal verify token and bounded quiet follow-up.
- Done, Started, Snooze, Pause, Reschedule, Dismiss just before a job executes: no stale post, no second attempt, no duplicate schedule or accidental project edits.
- Test no notification permission, blocked notification channels, Doze, reboot, force-stop, exact-alarm denied, battery saver, old Android 11 and Android 13/14+ notification rules.
- Verify session/action state, backup/restore, outstanding reminders, project data and no excessive battery wakeups.
- Accessibility, quiet hours, small screen and creator approval remain release gates.

## Known limitations
- This phase does not create a new Reminders Center pending-response screen; that follows in Phase 5.
- The timeout and follow-up times are minimum WorkManager delays and may run late.
- If Android blocks a quiet notification, no automatic audio escalation is attempted.
- V178/V177 emulator and all physical-phone hardware behavior still require explicit acceptance before any stable release.
- Phase 4 will add friendly controls for timeout, snooze, repeat and opt-out preferences; no user-facing knobs were added here.
