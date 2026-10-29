# V228.4 — Confirmed creator-stage celebration (engineering preview)

## Why refine rather than replace
Backlot already displays `V127StageCompletionPulse` when users tap **Step Done**. That old pulse lasts less than a second and starts immediately on the optimistic UI action, potentially celebrating a write that has not completed.

## Changes
- Stage-completion pulse uses `CreatorViewModel.rewardFeedback`, which is queued on a stage reward **after the existing durable save/reward path**, not `System.nanoTime()` on tap.
- Pure `V2284StageCelebrationPolicy` filters to `PROJECT_STAGE_COMPLETED`, never idea capture or publication rewards.
- Existing stage pulse becomes ~2 seconds total (1.6s present + 0.4s exit), with restrained red/gold ring, check mark, improved typography.
- Respects the Android animator-duration scale 0 / remove-animations preference by showing only a brief static confirmation and omitting haptics.
- Does not create or double-count rewards, change project progress, migrate storage, modify creator or personal reminders, trigger an alarm, or change any backup or YouTube data.

## Limitations and QA
This still needs an Android 11/15 real UI acceptance test to prove timing and stage-only behavior. Draft PR stacked on Build 255; Build 256 separately checks the same APK signer and source data compatibility. Existing `V22RewardToast` keeps its independent feedback timing.
