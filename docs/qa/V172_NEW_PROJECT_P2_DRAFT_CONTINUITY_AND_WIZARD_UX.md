# Backlot V172 — New Project Phase 2: Wizard and draft continuity

**Base:** V171 P1 safety preview. **Branch:** `feature/backlot-v172-new-project-p2-draft-ux`.
**Release state:** Draft preview only; no merge to stable/main and no data migration.

## Creator product contracts

1. **Full seven-step New Project experience remains the default**. Quick Create is a separate shortcut; all five reminder plans and advanced reminder delivery remain available. No changes to persisted task schema or scheduling engine.
2. **Unsaved draft protection:** After a creator modifies a project form, closing it (back-arrow, Android Back or dialog dismissal) asks whether to KEEP EDITING or DISCARD. The prompt does not write or delete any project. An untouched form can close directly.
3. **Settings detour:** Selecting Reminder Settings temporarily closes the composer, retaining its saveable fields, creator choices and wizard step. Closing Settings returns to the same project composer session, not a blank project. Settings → Profile → Settings → close follows the same path; child Weekly Plan/Automation returns to Settings. When a creator explicitly saves or discards, the saved editor UI session is released.
4. **Activity recreation:** Production-style choices are saveable (not just `remember`), along with the other existing form values. Persistence across process death without durable offline drafts is **not claimed**.
5. **Wizard navigation:** Switching between steps requests visibility of the selected step; the old always-scroll-to-bottom behavior is removed. Step counter and progress indicator follow the active step. Collapsed cards show a warning icon if the current value is invalid. Due dates and the final review show human-readable hints when invalid.
6. **Inherited protections:** V171 overdue deadline, reminder snooze, progress and save-failure protection continue unchanged.

## Automated gates

- V161–V171 inherited preservation scripts and `tools/check_v172_new_project_draft_ux.sh`
- JVM unit tests, including fingerprint/discard-choice tests and V171 project-safety regressions
- Android lint, `:app:assembleDebugAndroidTest` and standalone V172 preview APK
- Android Compose discard-button tests included in compiled device-test suite; compilation alone **does not** prove on-device execution

## Phone verification before stable release

- Back up app data. Install only on test device/prototype with compatible signing certificate; never uninstall creator data.
- Create a full wizard draft, edit title/style/platform/reminder, go Back, choose KEEP EDITING. Verify all values and active step stay intact.
- Repeat and choose DISCARD; reopen New Project and verify a clean new session.
- Change reminder settings, open Settings, edit permissions, return; confirm notes, project type, style, exact deadline, reminder selection, and step are unchanged.
- Test Quick Create → More Options → Back to Quick Create without a draft reset; return from Settings while in Quick Create.
- Try Android Back, app header Back and dialog close. If Settings opens the Profile child screen and returns, the draft must remain.
- Rotate device/recreate activity; check all editable fields including production styles.
- Try seven-step revision on compact phone and with 100%, 130%, 200% font. Active stage heading must remain visible, including when revisiting earlier cards.
- Test automatic background write error recovery and creator-owned reminder states again, since V171 is stacked but still awaiting full device sign-off.
- Verify legacy personalized project defaults, existing projects, voice/audio, weekly slots, backups and full app navigation remain unchanged.

**Not implemented:** durable auto-draft storage across device restart/app force-stop, autosaving every keystroke, background uploads, or a stable release. Offline drafts would require separate privacy/retention and cleanup design.

**Next phases:** P3 visual hierarchy/typography; P4 Quick Create; P5 reminders and project details; P6 end-to-end release acceptance.
