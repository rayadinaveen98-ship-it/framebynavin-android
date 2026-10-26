# Backlot V217 — Pose mapping and design-to-Android acceptance matrix

**Planning / review artifact only — no runtime resource changes.** Supports [PR #130](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/130), [issue #129](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/129) and [release acceptance #120](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/120).

## Identity continuity

- Current source: `V144GuideSystem.kt` persists `FRAME`, `NAVI`, `FUNNY` (Spark), `CUTE` under `backlot_character_identity_v151/selected_character`.
- **Spark** real reference: glasses-wearing human creator in black hoodie. Do not change its `guide_funny_*.webp` files, code path, pose mapping or selection.
- **Cute** real source: light/cream *kitten*, not dog. The new C3 exploration must retain cat species and eventually use meaningful state poses rather than reusing `guide_cute_welcome` for every state.
- **Frame** and **Navi** currently use the older Compose procedural renderer in `V137CharacterSystem.kt`. Their later artist-approved raster route must preserve every V144 public API and call site.

## Twelve semantic states — intended minimum design behavior

| State | Primary representation | Motion guidelines | Explicit review requirement |
| --- | --- | --- | --- |
| `IDLE` | idle/welcome | slow breathing or static | calm and readable at 48dp |
| `WALK` | walk / guided movement | short directional movement only | direction mirror safe |
| `POINT` | pointing | short gesture | gesture does not cover button/text |
| `PRESENT` | present/talk | brief explaining | distinguish from silent listening |
| `LOOK` | observant look | minor eyes/head movement | no compulsory extra bitmap |
| `NOD` | attentive nod | small discrete acknowledgement | not endless idle loop |
| `WAVE` | greeting | single welcome gesture | onboarding fits 96–112dp |
| `THINK` | thinking | eye/microexpression | avoid fake 'AI working' cues |
| `LISTEN` | listening | neutral attentive | **no implication of live mic without permission** |
| `SUCCESS` | positive confirmation | compact single reaction | only if real state changed |
| `CELEBRATE` | celebration | brief non-blocking cheer | reward hook is **later** under #126 |
| `REST` | restful | static/reduced motion | no screen overdraw or visual noise |

**Pose asset threshold (proposed):** at least nine authentic internally consistent art poses per new identity: idle/welcome, listen, think, present, point, success, celebrate, rest, directional walk. LOOK/NOD/WAVE may be deterministic smooth transformations *when approved*; they must visibly match the intended meaning. The four expression crops in current concept art are insufficient and must not be packaged as final WebPs.

## Look-dev style corrections before final signoff

- **Frame F2**: film aperture clue, matte black/gold precise surfaces, reduce oversized toy head, excessive glow, trim clutter.
- **Navi N1**: current exploration's human/black-hoodie similarity to real Spark must be fixed; preserve coral UI accent, distinct hair/wardrobe and gesture language.
- **Cute C3**: keep premium kitten anatomy and identifiable ears/whiskers/tail; tone down preschool cuteness, plush toy accessories and pink glow.
- No generated/recreated Spark art. Compare against the actual untouched 192px source bitmap and originally approved in-app appearance.

## Design preview QA actually completed

- Browser: **Chromium 144**, responsive standalone HTML concept viewer; checked JS via `node --check`.
- Interactive assertions: three character buttons, four expression buttons, character switch, celebration state, light/dark contrast.
- Viewport probes: **1360×1000** and **390×844**, no horizontal overflow reported.
- Images: cropped from **one generated composite concept board**; no clean alpha or cross-pose character consistency guarantee. CSS `48px` is **not** Android `48dp`.
- Nothing installed onto owner's Samsung. No Android instrumentation or device screenshot was exercised by this look-dev QA.

## Mandatory Android acceptance AFTER artwork approval

1. Render actual transparent PNG/WebP at 40/48dp, 96/112dp and 200dp in both themes. Compare **real Spark unchanged** side-by-side, no cropped fake.
2. Test source build/lint/instrumentation compile and Android 11 emulators **1080×2400/default** and **900×1600/1.5x system font**; include long Telugu and safe-area boundaries.
3. Test 12 states per identity, RTL/direction mirror, keyboard open, reduced-motion static fallback and low-RAM image decoding. Ensure no unrelated navigation overlay or blocker while reminders ring.
4. Verify persisted selections survive relaunch, upgrade, independent backup, restore preview and settings selection changes; no new persisted ID.
5. Verify images do not cause OOM, dropped frames or unnecessary CPU/GPU on long sessions, especially small/low-cost phones.
6. Preserve original creator onboarding and Spark resources byte-identically.
7. Keep draft PR and no stable/main merge until physical owner-device signoff and backup integrity per [#101](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/101).

## Non-goals

Do **not** modify alarm and voice particle engine, reminder scheduling, acknowledgment/snooze, bottom nav, assistant, project/idea data, user photos, original onboarding or stable signer. Those belong to other scoped phases.
