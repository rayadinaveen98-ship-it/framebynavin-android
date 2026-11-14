# Backlot Stage 2 — Legacy widget retirement

Status: **IMPLEMENTED IN SOURCE; not built or device-tested** (14 Nov 2026).

Owner instruction: retire *all current* launcher widgets. The new widget family will be designed and built separately, with a final combined QA gate.

## What was changed
- Removed **all 12** former `AppWidgetProvider` manifest registrations (9 legacy Creator + 3 V224).
- Removed the legacy widget-migration Activity registration and the widgets row in Settings. No old widget picker is reachable from Settings.
- Stopped appearance changes from initiating the obsolete widget refresh.
- Did not touch project/idea/Personal records, reminder scheduling, app identity, signer, or advanced local-data erase.
- Retained internal `CreatorWidgetContract`, `QuickIdeaActivity`, and old preview/provider/update implementation source **temporarily** to preserve app/deep-link compatibility and prevent accidental loss of unrelated entry points while the new widget engine is implemented. These classes are **not registered as Android widgets** and cannot appear as available providers in a fresh launcher list. Source/resource deletion will occur as part of Stage 5 new widgets after call-site consolidation.

## Upgrade caveat
Android does not guarantee the handling of already pinned widgets for a removed provider across all launchers. Pinned old instances may become empty or disappear on upgrade. This is not app data loss, and old widget IDs cannot automatically be mapped to a new provider without launcher cooperation.

## Acceptance gate for final combined APK
1. Fresh install advertises no legacy widget providers.
2. Upgrade from V336 preserves projects, Personal reminders, and challenges.
3. Settings has no deprecated widget picker/preview.
4. New widget family is implemented separately with valid declared `AppWidgetProviderInfo` XML.
5. On-device launcher upgrade/resize/refresh/negative-permission tests pass.
6. Existing Quick Idea and deep links still work.
