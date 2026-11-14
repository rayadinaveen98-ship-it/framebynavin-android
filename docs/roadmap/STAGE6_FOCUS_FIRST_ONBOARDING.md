# Backlot Stage 6 — Focus-first onboarding

Status: **implemented in source; final combined visual and functional QA pending**.

## Clean-install journey
1. Native splash + existing cinematic intro.
2. **Choose focus**: Creator, Personal, or Both. This is a preference, never a feature lock or data mutation.
3. Existing **optional account** step (Google or Continue Locally). Copy no longer starts by advertising withdrawn Settings backup tools.
4. Existing guide-selection gate now offers **Spark or Mira**, and saves the exact identity.
5. **Choose first action**: Creator project, idea capture, Personal reminders, or Explore Home, matched to initial focus. The only side effect is routing; no fake project/reminder/Done receipt is generated.
6. Creator's old detailed five-page goals/permissions setup is deferred, not silently completed. Profile > Complete Creator Setup explicitly opens it later. On-device reminder permission prompts remain controlled by existing reminder creation flow.
7. Existing fully configured users skip the new gates on upgrade. Existing interrupted old setup flows continue their prior gate instead of being reset.

## Settings
Appearance & Sound > Your Focus can change Creator/Personal/Both any time without replaying first-run or modifying stored records. Settings > Companion choice stays independent.

## Protected invariants
- No creator profile defaults invented, no saved project/reminder/Space/challenge mutations.
- Personal-only users are not forced through Creator platform/goal questions.
- Account and recovery checks remain in place for authenticated users and are not bypassed by the first-action screen.
- Deep links and widgets never replace identity or cause silent setup completion.
- Current guided tour remains available to configured/legacy users; new users aren't forced through a tour before doing their first real action.

## Final combined acceptance gates
- Fresh installs: all three focus choices × Spark/Mira × Google/local flows.
- Returning install: Creator profile, companion identity, existing data, settings and tour progress retained.
- Interrupted onboarding and process kill at each gate; relaunch resumes with no duplicated account/project/challenge.
- First project/idea/Personal action reachable and editable, and Android permission denial handled gracefully.
- Profile > Complete Creator Setup allows later opt-in without altering existing work.
- Font scale 1.0/1.5/2.0, accessibility focus/navigation, 320dp compact, portrait/landscape/tablet.
- Full regression, signed APK and owner physical-device validation only after all source stages.

No per-stage tests or APKs by owner instruction; do not imply final release readiness from this source change.
