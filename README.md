# FrameByNavin Android

Native Kotlin + Jetpack Compose creator command-center app.

## V0 Functional milestone — 0.2.0

The approved cinematic editorial visual direction is frozen for V0. This milestone makes the shell interactive:

- Working bottom navigation: Today / Plan / Studio / Insights
- Quick Add from the + button
- Persistent local task storage with Android DataStore
- Start and complete task actions
- Production pipeline advancement in Studio
- 25-minute Focus Mode with pause/resume and Done
- Local progress insights
- Scroll-safe Today layout so lower cards are reachable on real devices
- GitHub Actions APK build

The next engineering milestone is the reliability stack: exact reminders/alarms, permission-state handling, reboot recovery, acknowledgement/snooze actions, and escalation rules. Core production data will move to Room/SQLite as that domain layer is introduced; DataStore is used in this V0 interaction milestone to keep the first functional loop small and testable.
