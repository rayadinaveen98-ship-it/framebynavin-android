# Backlot UX modernization — 02 Onboarding, Personalization & Home

Status: **SOURCE IMPLEMENTED ON STACKED FEATURE BRANCH; NOT TESTED, MERGED OR RELEASED**.
Stage-01 parent: feature/backlot-ux01-welcome-collection-20261117, draft PR #393.
Stage-02 branch: feature/backlot-ux02-onboarding-home-20261117.

## Approved behaviors staged

- Retain Stage-01 welcome animation and existing Google sign-in, local-first use, restored guide selection, Drive recovery gate, all projects and existing data.
- Fresh-install order: welcome -> Creator/Personal/Both focus -> Google sign-in or Continue Without Account -> Spark/Mira choice -> Creator/Both personalization -> optional reminder-permission guidance -> six-step Creator/Both guide -> Home.
- Account connection is not silently marked complete simply because a first-run focus was chosen. Existing complete accounts and locally configured users are not re-onboarded after upgrading.
- Google sign-in into an existing Backlot account explicitly clears the fresh UX02 journey marker before continuing; a returning account is never treated as newly created merely because the installation is fresh.
- Four Creator/Both questions: What do you create? Where do you publish? Why do you create? How do you create? Optional local Backlot display name; existing CreatorProfile is the source of truth, no implicit new project/data created.
- Permission guidance reuses existing Android notification/exact timing/full-screen/battery routes. It never requests all permissions automatically or marks them granted. The optional guide is durably marked complete. Existing V393 users who started before UX02 are not forced into the new step.
- Both guides support optional, session-only Android text-to-speech narration, disabled by default. The Android TTS engine is not initialized until opted into and is shut down when the guide exits.
- Six Creator/Both lessons reuse stable guided-tour enum IDs: Home, New Idea/Idea Vault, Project creation, Reminder Hub, Insights, Quick Actions. Skip/exit remains available. Opening Reminder Hub temporarily pauses the tour instead of drawing the coach on top of the hub.
- Fresh Personal-only focus gets a short three-step companion-led guide (Personal Reminders, 21-Day Challenges, My Spaces) before the real Personal hub. It never fabricates a Creator profile; the six-step Creator/Both guided tour is not forced on Personal-only users. Pre-UX02 Personal-first entries retain their existing direct route.
- Preserve actual Home slideshow and editor; keep Top 3, Project queue, Reminder Hub, Progress Journey and existing Weekly Focus content.
- Home greeting prefers a signed-in user's Backlot username, never their Google account display name; local users retain locally chosen Creator name and safe fallback.
- Header menu button uses existing approved Spark/Mira artwork. It still exposes the original Notification Center, companion panel and Gallery, and now adds Profile/Account, My Spaces, Weekly Focus, expandable Personal/Creator Reminders and Settings.
- Activity preview uses read-only actual V306 NEEDS_ACTION evidence, not generated/unread claims; See more opens the original Activity Center.
- Profile back navigation returns to Home when opened from the Home menu; Settings when opened from Settings.

## Files modified/created

1. ui/V393FirstRunQuickStart.kt — first-run metadata, four questions and durable permission-guide status with legacy skip.
2. ui/V23AccountOnboarding.kt — explicit returning-account signal for safe resumed onboarding.
3. ui/V402FirstRunPermissionGuide.kt — new optional permission explanation screen, existing permission callbacks only.
4. ui/V402PersonalFirstRunGuide.kt — short Personal-only, companion-led, non-destructive walkthrough.
5. ui/V402TourNarration.kt — opt-in, lifecycle-owned text-to-speech helper.
6 ui/FrameByNavinV101BApp.kt — routing, state, Home menu destinations, greeting, tour progression, profile back stack.
7 ui/V339HomeOverflowMenu.kt — selected companion avatar, non-destructive activity preview, additional real navigation.
8 ui/V2214HomeCompanion.kt — forwards authentic V306 projection and navigation closures.
9 ui/V18TodayScreen.kt — Home name override and existing Weekly Focus dialog request, not a duplicate dialog.
10 ui/V207GuidedTourPolicy.kt — six stable tour lessons.
11 ui/V20GuidedFirstRun.kt — Reminder Hub lesson and correct next action.
12 This document.

No schema migration, paid service, privacy-permission grant, image replacement, account deletion, project data rewrite, reminder engine edit or release is authorized.

## Important risks / integration gates

- The signed-in identity display must remain the Backlot username; profile editing and sign-out need later two-account device certification.
- The V339 menu has more items and needs a compact-phone and large-font accessibility review.
- Optional Android TTS availability, rapid step changes, voice-off behavior and lifecycle shutdown need Stage-10 real-device testing.
- New first-run Creator/Both tour has six lessons; pre-existing creator accounts remain complete and must not be forced to replay.
- Personal-only users see a shorter 3-step companion guide after permission guidance, then enter the existing Personal hub; Creator lessons are not imposed.
- UX04 Settings refactor must preserve the same routes and Profile return behavior.
- UX07 Reminders changes must preserve independent Personal/Creator delivery, while menu previews stay read-only.
- UX10 border-density changes can polish onboarding and menu layout but not alter their behavior.
- Integration with UX01 and future ten-stage cumulative branch requires owner review.

## Deferred acceptance after Stage 10

1. New Creator/Both first-run: exactly one focus save, optional account choice, persisted Spark/Mira, full what/where/why/how setup, permissions, six guided lessons, Home.
2. New Personal-only: no forced Creator setup or fabricated Creator data; optional permissions, short three-lesson companion guide and original Personal hub.
3. Existing completed installs: welcome -> Home, no re-onboarding; incomplete existing five-page setup resumes.
4. Verify network failure, Google credential cancel, interrupted first run and process death at every gate.
5. Confirm opt-in permissions are never reported as granted without Android evidence.
6. Both guide personas appear correctly in tour; optional voice narration defaults off and shuts down on dismissal; guided secondary actions do not modify data without consent.
7. Home username from Backlot rather than Gmail, local fallback, sign-in/sign-out and account switch.
8. All menu actions open the original screens, do not duplicate state, and return to the expected origin.
9. Activity preview only displays genuinely actionable V306 records; no false read or reminder acknowledgement.
10. Slideshow continuity, Reminder Hub correctness, no 20-30s Home reload regression, weekly focus, navigation, accessibility, and full Android unit/UI/integration/performance/physical certification.

## Release state

Only implementation commits and review documentation have been created. No tests, compile/build, CI run, APK, merges, production deploys or releases have been executed for UX02. Stage-10 comprehensive certification remains mandatory.
