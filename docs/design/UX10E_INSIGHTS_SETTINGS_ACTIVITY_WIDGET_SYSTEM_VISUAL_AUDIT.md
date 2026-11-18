# Backlot UX10E — Insights, Settings, Notification Center, Widgets and system states

Date: 2026-11-17
Status: SOURCE-STAGED DRAFT ONLY. No Kotlin/Gradle/Compose compilation, JVM/CI, instrumentation, emulator, Android physical testing, APK, merge, or release.
Stacked parent: UX10D PR #426 head ce47f489d0da54caa016eb7955d277d0556d4559.
Protected stable: owner-accepted V337 P0 at 815ba69775f75b6e5f25bb838c2c17b4f09c203e, unchanged. Its accepted 19 corrective commits / 22 changed paths still need explicit reconciliation.
Roadmap order: Founder approved UX10 now, UX09 later. Multi-device sync remains DEFERRED, not done; no Supabase, RLS, credential, backend, backup/restore, app manifest or account change.

## 1. Source-backed functional inventory and decisions

| Product surface | Existing owner | UX10E presentation improvement | Critical preservation |
|---|---|---|---|
| Settings, search, help and categories | V144SettingsHub / V404SettingsIndex / V167SettingsNavigationPolicy | Original searchable hierarchy is sound; shared gutter, 56dp touch rows, calmer dividers and neutral surfaces | Original Account, Personalization, Reminders, Planning, YouTube, Privacy, Advanced, About destinations, preferences and confirmation gates |
| Director's Cut / Light appearance and launch sound | V127AppearanceSettings / VisualExperiencePrefs | Keep selected theme strong and visible, soften only inactive lines, raise 7.4sp ACTIVE badge to 10sp | Theme IDs, visual assets, sound flag, saved preferences, welcome choice |
| YouTube Insights tabs and metrics | V172InsightsUi / V11YouTubeInsights | Preserve Overview, Content, Revenue, Creator routes; horizontal 108dp+ tab strip on narrow or large-font screens; 56dp targets; 25 formerly under-10sp channel labels raised; neutral card edges quieted | Snapshot, reporting period, zero vs unavailable, creator summaries, historical evidence, YouTube login/refresh and video linking |
| YouTube metric drilldown and Audience | V20InsightsDrilldownUi / V190InsightsUiPrimitives / V20InsightsFoundationUi | Shared neutral outlines only | Loading/Pending/Unavailable/Ready data states, source evidence, metric calculations and toggles |
| Notification Center and Spark actions | V306ActivityCenterScreen / original receipt and reminder readers | A pure policy distinguishes truly empty, still loading and partial read failure; slightly quieter neutral cards, strong delivery issue border kept | Needs Action/All/History filters, refresh, original target IDs and callbacks; NEVER mark Done, Snooze or acknowledge on viewing |
| Home overflow actions | V339HomeOverflowMenu | Neutral trigger border quieter | Existing notification/Spark/gallery/profile/Spaces/weekly/reminder/settings navigation and status indicator unchanged |
| Widgets and previews | V275WidgetPreview / WidgetMigrationActivity / existing RemoteViews and V2273 providers | Read-only widget preview height grows from 174dp to 208dp for large fonts; consistent container/pin button size and soft border | Original launcher pin request and refresh are opt-in; no auto migration, removal, provider or widget layout edit |

### Critical widget product discrepancy

The founder previously requested removing old widgets and planning a replacement widget system. The inspected repository still contains widget providers and a migration screen. UX10E is a VISUAL package, so it does NOT unilaterally delete providers or claim the replacement widget system is complete. UX10F must surface this as an explicit unresolved product/architecture gate, not erase existing launcher shortcuts silently.

### Product principle

UX10 is not a new design language. Director's Cut primary and supported Light appearance, semantic REC-red selected/warning states and existing companion/art remain authoritative. Quiet outlines apply only to neutral surfaces. Destructive, failed delivery, selected, focus and high-contrast states remain visible. Avoid shrinking labels back to 7–9sp to hide layout problems; use adaptive reflow after real Android screenshots.

## 2. Exact source-stage change register

