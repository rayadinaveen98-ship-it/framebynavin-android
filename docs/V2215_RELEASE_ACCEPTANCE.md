# Backlot V221.5 — release acceptance matrix

Status: **development only; not owner-released**. V221.5 is stacked on V221.4 in PR #147, which is stacked on V221.2. The user explicitly skipped V221.3 premium pose artwork. Do not merge its unapproved 2D vectors.

## Evidence-based gates

| Area | CI acceptance | Physical-device gate |
| --- | --- | --- |
| Companion evidence | Read-only, actual saved tasks/ideas and current unanswered acknowledgements only; no inferred delivery | Create and finish projects, change ideas, confirm silence when there is no eligible evidence |
| Home priority | Real Top 3 above Up Next; actual Next Move and Reminders Hub above Companion; Weekly Focus remains below | Confirm first viewport at real One UI resolution and system font |
| Companion choice | Original Frame/Navi/Cute V217 illustrations; Spark historical WebPs untouched | Existing saved selection survives data-preserving upgrade |
| Settings path | **Settings → Personalization → Companion** actually opens V2215 switch in live `V144SettingsHub`; no reliance on orphaned `PSettingsScreen` | Navigate from real Settings using keyboard, gestures and screen reader |
| Opt-out | Root owns persisted preference and passes it to both Home and real Settings; Home hides **immediately**, not after 60-second refresh | Turn off, return to Home, force-close/relaunch, return to Settings, re-enable; reminders must continue |
| Dismissal | Not now hides the whole card for this Home visit; local 12h bounded suppression remains | Confirm it does not delete/change project, reminder, Cloud or companion identity |
| Action routing | Project ID revalidated against latest active tasks; Ideas goes to Idea Vault; Reminders goes to real Hub | Confirm correct selected project opens and back navigation is intact |
| Accessibility | Minimum 48dp action controls, wrap at large font, no primary message truncation, descriptive TalkBack control labels | Screen reader focus order, 1.0x / 1.5x / 2.0x fonts, device navigation bar/IME |
| Build safety | Full JVM, debug + personalStable + AndroidTest APKs, actual emulator suite/screenshot evidence | Validate matching version/code, certificate and Android package for in-place upgrade; never uninstall populated app |
| V220.4.1 known issues | Keep verified save footer and canonical backup checksum changes intact through stacked history | Real **Save Idea** with keyboard open, gesture nav and Telugu title; local export; cloud **Sync Now** and validated restore/recovery with no destructive overwrite |

## Real-app Settings RCA — fixed in V221.5

V221.4 previously added the companion switch to unused private `PSettingsScreen`; the actual route was `POverlay.SETTINGS -> V144SettingsHub`. This made the prior toggle unreachably optimistic. V221.5 removes the dead switch, adds `V2215CompanionControls` to the active `V167SettingsSection.GUIDE` path and shares one Activity-owned preference state with Home. Automated integration coverage must target the actual `V144SettingsHub`, not a standalone toggle preview alone.

## Owner device checklist (do not mark complete until observed)

- [ ] Capture currently installed Backlot versionCode and signing certificate; confirm compatibility before updating.
- [ ] Safeguard an existing local export and verify all saved projects, ideas and recordings remain accessible.
- [ ] Install compatible personally signed non-debuggable candidate **in place** without clearing data.
- [ ] Check New Idea Save CTA at default font, enlarged font and with keyboard displayed, using gesture navigation.
- [ ] Confirm YouTube Top 3 appears above Up Next only for genuinely connected data.
- [ ] Enter real Settings → Personalization → Companion; turn suggestions off and on; verify immediate Home effect and persisted preference.
- [ ] Change guide to each non-Spark identity and back to original Spark without changing/removing any artwork or settings.
- [ ] Validate TalkBack focus order, touch targets, content under navigation bar and no clipping at 200% font size.
- [ ] Validate local backup, non-destructive cloud integrity check and user-controlled recovery. Never skip checksum/tamper validation or overwrite old cloud snapshot.
- [ ] Verify ongoing alarm, voice, notification, and unanswered-reminder behavior separately.
- [ ] Record owner signoff and a recoverable rollback plan before any `main` promotion.

**No mobile-owner/device acceptance or real cloud-account test has been completed simply by passing CI.**
