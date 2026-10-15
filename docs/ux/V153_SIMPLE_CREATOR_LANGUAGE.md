# Backlot V153 — Simple Creator Experience

## Product rule
A creator should know **where they are, what matters, and what to tap next** without learning Backlot's implementation. Show the answer first; reveal definitions, limitations and methodology on demand.

## Source inventory and triage (baseline: V152)
Source-level review found 83 Kotlin files under app UI, plus account/cloud, widget, reminder and data-engine generated copy. Not every UI file is an active screen. Maintain a verified route inventory before changing a feature. This inventory is a starting point, not evidence of visual-device testing.

| Entry route | Active implementation | User-facing audit surface | Phase |
| --- | --- | --- | --- |
| Launch & setup | MainActivity, V131LaunchGate, V174CinematicWelcome, V23AccountOnboarding, V18CreatorOnboarding, V20GuidedFirstRun | welcome, accounts, permissions, tutorial | 2 |
| Home | FrameByNavinV101BApp, V18TodayScreen, V20TodayCommandCenter, V131PolishUi, V133CreatorHome | next action, dashboard, navigation, quick actions | 2 |
| Ideas & capture | V09IdeaVaultUi, V14QuickCaptureUi, V117VoiceQuickIdeaUi, VoiceIdeaRecorderUi, widget/QuickIdeaActivity | capture, voice, search, status and conversion | 3 |
| Projects | V101BReminderUi, V20NewProjectWizard, V18CoreScreens, V131PolishUi | quick project, dates, reminders, progress | 3–5 |
| Project editor | ContentWorkspaceActivity, V19ContentWorkspaceAlpha6Ui, V19ContentWorkspaceAlpha3Ui, V19ContentWorkspaceAlpha4Ui, V19ContentWorkspaceAlpha5Ui | Plan, Script, Publish, progress, conflicts | 4 |
| Calendar, reminders & automation | V08WeeklyScheduleUi, V17AutomationCenter, V101BReminderUi, ReminderCheckInScreen, VoiceReminderActivity | schedule, check-ins, permission prompts | 5 |
| Insights & YouTube | V11YouTubeInsights, V172InsightsUi, V20InsightsDrilldownUi, V144YouTubeRevenueUi, V20InsightsFoundationUi | metrics, time windows, estimates, refresh | 6 |
| Learning & AI | V20OpportunityEngineUi, V20CreativeIntelligenceUi, V20WorkflowIntelligenceUi, V20VideoPostmortemUi, V20GeminiIntelligenceUi; data/CreatorOpportunityEngine and CreatorBrain | advice, confidence, explanations, data-quality disclosures | 6 |
| Settings, account & recovery | V144SettingsHub, V23ProfileAccountUi, cloud/CloudSyncActivity, BackupActivity, RecoveryCopiesActivity | permissions, sync, export, destructive actions | 7 |
| Assistive surfaces | V144GuideSystem, V140VoiceStudioPicker, V141InheritedVoiceCard, widget and reminder surfaces | Spark, voice, notifications and TTS | 1, 5, 7 |

## Canonical vocabulary (labels, not persistence/database identifiers)
- Home (not Control Room).
- Quick Actions (not Control as a section title).
- Ideas (navigation and user-facing destination; Idea Vault can remain in long-form help during migration).
- Projects (not Create for a view of existing projects).
- New Project (creation button).
- Plan, Script, Prepare to Publish (not Plan & Produce, Script Studio or Publish Studio in regular UI).
- Video Review (not Quick Autopsy).
- Your Results / Insights (not Revenue Intelligence, Creator Brain, Creator Playbook or Explainability as first-level headings).
- Earnings (for YouTube estimated revenue; always mark estimated numbers correctly).
- Backup & Sync, Back Up, Restore (do not conflate copying with replacing).
- Spark (one default guide; legacy enum FUNNY remains stable for migration).

## Copy standards
1. One main action and one main message per card. Remove repeated marketing statements.
2. Titles normally 1–3 words. Buttons use clear action verbs, usually 1–4 words.
3. Default helper copy one concise sentence; put advanced methodology behind “How this works” or “Details”.
4. Write concrete state: “Waiting for YouTube data”, not “Dataset unavailable”.
5. Avoid engineering jargon: snapshot, state authority, serialized, schema, URI, delta, baseline, counter comparison, learning memory, model provider, source-of-truth.
6. Avoid internal tags in accessible UI: ALPHA, RC, v18/v20, technical backend providers.
7. For a failure say what happened, whether work was kept, then one next step.
8. Never remove safeguards: permission effects, irreversible data loss, sync/restore conflicts, estimated revenue, AI limitations and evidence uncertainty.
9. Use sentence case in normal explanatory text. All-caps section labels only when visually useful.
10. Avoid body/helper sizes under 12sp where layouts permit; validate 1.3x and 2.0x font sizes on a device before accepting.
11. Keep backend keys, enum values, original creator data and existing core behaviours unchanged during copy-only passes.

## Delivery phases / regression gates
- P0 baseline & glossary (this document and route inventory).
- P1 stale copy and internal-release tags; require no reachable “Funny and Cute” or “CONTENT PROJECT 2.0 · RC2”.
- P2 Home/setup/navigation and CTA clarity; existing onboarding and home persistence still work.
- P3 Ideas and create-project flow; quick capture and project saving unchanged.
- P4 Plan, Script, Publish; no live YouTube publish claim, editor save/concurrency remains safe.
- P5 calendar/reminder wording; timing, snooze and permission effects unchanged.
- P6 Insights, earnings and evidence language; no invented analytics or false certainty.
- P7 backups and system errors; irreversible warnings remain explicit.
- P8 accessibility, visual screenshot walkthrough, state/regression tests, device validation.

Keep each phase reviewable; do not merge into the stable branch without successful CI and device checks. Work in this branch and tag acceptance status truthfully.
