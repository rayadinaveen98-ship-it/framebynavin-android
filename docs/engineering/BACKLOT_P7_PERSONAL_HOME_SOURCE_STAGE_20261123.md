# Backlot Point 7 — Personal Home · Source Staging Ledger

**Founder authorization:** Approved continuation after the P6a unified Creator-first correction. Personal OS is the cared-for secondary space for creators, not a second setup or app mode.

**Stage:** SOURCE-STAGED CANDIDATE; UNCOMPILED; UNTESTED; NO APK; NOT DEVICE-ACCEPTED.

## Source of truth
- Repo: rayadinaveen98-ship-it/framebynavin-android
- Cumulative branch: feature/backlot-v337-point07-personal-home-source-after-p6a-20261123
- Parent P6a SHA: 41a900855232eb96afd411b9c5896b11e6e0fc3c
- Owner-accepted stable V337 SHA: 815ba69775f75b6e5f25bb838c2c17b4f09c203e
- Main and stable unchanged. No merge/CI/JVM/Gradle/instrumented test/APK.

## Existing Personal surface preserved
- V2232PersonalReminderHub, PersonalReminderStore, independent Phase1HabitStore, Today/Settings and Activity Center.
- V288PersonalTodayCard uses actual reminder records and verified completion receipts; no inferred done counts.
- Unified personal agenda, scopes, Done/Snooze, edit/pause/resume/delete with confirmation, bulk selection, voice/alarm preferences, delivery error states.
- V289PersonalSpacesScreen with notes/photos; Phase1HabitLocalStoreReview challenges; V298ReminderGroupsScreen; original reminder editor.
- No changes to scheduler, alarm receiver, database, auth, backup, cloud, personal recurrence, challenge writes or project data.

## Stage P7
1. Added P7PersonalHomeIntro.kt: warm compact editorial panel on Personal Today only (not Settings or Creator Home).
2. Message: "A little room for you." / "Your creative work matters. So does your everyday life." Avoids false data claims or emotional inference.
3. Uses the already-approved Spark/Mira LISTEN asset in a modest decorative stage, not a clickable notification avatar; preserves artwork aspect ratio.
4. Four real actions: New reminder, My Spaces, 21-day challenges, Groups. Each reuses the existing callback, store and destination.
5. Preserves existing v289-open-spaces UI test tag on the new Spaces action, removing the standalone duplicate top-row link.
6. Today summary, all existing reminder and challenge rows and safety-critical interactions remain underneath.
7. A pure P7PersonalHomePolicy gates unavailable read/write actions while keeping the independent challenge source accessible if reminder data fails.
8. P7PersonalHomePolicyTest.kt staged as unexecuted source tests.

## Deferred QA
- First install after P6a: welcome -> account -> companion -> original five pages -> Creator OS transition -> Creator Home -> Personal. Never show onboarding on Personal/Creator/Home taps once setup is complete.
- Android Compose compile, resource binding, small/large Android layouts; no clipped imagery or buttons at font 1.5x.
- Functional buttons: New Personal Reminder, Spaces overview, challenge manager, groups. Confirm return/back and existing deep links.
- Empty/loading/error states, failed alarm permission, challenge independent failure; never fabricate completion or history.
- Preserved action tags, Today scope, history, selection, Done/Snooze, editor, pause/resume/delete safeguards.
- TalkBack, focus order, reduced motion, readable contrast and 48dp targets.
- Existing creator account, P1–P6a flow and reminder data regression, read-only safety certification before eventual owner APK.
- Run targeted then consolidated QA only after founder explicit authorization. No execution during source stage.

**Point 8 (Idea Vault) not started.**
