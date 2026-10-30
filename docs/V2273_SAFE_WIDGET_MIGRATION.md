# Backlot V227.3 — Safe widget migration, no silent removal

## Root cause and Android platform constraint
Backlot has 3 new V224-series widgets, but nine V197/legacy providers remain registered, because users may have pinned them before the redesign. A manifest-only removal, class rename or disabling a provider can make launcher widgets disappear, display an error or stop receiving updates. Android does not provide a cross-launcher transactional API for automatically swapping every old pinned widget with another provider while preserving its launcher position. The presence of a provider in the launcher picker is separate from its installed instances, and any OEM-specific picker hiding is too risky for a safe release.

## V227.3 foundation
- Add **Settings → Personalization → Home screen widgets** entry, opening non-exported WidgetMigrationActivity.
- Read current AppWidgetManager pinned IDs for the three new providers and nine legacy providers, showing grounded counts only (no idea, project or Personal reminder content).
- Offer opt-in Android `requestPinAppWidget` for each new widget where the launcher supports it; make it explicit that a successful *request* is NOT a pin placement until the user approves launcher confirmation.
- For launchers without direct pinning, show manual Home → Widgets → Backlot instructions. No root/overlay, accessibility automation, broad system permission, launcher settings mutation or confusing fake success.
- Clearly separate the steps: add new widget, verify real data/actions/size, then long-press the old widget on Home and Remove. Removing a widget from a launcher is not removing app data. Some specialized old widgets do not have one-to-one successors; communicate that before manual removal.
- Maintain all nine original AppWidgetProvider components, XML descriptors, PendingIntent actions and widget update code unchanged. No automatic old deletion, provider disable, DataStore writes, remote sync or AppWidgetHost.deleteAppWidgetId.
- Provide pinned-count refresh on return from launcher and surface failure conservatively (unknown rather than zero).
- Full versionCode 252 signed development APK to install on top of V227.2 versionCode 251, same package/sideload signer.

## Validation
- Pure JVM tests: all four migration advisory states, truthful pin-request feedback, exact pin counts.
- Android 11/15: Compose launch UI has 3 functioning buttons with in-memory callbacks; unavailable launcher disables pin and explains manual path. Existing V224 provider registry instrumentation ensures original providers remain discoverable; full 56-target prior UI/backup/reminder suites retained.
- Source-safety gate ensures nine provider registrations stay intact and no `setComponentEnabledSetting`, `AppWidgetHost.deleteAppWidgetId`, automatic widget destruction or Personal/Creator data export is introduced.
- Manual owner QA: add each new widget, return to refresh counts; test resizing/actions and privacy; only then remove old widgets manually on Home. OEM launcher detection may be incomplete; no claim of automatic migration.

## Remaining issue
An app cannot safely hide old providers from every widget picker while keeping all their pinned instances running. After user acceptance, consider a separate, carefully gated legacy-provider sunset or support migration path; do not fake an Android capability that does not exist.
