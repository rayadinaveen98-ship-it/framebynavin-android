# Backlot P3 — Approved Three-Panel Signature · Source Staging

**Decision:** Founder approved the animated moving Backlot Signature motion study. P3 is independently approved for SOURCE staging, with all QA and APK/release gates still closed. Points 4–10 require separate approval.

## Baseline
- Repo: rayadinaveen98-ship-it/framebynavin-android
- Source branch: feature/backlot-v337-point03-signature-welcome-source-after-p2-20261123
- Parent cumulative P1 + P2 source: d607908c5b886f9d74deedc8719ff1056ed1c8ea
- Original accepted V337 P0: 815ba69775f75b6e5f25bb838c2c17b4f09c203e
- Rejected V338 not merged. Stable and main unchanged. No test execution, CI trigger, APK or release.

## Approved motion design
- Minimal almost-black background #0C0D11, no particle field, glowing rings, light beams or extra decorations.
- Three colored panels enter independently from distinct small offsets with restrained scale/fade and ease-out. Their EXACT approved SVG-equivalent paths, original colors and gradients have been extracted into three dedicated Android vector resources.
- As the panels settle, crossfade into the original fully assembled backlot_v198_brand_mark vector; the B-shaped dark spine is preserved exactly as the approved artwork.
- Fade in Backlot wordmark and then BUILT FOR CREATORS tagline, with quiet still hold, ~3-second signature total.
- Native splash uses the same original scalable logo vector, near-black background; adaptive launcher and legacy launcher use the approved vector; Appearance preview updated.
- Existing optional opening sound preference and original generated audio mechanism retained. No audio service or network changes.
- Normal app launches use P3BacklotSignatureWelcome instead of the prior V199 particle welcome. Widget/deep links keep their existing immediate bypass. End transition becomes an understated 240ms/180ms fade.
- Reduced-motion setting shows complete logo and text directly with a short 350ms hold. No repeated sprite motion.

## Exact source boundaries
- Add P3BacklotSignatureWelcome.kt
- Add p3_backlot_top_panel.xml, p3_backlot_middle_panel.xml, p3_backlot_lower_panel.xml
- Modify only V131LaunchGate.kt, values/themes.xml, adaptive launcher and legacy icon XML, and V133BacklotAppIcon.kt preview.
- Original V198 master vector remains unchanged. Old V199/other experimental welcome assets are not deleted, but the runtime launcher no longer references V199.
- No onboarding/auth/companion/navigation/scheduling/voice/backup/cloud code changes.

## Prepared QA (not executed)
1. Compilation-only Kotlin and AAPT verification for new Compose resource references and vector XML.
2. Run only P3SignatureTimelineTest and affected existing V151 launch policy tests before broad integration QA.
3. Android cold-start and warm-start video capture at standard, compact and large screen density; compare logo geometry and brand finish against approved moving prototype.
4. OS splash-to-Compose handoff: check visual flash/duplication, screen brightness and status/nav bars; no Home shown during logo animation and no blank frame.
5. Test existing launch sound preference on/off, device volume off, audio preparation failure, cancellation on background and deep link/widget bypass.
6. Confirm reduced-motion path, TalkBack, large text/contrast; app opens promptly.
7. Verify adaptive launcher crop/mask on Android 13–16 launchers, icon scaling and monochrome/themed icon; old 256px WebP is no longer launcher foreground.
8. Ensure persisted user data, setup/Sign In flow, notification routing, and all functionality unaffected. Use disposable QA data, never owner data.
9. Owner-approved consolidated QA and physical acceptance before any merge, signed APK or promotion. No CI/build has been run during source staging.

## Open risk
- The OS splash may briefly display the full mark before the animated panels reconstruct it. Treat this as a visual QA risk, not as verified polished behavior; final screenshot/recording review is required.
- Native device typography may differ from the browser-based reference; actual physical-device approval needed for wordmark, tagline and transition timing.
- Source is not compiled yet; no claim of APK readiness or test pass is made.

**State:** SOURCE-STAGED candidate / UNCOMPILED / UNTESTED.
