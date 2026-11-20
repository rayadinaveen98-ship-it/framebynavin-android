# Backlot UX11-B — Top-right companion header and face-only avatar
Date: 2026-11-20
Status: SOURCE-STAGED ONLY. NO Kotlin compile, UI test, emulator, screenshot/device acceptance, APK, merge, release, account, reminder or backend changes.

## Approved source lineage
Source branch stacks on UX11-A draft PR #454 at exact source commit 4d8b303337ac11a16e9d2fb009897a2205f14bac, itself stacked on RX01-A..F and based on owner-accepted stable/main abbe88984bac16cfafbd990d9cb00ed54cfdd07e.
No merged changes to approved stable or main; UX09 remains outside the stack.

## Founder-observed defect
Home screenshot: "BACKLOT" / "Good Morning, Creator" appears at upper left, but a tiny full-body female companion floats in a second row below the greeting at upper right.
The exact source root causes were independently identified:
1. V18HeroGreetingBar (file V175BestFramesUi.kt), under 440dp phone width or high font scale, takes a Column wrapActions branch, placing the avatar in a new Row BELOW the greeting.
2. V339HomeOverflowMenu renders painterResource(v144GuideDrawable(selectedGuide, WAVE)) at only 40dp with ContentScale.Fit inside a 48dp circle. The approved original transparent WebP renders a full-body character whose face then occupies a tiny region.

## Source changes, exact scope
1. NEW UX11CompanionFaceAvatar.kt:
- Read-only normalized rectangular source crops from original approved transparent, nodpi welcome master WebPs, no image alteration or generation.
- Spark/Funny 192x192: source approx (58,2) + 80px square; Mira 256x256: (74,4) + 116px square; Cute 192x192: (36,4) + 120px square.
- Crop rect is computed with source image dimensions, clamped to valid bounds and drawn into a circular 44dp content region with high-quality scaling. Existing 48dp parent clickable Surface stays unchanged. Handles missing/corrupted resource by a textual first-letter fallback.
- FRAME/NAVI continue using the same approved Funny welcome master as the existing V144 source map; this stage does not claim redesigned identity assets.
- Crop windows have been visually reviewed against actual shipped QA APK originals. They show head/face/hair and minimal shoulders; do not distort face, recolor, redraw, replace or rescale body outside this exact Home avatar.
2. V339HomeOverflowMenu.kt:
- Replace only the full-body Image call with UX11CompanionFaceAvatar(selectedGuide); retain existing 48dp onClick Surface, dynamic chosen guide, existing visual indicator, dropdown state and callbacks.
- All menu actions remain untouched: Notification Center, Spark/Mira suggestion, Gallery, Profile, Settings, My Spaces, Weekly Focus, Creator Reminders and Personal Reminders. This is not a new profile-photo uploader or a second avatar preference store.
3. V175BestFramesUi.kt:
- When galleryInOverflow is TRUE (normal live Home), put the greeting text and one avatar in the SAME top-aligned Row. Greeting uses remaining flexible width, max two lines with safe ellipsis; avatar is anchored inside fixed 48dp top-right region. No conditional wrap at width below 440dp or font scale at/above 1.55.
- Legacy/standalone Hero when galleryInOverflow is FALSE still has its separate Gallery button and retains the previous narrow-width wrapping to prevent a crushed second touch control.
- No slideshow storage, photo import, navigation, project/routine, companion behavior, alert count, audio or notification state changed.
4. This source-stage acceptance document.

## Expected design result
- Phone widths 320dp, 360dp, 393dp, 412dp and larger: the avatar aligns horizontally with the BACKLOT/greeting header near the top-right safe inset; it no longer drifts into a detached second row.
- Circular identity reads as Spark or Mira FACE, not a tiny character poster. Original hair and expression are retained. Avatar has 48dp minimum touch target and no floating decorative overlay.
- The greeting can wrap to two lines, but the companion still stays in the same top header row. For unusually long user names or 200% font scale, greeting ellipsizes rather than overlapping the avatar.
- When the selected guide changes in Settings, the live Home avatar updates from existing V144GuidePrefs; no new account identity or persisted face crop is introduced.
- Selected character continues to appear full-body in its appropriate tour/celebration/companion surfaces; UX11-B does not edit those resources or characters.

## Must check after founder says ALL DONE — planned, not performed
- Compile with actual Compose Canvas imageResource/drawImage API and current Kotlin version.
- Render screenshot comparisons against the founder's Home example on representative Samsung Android 15 and smaller API26 devices. Confirm real face placement and crop, especially Spark glasses, Mira eyes/hair/forehead and Cute ears.
- Verify 48dp touch target and accessible "Open Backlot menu with [character]" label, selected identity, menu expanded/collapsed state and alert badge.
- Verify all menu actions still open their original destinations, including disabled Spark and private reminders.
- Test 320dp 393dp/tablet, portrait/landscape, 100%/200% font, extra long Creator name, system navigation/status bar and accessibility TalkBack.
- Verify original slideshow banner/gallery and empty projects/home cards do not shift incorrectly or cover the face.
- Verify no new drawable WebP, no face deformation, no image resizing outside Home.
- Failures block the final integrated QA APK until repaired in isolated candidate.

## Next stages
UX11-C restore the historic five-page Creator setup rather than redesigning it; compare current V393QuickCreatorSetup and still-existing V18CreatorOnboarding with exact original. UX11-D sign-in/sign-up polish, UX11-E Personal Sanctuary home, UX11-F remove numeric composer chips. Continue source staging one at a time.
When the founder says ALL DONE, and only then, reconcile RX01 A..F and UX11 A..F, compile/run unit/regression/emulator/UI QA and prepare a signed, version-identified APK for multi-day physical testing before any separate release approval.
