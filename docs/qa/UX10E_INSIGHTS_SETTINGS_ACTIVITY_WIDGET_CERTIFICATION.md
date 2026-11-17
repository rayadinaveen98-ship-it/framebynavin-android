# Backlot UX10E — Settings, Insights, Notifications, Widgets and System States certification

Date: 2026-11-17
Status: DEFERRED ACCEPTANCE PLAN — 0/40 scenarios run; 0/8 new source unit tests run. No Gradle compile, JVM/CI, Compose UI, emulator, physical APK, merge or release.
Exact parent: UX10D PR #426, head ce47f489d0da54caa016eb7955d277d0556d4559.
Protected owner stable: V337 P0 at 815ba69775f75b6e5f25bb838c2c17b4f09c203e, unchanged. 19 accepted corrective commits/22 paths still require release-candidate reconciliation.
Founder order: UX10 now; UX09 automatic multi-device synchronization DEFERRED and incomplete.

| ID | Severity | Scenario | Pass evidence |
|---|---|---|---|
| E01 | P0 | Open original Settings from Home and Quick Actions | Exactly original screen/routes, no altered state |
| E02 | P0 | Settings search account/theme/voice/permissions/advanced | V404 index points to real existing destination; no destructive direct action |
| E03 | P1 | Settings search no matches / clear | Honest state, typing resets results, 320dp+ accessibility |
| E04 | P0 | Settings Account and Google status | Same original account session, local option, no token or credential leak |
| E05 | P0 | Appearance switch Director's Cut and Backlot Light | Original saved theme preference/legacy normalization, stable brand assets |
| E06 | P1 | Appearance ACTIVE badge at font 2.0 / 320dp | Readable >=10sp, visible red selected border, no clipping |
| E07 | P0 | Appearance welcome style and launch-sound option | Original saved choice and original consent/audio toggle |
| E08 | P0 | Reminders, Planning, Privacy, Advanced and About Settings routes | All original controls, zero missing buttons |
| E09 | P0 | Settings erase/backup/restore/account deletion guarded controls | No alternate destructive shortcut or P0 regression |
| E10 | P1 | Settings rows dark/light, 320dp/tablet/2x font | Consistent 18dp gutters, >=56dp rows, contrast / TalkBack |
| E11 | P0 | Insights Overview/Content/Revenue/Creator tab selection | All four original views and same onSelect callback |
| E12 | P1 | Insights 320/393dp normal font | Scrollable 108dp+ tabs, readable labels, accessible last tab |
| E13 | P1 | Insights wide normal font | Four aligned equal-width tabs, no unnecessary scrolling |
| E14 | P1 | Insights tablet and 1.35–2.0 font | Scrollable readable tabs, active state announced |
| E15 | P0 | Insights active channel and reporting period | Correct original identity, no cross-channel stale report |
| E16 | P0 | YouTube signed out, consent denied, credentials expired | Original correct sign-in/error/retry actions and no fabricated data |
| E17 | P0 | Analytics pending/unavailable/empty/ready | Distinct truthful states, missing values not displayed as zero |
| E18 | P0 | Content catalog loading and pagination | Original source loading/error, video links and detail routes |
| E19 | P0 | Revenue unconfigured / available | No invented revenue or unsupported monetization claim |
| E20 | P0 | Creator progress source metrics | Exact tasks/ideas/linked videos, no false Creator XP or publish |
| E21 | P1 | 25 raised YouTube source labels at font 2.0 | Legible and wrapped, no overlap/tiny caption regression |
| E22 | P1 | Insights delta cards, Audience panel, trends, signals and drilldowns | Quiet neutral chrome, accurate selected metric and source evidence |
| E23 | P0 | Notification Center launched from Home menu / reminder route | Same real Activity Center, no duplicate center |
| E24 | P0 | Needs Action / All / History filters and refresh | Original filter/read/refresh semantics and saved-state status |
| E25 | P0 | Activity read sources still loading | Loading language, NOT false 'No recorded activity' |
| E26 | P0 | Personal receipt or original Creator read failed | Unavailable/failed state, no fabricated zero count or hidden error |
| E27 | P0 | All sources genuinely loaded but no events | Correct filter-specific true empty message |
| E28 | P0 | Delivery problem and permission warning | Strong error/REC-red border, true source IDs, review opens original reminder |
| E29 | P0 | Spark next-action card | Original suggestion exact target ID, never automatically mark Done or Snooze |
| E30 | P0 | Open Personal/Creator Activity event | Original respective reminder route, no cross-domain privacy leak |
| E31 | P1 | Notification Center at 320dp/2x font/TalkBack | Filters scroll, refresh and every row accessible, error not visually suppressed |
| E32 | P0 | Home three-dot menu Notification/Spark/Gallery/Profile/Spaces | Original options, state badge and saved companion retained |
| E33 | P0 | Home overflow notifications, reminders, weekly and settings | No missing original route or accidental action |
| E34 | P1 | Widget preview normal / large font | Actual synthetic RemoteViews contained at 174dp/208dp, no personal titles |
| E35 | P0 | Existing pinned widget and launcher action | Unchanged providers, RemoteViews XML, shortcuts, pending intents |
| E36 | P0 | Widget pin request on unsupported/supported launcher | Only explicit opt-in, no silent replacement or installation |
| E37 | P0 | Widget refresh/pinned-count display and legacy guidance | Correct launcher-owned counts; no automatic deletion |
| E38 | P1 | Widget gallery at 320dp/tablet/light/dark | Readable label, >=56dp pin control, no overdraw/clip |
| E39 | P0 | V337 accepted P0 alarm/snooze/restore/backup and UX09 boundary | All 19 correction commits/22 paths reconciled; no false sync promise |
| E40 | P0 | Integrated candidate exact source SHA + APK hash + owner physical signoff | Real JVM, CI, instrumentation and device evidence before stable authorization |

## Authored tests but 0 executed

app/src/test/java/com/framebynavin/app/ui/UX10EPresentationPolicyTest.kt has 8 deferred tests for narrow/wide/high-font/invalid Insights tab layout, distinct true-empty/partially loaded/failed Activity Center copy, and widget preview sizing at normal/large/invalid font scale.

No screenshot, talkback, Android phone/tablet, launcher pinning, OAuth or notification receipt test has actually been run. Unit policy tests are not an end-to-end pass.

## Must not proceed to release if

- Any Settings route or account/deletion/restore permission confirmation is lost or bypassed.
- YouTube reports unavailable metrics as real zeros, leaks another channel, changes source owner or creates a video/project automatically.
- Notification Center reads or views fabricate acknowledgements, hide delivery failures or cross Personal/Creator privacy boundaries.
- Widget provider changes remove installed shortcuts, perform pin/delete without consent, or preview real private reminder titles.
- Small/light-theme/a11y controls clip at 200% text, or selected/warning focus becomes invisible after neutral-border reduction.
- V337 accepted P0 corrective lineage remains divergent, UX09 is advertised as complete, no physical APK or owner authorization exists.

Current: UX10E source stage only. Next UX10F global consistency audit, integration conflict gates, then full owner-approved build/test path. UX09 is still postponed.