Production UI/policy source (12 paths):
1. NEW ui/UX10EPresentationPolicy.kt — pure viewport policy for Insights tabs, Activity Center loading/failed/real-empty text, and widget-preview container size.
2. ui/V172InsightsUi.kt — original four tab callbacks, scroll on narrow/large-font screens, two-line labels, selected semantics, 56dp touch, neutral card outlines.
3. ui/V144SettingsHub.kt — preserves original settings index and actions, shared 18dp gutter, title, 56dp rows, calm neutral dividers and borders.
4. ui/V20InsightsDrilldownUi.kt — source metric/evidence dialogs unchanged apart from quiet neutral outlines.
5. ui/V190InsightsUiPrimitives.kt — existing disclosure state and callback preserved, quiet border.
6. ui/V339HomeOverflowMenu.kt — original three-dot navigation unchanged, quiet inactive outline.
7. ui/V306ActivityCenterScreen.kt — read-only truthful empty/loading/unavailable copy policy, quieter neutral entries; strong delivery problem border retained, callbacks unchanged.
8. ui/V127AppearanceSettings.kt — active appearance remains red, inactive line muted, tiny ACTIVE label lifted.
9. ui/WidgetMigrationActivity.kt — existing opt-in pin/refresh controls, 56dp action and calm neutral cards; no provider changes.
10. ui/V275WidgetPreview.kt — actual synthetic RemoteViews preview height responsive to font scale, not real private reminder content.
11. ui/V11YouTubeInsights.kt — 25 labels raised to minimum 10sp, nine neutral card outlines quieter, actual data and Google auth paths untouched.
12. ui/V20InsightsFoundationUi.kt — Audience status panel neutral border quiet, original data status source retained.

One NEW test file: app/src/test/java/com/framebynavin/app/ui/UX10EPresentationPolicyTest.kt — 8 authored source tests, NONE EXECUTED.
Two docs: this source/ownership audit and docs/qa/UX10E_INSIGHTS_SETTINGS_ACTIVITY_WIDGET_CERTIFICATION.md with 40 deferred scenarios.

No changes to original data/stores/YouTube engine, Activity Center receipt owners, notification receiver/alarm/snooze and voice/music, widget providers and RemoteViews XML, app manifest, Personal/Creator data, cloud config or accepted V337 baseline.

## 3. Preserved user journey

Home / Settings -> existing Account, Appearance, Reminders, Planning, Privacy, Advanced, About and searchable destinations -> original save/permission/confirmation owner.

Home / Insights -> Overview, Content, Revenue, Creator -> original channel snapshot and exact evidence drilldown -> original video/project link actions.

Home / three-dot menu -> Notification Center -> original Needs Action, All, History -> original Creator or Personal reminder -> original token-based Done/Snooze owner. Merely viewing Activity Center does NOT acknowledge or schedule.

Settings / widget preview -> existing synthetic actual-layout preview -> explicit user pin/refresh requests, never automatic launcher mutation.

## 4. Evidence gaps and release gates

- NO build/test/physical PASS: Compose imports, conditional RowScope weight, widget RemoteViews sizing and read-only copy states require real compilation and Android screenshots. 320dp/393dp/tablet, 1.0/1.5/2.0 font, dark/light contrast, TalkBack, IME, offline and launcher tests are deferred.
- Raising 25 legacy YouTube labels may increase line wrapping. Inspect actual screens before final signoff; do not equate code changes with accessible UI.
- Existing data-health distinctions must remain accurate. A disconnected YouTube account, unavailable Audience source or failed local receipt read must never be treated as a true empty report.
- The old widget removal/new widget system request is separately gated; user confirmation is required before any destructive provider, pinned shortcut or architecture change.
- V337 accepted P0 alarm/restore/backup/Snooze/ledger fixes must be reviewed and incorporated before a trusted integrated candidate. UX09 sync remains deferred and incomplete.
- NEXT: UX10F global audit across all source-staged UX changes, determine remaining screen gaps, reconcile approved design consistency, and prepare the exact proposed integration/testing/release gate. No merge or stable release by default.

Verdict: UX10E source-stage candidate only; authoring test plans is not execution evidence.
