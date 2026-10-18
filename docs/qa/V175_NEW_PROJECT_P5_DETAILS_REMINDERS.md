# Backlot V175 — New Project Phase 5: Project details and reminder clarity

**Base:** V174 Quick Create preview; inherits V171 editor safety, V172 draft continuation, and V173 typography.
**Branch:** `feature/backlot-v175-new-project-p5-reminders-details`.
**Status:** Draft development preview. Not stable; no migrations or reminders engine changes.

## Implemented
- **Notes belong to the project.** In the full seven-step wizard, optional creative/project notes sit within the NAME stage (no eighth stage). In existing-project edit, notes sit next to the title, not inside reminder controls. The shared notes component remembers its expand/collapse state, shows existing notes and preserves the same `notes` draft value and save path.
- **Quick Create remains separate.** Its compact essentials and 24-hour default remain unchanged. Advanced creation reveals the same notes, styles and project metadata; no hidden shortcut persistence.
- **Voice is project-specific before creation.** Within advanced reminder settings, new projects and Quick Create use the existing `V140VoiceStudioPicker`, already used by edit mode. Voice selection changes the draft's `voicePersona` and saves with it, without requiring a detour through global Settings.
- **Reminder explanations are concise and honest.** The selected plan describes its rough check-in behavior and the alert delivery choice. Where appropriate, a reminder permission warning points the creator to Settings without falsely promising a guaranteed ring or voice alert.
- **Review explains the next step.** Project notes route back to the NAME step to edit; review tells creators that details can be edited later in the project workspace.
- **No underlying reminder engine changes.** All five plan values, preferences, scheduling and backup schema remain intact. No automatic resetting, defaults modification or silent migration.

## Automated gates
1. V161–V174 inherited checks and V175 source guard.
2. JVM unit tests including V175 copy/permission expectations, V174 Quick Create parity and V171 reminder safety.
3. Android lint, UI-test APK compilation (not on-device execution), preview APK assembly and artifact upload.
4. PR remains draft until phone QA and creator sign-off.

## Real-device QA before stable
- Create a standard seven-stage project; expand notes under NAME, write a long description, complete the wizard, reopen and check that the note persisted.
- Edit an existing project; verify notes appear beside the name, and editing them leaves overdue deadline, stage progress and snoozed reminders unchanged.
- Quick Create > More Options > choose notes, select voice and return; verify the draft is preserved and project-specific voice is saved.
- Switch reminder plans Off/Light/Guided/Urgent/Custom, including CUSTOM time validation and advanced preferences; verify descriptions update without changing reminder scheduling semantics.
- Select a specific voice inside advanced settings, preview it, create project, edit again and confirm the saved voice remained correct. Confirm global creator default voice was not modified.
- Test missing permissions on device: the app should warn/guide to settings, and draft values must survive navigation as established in V172. Do not claim guaranteed delivery before OS permission testing.
- Test rotation, long text, 100/130/200% font scale, limited space and keyboard use.
- Verify no regressions in Quick Actions, Settings, reminder center, backup and existing projects, and no data loss across in-place upgrade.
- Approve only after emulator/real-phone regression and creator review. No stable/main merge from CI alone.

**Next:** Phase 6 end-to-end release acceptance and physical-device sign-off.
