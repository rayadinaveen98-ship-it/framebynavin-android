# UX10F — Backlot global visual consistency / real-screen audit

Date: 2026-11-17
Current: SOURCE-STAGED design audit only; NOT a physical visual certification. Theme identity and all original source owners preserved.
Parent: UX10E PR #427 head 411b733dae308dcd3948d99c39faf840d443d059.
Protected owner-accepted V337 P0: 815ba69775f75b6e5f25bb838c2c17b4f09c203e; do not merge or change.
Owner-requested UX09: intentionally postponed, NOT complete.

## 1. Core design language accepted across UX10

Existing Director's Cut + Backlot Light palettes, cinematic REC-red emphasis, ivory text, MutedGold helper labels, Spark/Mira and approved welcome screens are canonical. UX10 introduces ONE set of complementary shared design tokens, not a new third theme or mass theme migration:

- 18dp standard page gutter / 16dp compact, 16dp section gap, 6dp row rhythm.
- 16dp primary cards, 14dp rounded surfaces, 12dp compact chips.
- 23sp page title, 14sp section, 13sp body, 11sp support and a 10sp minimum eyebrow for migrated labels. These are scalable SP, not a forced maximum.
- At least 48dp controls, 56dp typical navigation/action rows. Long titles can wrap; small screens should stack cards/actions rather than shrink text to 6.5sp.
- Quiet neutral card hairlines must remain subordinate to text. Stronger red/secondary selected, warning, error, focus and destructive affordances MUST NOT be stripped out globally.
- Original app/system insets, visibility state, accessibility focus and reduced-motion support stay authoritative.

## 2. Actual source-owner continuity matrix

| Area | Staged UX stage/source surface | Original destination and state to preserve | Outstanding test |
|---|---|---|---|
| Welcome and launch | UX10B, V131/V401/V199 untouched | Original four selected welcome styles, reduced-motion static variant, external notification/widget deep-link bypass | First-run, actual three-second animation and reduced motion |
| Onboarding / account | UX10B first-run focus/Google/local/Personal first | Saved selected route, no automatic permission grant or forced login | 320dp/2x font and process recreation |
| Home / navigation | UX10B V18TodayScreen, original PBottomNav | True Up Next tasks and Companion, same Continue/Focus IDs, original Home/Ideas/+Projects/Insights, Back and top menu | nav clearance, TalkBack icon-only labels |
| Project creation / stage | UX10C V20/V173/CW3 | Original quick/seven-step validation, stage receipts, stage writer, draft and evidence ownership | IME, revision conflict, accidental duplicate or XP |
| Idea Vault | UX10C V09/V150/V227 | Original ideas/linked project IDs and voice draft, archive/restore/convert/search | badge FlowRow, 200% font, not truncating actions |
| Workspace Plan/Script/Publish | UX10C Alpha3/4/5/6 + adaptive frame | One canonical Task/workspace, original save/revision approvals, 4 Plan/Script/Publish/Guide tools | project link, media retention, output status |
| Reminder experiences | UX10D V2232/V101B/V157/V406, UX06 upstream | Personal/Creator distinct recipients, original Done and Snooze tokens, voice/music, scope and 4-step composer | actual Android 15 full-screen sound/lockscreen/notification |
| Personal / Spaces / photos | UX10D V289/V290/UX07C | Private Room/Space records, true 3 source metrics, metadata-stripped local photos, no private cloud leakage | 320dp/2x font, recovery and journal privacy |
| Reminder Groups / Routines | UX10D V298/V295/UX07B | Original groups, link guards, Confirm Pause/Resume, repeat/voice actions | safe confirm and disabled busy/read-error controls |
| 21-day Challenge | UX10D Phase1HabitShared/V297 | Original 21 actual dates/confirmed Done, no false progress, source-based alarm/Snooze | real scheduled 10-minute event and reboot/OEM idle |
| Progress Journey and Achievements | UX08B/C and UX10A | Real events/ledger, celebrate selected Spark/Mira after genuine saved XP once | historical read false-empty/XP duplicate/animation timing |
| Daily Brief, Calendar, Quick Actions | UX08D/E/F, UX10A | Real source task IDs, due events, reminder actions, only confirmed edits | weekly loaded/error truth and Skip owner race |
| YouTube Insights | UX10E/UX10F V11/V172/V20 | Correct channel/period/metric IDs; 4 original tabs, 4 YouTube and 5 Creator metrics with unchanged click targets | report availability, font 2x, metric card reflow, no false numbers |
| Settings | UX10E V144/V127 | Original search index, theme and sound, all profile, reminder, privacy, advanced and erase gates | dark/light and destructive confirm |
| Notification Center | UX10E/F V306 | Real Creator read, Personal events, Needs Action/All/History, refresh, Spark exact source; failed read is not empty | read failure and loading true/false states, no receipt writes |
| Widgets and home overflow | UX10E V275/WidgetMigration/V339 | Original existing widgets, explicit launcher pin, synthetic previews; no user data in preview | OEM launcher, no automatic deletion/migration |
| Future automatic sync | UX09 DEFERRED | Local-first, per-account sync and secure RLS still requires separate approval and implementation | CANNOT certify until UX09 implemented |

