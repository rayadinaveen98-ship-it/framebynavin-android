# FrameByNavin v130 — Cine Pulse Mascot Rebuild

## Source of truth
The user-approved Cine Pulse concept board is the visual source of truth for the mascot. v129 proved the live guide architecture but did not match the locked character closely enough.

## Locked visual identity
Cine Pulse must read as the concept character, not the earlier stick-like guide:
- soft horizontal black oval face
- two warm vertical luminous eyes
- black tapered body with rounded silhouette
- glowing heart core on the chest
- multicolour translucent crown/scarf ribbons using pink, coral, orange, aqua and warm gold
- four-point crown spark
- rounded black limbs with luminous coloured edge treatment
- cute, premium, calm proportions rather than geometric robot proportions

## Motion contract
The existing app-driven states remain: IDLE, WALK, POINT, THINK, LISTEN, SUCCESS, CELEBRATE and REST.

Motion must be restrained and meaningful:
- idle: breathing + ribbon drift + occasional blink
- point: body lean + one curved arm pointing to the actual target
- walk: opposing bent stride + gentle vertical bob
- listen: open attentive arms
- success: smiling eyes + small open-arm response
- celebrate: smiling eyes + raised arms + restrained star/spark accents
- rest: reduced motion and softened eyes

## Rendering architecture
v130 ships as a high-fidelity native layered-vector Compose renderer so the full build can be produced and validated in-repo. The journey API remains isolated behind FrameGuideCompanion/CinePulse state mapping so a future authored Rive `.riv` file can replace the renderer without changing onboarding/tour business logic.

No GIF or looping video is allowed for the live guide.

## Regression requirements
- v129 Google Calendar integration remains intact.
- v129 orb idle -> tap-to-listen behavior remains intact.
- Lumen Flow/Aurora and existing supported themes remain intact.
- New Project parity remains intact: Urgent, Custom, reminder styles and notes.
- welcome animation/thread work remains unchanged.

## Release gate
- versionCode 130
- versionName `2.0.0-rc6-cine-pulse-mascot-rebuild`
- unit tests pass
- Kotlin compile passes
- debug APK assembles
- CI uploads installable APK
