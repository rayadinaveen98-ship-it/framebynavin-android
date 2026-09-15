# V134 — Backlot Desktop Foundation

## Goal
Ship the first real Backlot desktop application without destabilizing the validated Android product.

Backlot desktop is not a web wrapper and not an Android emulator. It is a native JVM desktop application built with Compose Multiplatform so the product stays in the Kotlin/Compose family while desktop interaction can evolve independently.

## Platform strategy
- First target: Windows 10/11 x64.
- Native packages: MSI and EXE.
- Desktop build is isolated under `desktop/` as a standalone Gradle project.
- Android `app/`, package id, backup formats, reminders and cloud compatibility remain untouched in V134.
- Kotlin: 2.4.20.
- Compose Multiplatform: 1.12.0.
- JDK: 17.

## Product contract
The desktop foundation must express the same Backlot product promise:

> Capture an idea → turn it into a project → keep it moving → finish it → learn from the workflow.

V134 includes real usable desktop surfaces:
1. Today — creator desk, pipeline overview and next actions.
2. Idea Vault — capture ideas and promote them into projects.
3. Projects — stage-aware creator pipeline.
4. Calendar — project deadlines.
5. Insights — private workflow signals based on actual local project state.
6. Quick idea capture and new project creation.
7. Project stage progression from Idea through Published.
8. Local persistence across desktop restarts.

## Visual contract
- Premium cinematic Backlot identity.
- Deep black/navy surfaces.
- Warm gold as the main action/brand accent.
- Cool blue for creation/productivity actions.
- Dense but calm desktop information hierarchy.
- Left workspace navigation and wide creator canvas.
- No fake analytics and no placeholder social metrics.

## Data safety
V134 desktop writes only to the user's local profile directory:

`~/.backlot/desktop-v1.db`

Writes use a temporary file followed by replace/atomic move when supported.

The desktop store is intentionally its own V1 format. It does **not** reinterpret or overwrite the Android backup/cloud formats. Cross-device sync will be introduced through an explicit shared schema/migration layer, not by guessing at compatibility.

## Deliberately deferred
These are not to be faked in V134:
- Android reminder/alarm parity.
- Google/YouTube account connection.
- Supabase cloud sync.
- Android ↔ desktop live sync.
- YouTube performance analytics.
- Video Postmortem / Creator Brain cloud intelligence.
- Desktop notifications and background scheduling.
- macOS/Linux installers.

## Validation
CI must run on Windows, execute unit tests, compile the desktop app, and create installable Windows packages. The local persistence round-trip is covered by a unit test.

## Next desktop milestones
- V135: shared Backlot creator data contract + safe import/export bridge.
- V136: cloud/account connection and cross-device sync.
- V137: desktop reminders, system tray and notification center.
- V138: creator analytics / Video Postmortem desktop experience.

The milestone numbers above are directional; implementation may be regrouped if repository evidence shows a safer sequence.
