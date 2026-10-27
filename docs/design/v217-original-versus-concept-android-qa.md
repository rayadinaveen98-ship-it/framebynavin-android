# Backlot V217 — Real-Android Original vs Concept Visual Acceptance

**Date:** 2026-10-27. **Scope: test-only companion comparison; not a production redesign or APK release.**

## Exact-head evidence

- Source SHA `c2d35f5a3e2b0f4ae7924b55c52fb9e3ee28a546`, branch `feature/backlot-v217-companion-renderer-prototype`, draft [PR #131](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/131).
- [GitHub Actions run 40441511417](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40441511417): **build SUCCESS**, **emulator SUCCESS**, four V217 instrumented tests on each of default and compact Android 11 configurations. Artifact **12706486890**: `Backlot-V217-Concept-Real-Android-PNGs`, 8 real Android window PNGs, plus emulator test logs.
- Profiles: 1080×2400 default emulated Pixel 6; 900×1600 compact at 1.5× system font scale.
- Downloaded all **eight** PNGs, decoded and visually compared each (concept top/bottom plus Original-versus-Concept top/bottom, each profile). They all contain genuine rendered UI pixels; none is a previously observed blank first frame; no OS error overlay.
- Source-only QA includes `tools/check_v217_companion_assets.py --mode concept`: five original Spark WebP Git blob hashes verified unchanged; unique, transparent 740×760 WebP F2/N1/C3 concept cutouts confirmed. No random new Spark art accepted.

## Comparison findings

- **Real Spark stays identical**. The left existing Spark and right benchmark Spark are the identical black-hoodie glasses-wearing character, not AI-generated fictional robot/star artwork. Small avatar also renders.
- **Frame:** existing procedural flat mascot with rectangular gold-framed head vs staged 3D gold/black aperture character. New concept has superior volume, texture and professional polish. Still only **one static base pose**; emotion/gesture variety missing.
- **Navi:** existing simple flat human/guide vs staged 3D young human with navy/coral jacket. Real Spark remains glasses-wearing human in black, so Navi is visually distinct but still needs expression and wardrobe testing at small size.
- **Cute:** current undistorted existing kitten vs staged C3 cream kitten. Identifiable ears, eyes and tail. Actual 48dp concept silhouette is noticeably tiny because the original full-body canvas has substantial transparent margins: optimize **visual fill** after pose production. Do not activate the corrupted old Thinking/Celebrate image files; retain current welcome-only master until approved replacement.
- **Compact viewport**: top comparison shows Spark/Frame/Navi and beginning of Cute, while bottom screenshot contains Cute fully above Android system navigation after scroll. Text is intentionally large at 1.5×; no actual production content/card clipping is proven or disproven by this synthetic comparison fixture.
- **Default viewport**: all four guide rows fit simultaneously in top and bottom comparison screenshots. Concept-only screen top/bottom show all staged characters.
- Screenshots are **real emulator captures**, NOT Canva art/AI contact sheets. They are synthetic Compose test scenes, **NOT MainActivity, onboarding, regular Home or Daily Brief**. Do not label them app screens or user-visible shipping behavior.

## V217 production blocker and next gate

1. Final nine **independently illustrated** alpha-transparent poses each for Frame, Navi, Cute: idle, listen, think, present, point, success, celebrate, rest, walk. LOOK/NOD/WAVE remain intentional motion transitions; every `CinePulseState` must have a documented deterministic meaning.
2. Use `tools/prepare_v217_pose_pack.py` on independent source PNGs/WebPs in a separate staging directory; verify transparent margins, matched baseline, silhouette occupancy at 48dp. This helper never writes app resources.
3. Strict release gate `python3 tools/check_v217_companion_assets.py --mode production` must succeed **after** asset approval. Right now it should block because the nine pose sets are missing; successful concept QA does not waive it.
4. Integrate selected guide in first-run companion choice, Settings, Daily Brief, reward/empty-state surfaces in isolated PR, preserve original V144 selection keys and old assets; run full Android visual screenshot acceptance and #101 physical upgrade/backup gate.
5. No changes to Spark art, alarm/voice/reminder engines, signing, version, stable/main or creator records in this candidate.

**Decision:** V217 concept loading, original-guide visual comparison and image-byte safety gate are **scoped accepted** at the exact SHA above. Complete premium companion system remains **not accepted / not released**.