This matrix identifies critical source owners and major routes. It does NOT prove every one of the ~220 UI Kotlin files has had a pixel-by-pixel or accessibility audit, nor imply all deep screens are finished visually. More specialized overlays, media pickers, recovery/debug routes and extremely large-content/device states still require on-device sampling.

## 3. UX10F targeted consistency corrections

- Existing legacy YouTube metrics were still in fixed two-column rows despite 25 label-size increases in UX10E. UX10F introduces UX10FContinuityPolicy and renders the same four original metrics with 1 column under 400dp or system fontScale >= 1.35, otherwise two columns.
- Existing Creator Insights had five real metrics in fixed paired rows. Same policy reflows them without changing their exact source calculations or original PUBLISHED/ACTIVE/FINISHED/CONNECTED/IDEAS drilldown kinds.
- The original V306 local Creator acknowledgement journal was read synchronously inside remember/runCatching; if the result is null, that is a failed read, not a forever-loading background request. UX10F instructs the existing read-only empty policy to show unavailable and keep the existing read-error warning; it does not acknowledge an alert.
- Actual saved dates are now included in 21-day grid TalkBack descriptions; index/confirmed/today, source status and clickable day detail stay original.

## 4. Design stop-the-line inventory

**P0 source/data truth:** Never silently change project, idea, reward ledger, Personal, Challenge, Space, Account or reminder/backup owners. Preserve accepted V337 corrected Snooze, missed Android channel, restore forensics and 19-commit/22-file path inventory. UX09 full two-device sync is unimplemented; account copy must not promise it.

**P1 navigation and readability:** At 320dp and system font scales 1.5/2.0, side-by-side fixed-size metric rows, Canvas/week grids, editor footer, top tabs, popup menus and nested bottom sheets may clip. Real-device screenshots/TalkBack should guide changes rather than assume 10sp makes a surface accessible.

**P1 states and high contrast:** Read failures, active reminder, source-unavailable metrics, Settings permission warnings, Photo privacy, selected theme/Creator focus, group confirmation and disabled actions must remain distinct in BOTH themes. Quiet hairlines may become too subtle in bright light; focus rings still need differentiation.

**P1 motion and insets:** Welcome and reminder cinematic visuals/companion celebrations require reduced-motion handling, listener cleanup, TalkBack ordering, correct status/navigation bars, Android 15 Edge-to-edge and lockscreen/notification action tests. A static Compose source inspection cannot certify these.

**P2 product decisions:** Old widgets present despite request to replace; Personal achievement ledger still needs product approval; final Supabase migration/cost/real-time sync remains UX09. Do not sneak these into UX10F.

## 5. Final owner-facing verdict

UX10F finishes the planned **source-level UX10A–F visual consistency wave**, subject to exact draft PR verification. It does NOT complete UX09, satisfy the deferred combined campaign, reconcile V337 or authorize APK promotion.

Next engineering work must be separately gated: all-10-point integration including UX09; preserve accepted V337 P0; build and run complete JVM/Android/real-device tests on an exact reviewed SHA; inspect every critical screen in dark/light, compact/wide, 1x/2x, offline/error/empty, high contrast and reduce motion; owner certifies the physical APK; only then discuss stable and publishing.
