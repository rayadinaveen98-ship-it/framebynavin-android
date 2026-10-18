# V185 — Recoverable idea-to-project conversion

Built on V184 capture safety, accepted by the creator on Samsung A54 in this session. Version 185 / 2.3.5-v185-idea-conversion-preview is the next bounded Phase 1A candidate, not a stable promotion or completion of the full master plan.

## Behavior

- Conversion commits an intent journal before project creation. Recovery finishes the same project, source-idea link, reminder plan and single reward entry after interruption at any write boundary.
- Current canonical stores are read inside the creator-data gate. Repeated/concurrent conversion shares the existing project. An older partially created project is adopted without overwriting its notes or work.
- A missing previously linked project never triggers an automatic replacement. Removed/archived ideas are not resurrected. Restore generations invalidate pending conversions and stale requests.
- The conversion dialog shows Off, Light, Guided and Urgent reminder choices and describes their behavior. Initial choice is Off; later conversions inherit the last successfully committed conversion choice. Custom setup remains available in the project.
- Create and Cancel are disabled while saving. Success/navigation waits for durable project/link/reward acceptance; failure keeps the dialog, selected inputs and original idea for retry.
- Original idea notes, transcript and audio stay in the vault; source context is available in project notes. No store/schema replacement or deletion is introduced.
- Home slideshow and V184 capture safety remain intact. Progress-meaning work and Phase 1B Insights are pending.

## Verification

Required: V161–V182 source gates, full JVM regressions, lint, debug/test compilation and personalStable APK; 23 Android acceptance tests, including nine new conversion/UI tests and the existing 14 capture/reminder checks. Interruption simulation throws after intent, project, link and reward writes, then recreates the service and replays twice. Concurrency, old-generation replay, missing/deleted source, legacy partial creation, invalid deadlines, consistent backup export after interruption, pending UI, null/throw failure and retry are covered.

Results must be recorded on PR and CI after execution. No local Android/Gradle runtime is available. A screenshot from the instrumented dialog is captured for visual review. Debug variant is exercised on emulator; physical-device upgrade, reminder delivery and the non-debuggable APK still require acceptance.

## Device checklist

Upgrade V184 without uninstalling. Capture a throwaway Telugu/English idea with notes/voice, convert with Off, confirm the project opens once and the original idea/audio remain. Repeat with Light and confirm its visible setting. Confirm the next conversion remembers the last successful plan; Cancel must not change that preference. Reopen the linked idea/project and verify no duplicate project/reward. Check date/time pickers, long titles, keyboard, Back and large text. Actual process-kill recovery and restore should be tested on an isolated fixture install; avoid destructive tests against personal work.
