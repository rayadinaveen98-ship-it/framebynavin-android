# Backlot Stage 3 — Spark + Mira companion identity

State: **Implemented in source/artwork; not yet built or device-validated.**

## Product contract
- Spark (`FUNNY`) remains unchanged for existing installs, and remains the fallback if no selection exists.
- Mira (`MIRA`) is the second user-selectable human girl guide. Do not repurpose the historical CUTE kitten or NAVI/FRAME identifiers.
- Six original, transparent, pose-consistent Mira WebP resources are in `drawable-nodpi`: welcome, point, present, listen, think, celebrate.
- The existing `V144GuidePrefs` preference key and choice gate persist the selection without changing project, reminder, or personal data.
- Two equal onboarding cards and Settings choices enable switching. `CinePulseGuide` routes to the same selected guide in onboarding, Home and first-run tour.
- Home companion, offline chat labels and prompts use the selected guide display name. Old internal Spark class names and test tags remain stable for compatibility.
- Creator/Personal/Challenge private notification builders add the selected guide large icon; Android monochrome small status icons remain unchanged. The challenge public lock-screen version remains anonymous.
- Background receivers read the persisted identity directly; cold starts do not revert notifications to Spark because MainActivity has not opened.

## Visual acceptance
1. Inspect Mira's six cutouts on both dark and light surfaces. Check pose consistency, edge alpha, eyes, hands, clothing and full-body crop at 64, 96 and 144 dp.
2. Spark is visually untouched.
3. Choose Mira on a clean install, set up Creator, switch to Spark in Settings, force stop and reopen. The exact identity must persist.
4. Compare Home portrait, guided steps, empty state, reminder/notification large icon, offline conversation and Settings preview.
5. Android 13–15 notification privacy: no activity text on public lock screen; small status-bar icon remains compliant; user can disable notification/channel and still operate.
6. Accessibility: focus order, semantic selection state, 1.5x font, reduced animation.

No per-stage APK or tests. Owner requested complete feature implementation first, then final integrated QA and signed APK.
