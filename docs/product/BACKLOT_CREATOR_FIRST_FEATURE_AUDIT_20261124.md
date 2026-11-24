# Backlot Creator-First Capability Audit
Date: 2026-11-24
Status: PRODUCT AUDIT / PROPOSED NEXT ROADMAP, NOT NEW FEATURE IMPLEMENTATION
Accepted source: 28a7000159554f254ab1ecc1aecc7792a1385ac2 (matching approved Stage 7 Lab APK application tree)

## North star
Backlot is a Creator Operating System. Content Workspaces are the heart. Reminders and Personal support consistent creative execution and wellbeing, never displace Creator creation and production.

## Maturity scale
HIGH = substantial integrated source and persistence; core strategic asset. MEDIUM = working source-backed functionality with limited interaction depth / certification. WEAK = narrower, early, unverified or Lab-disabled capability. This is a repository implementation and product-quality assessment, not a pass verdict for 33 physical device suites.

## Ranked current Creator capabilities
| Rank | Capability | Tier | Why |
|---:|---|---|---|
| 1 | Content Workspace | HIGH | Actual hub routes Plan, Script, Publish, Stage, Output, Board, Learning and Blueprint from one canonical project. Needs better UI cohesion. |
| 2 | Workflow stages & creator templates | HIGH | CreatorWorkflowV2 and stage policy; explicit advances separate from publication. |
| 3 | Plan/Brief/Research/Assets | HIGH | Plan editor with audience, problem, promise, angle, optional objectives, sources, assets and task checklists. |
| 4 | Script Studio | HIGH | Hook/title ideas, beats, narration, visual notes, guarded revisions and publish handoff. |
| 5 | Idea Vault & durable Idea-to-Project | HIGH | Search, statuses, editing, voice, restore and linked project conversion. |
| 6 | Project lifecycle | HIGH | Full project wizard, presets, ongoing project list, edit, archive, unarchive and delete. |
| 7 | Publish Studio & multi-output ledger | HIGH | Status/readiness, title/cover/description, actual creator-entered publication event, update/reopen; not direct platform posting. |
| 8 | Local draft integrity & recovery | HIGH | Revision, epoch, journaling and safe editor-switch guards; not proof of remote sync integrity. |
| 9 | Today / Continue / Up Next | MEDIUM | Real project priority, stage and deadline entry; needs fewer clicks to Workspaces. |
| 10 | Stage Desk evidence & decisions | MEDIUM | Stage notes, tasks, timecodes, linked proof/beat, review, manual explicit save. |
| 11 | Quick Capture + voice idea capture | MEDIUM | Voice/text, optional notes, local draft, saved Idea Vault ownership; external share capture needs more verification. |
| 12 | Output Craft & cut versions | MEDIUM | Output-specific derivative scripts, independent histories, review notes, file/URL references. |
| 13 | Connected Creative Board | MEDIUM | Filtered read-only canonical source/asset/stage/beat/output cards with deep links, not a freeform canvas or media library. |
| 14 | Output Learning Desk | MEDIUM | Output-owned append-only user reflections and manual metrics, draft recovery; weak automation. |
| 15 | Content Blueprint | MEDIUM | Content-type/format guidance generated from canonical project facts, mainly guidance/read-only. |
| 16 | Focus sessions | MEDIUM | Project-linked timed sessions with interruption handling; no native editing. |
| 17 | Weekly Plan and 14-day Auto Plan | MEDIUM | Recurring creator templates, opt-in draft project creation and background execution; no automatic posts. |
| 18 | Content Calendar | MEDIUM | Week/Month/Agenda views of planned project/recurring commitments, explicit planned vs published facts. |
| 19 | Daily Brief and Quick Actions | MEDIUM | Source-backed priorities, overdue controls and direct creator destination routes; cockpit could be simplified. |
| 20 | Publication checkpoints and Video Review | MEDIUM | Post-publish actions and evidence-based review, dependent on linked creator/project metrics. |
| 21 | YouTube Insights + video-project linking | MEDIUM | Real read-only YouTube authorization/API clients, trends, uploads, linking; actual Lab account OAuth not recently certified. |
| 22 | Creator rewards/progress | MEDIUM | XP, achievements, momentum and history; support tool, not core workflow. |
| 23 | Creator profile, modes, goals, weekly target | MEDIUM | 5-page creator setup and profile config; automatic app tour gap is separate. |
| 24 | Consent-based device Google Calendar export | MEDIUM | CalendarContract export to chosen Google calendar, not full Google Calendar sync. |
| 25 | Creator reminders and routine nudges | MEDIUM | Strong supportive execution system, but not Backlot's heart and full device matrix pending. |
| 26 | Spark creator context/shortcuts | MEDIUM | Home companion can route creators; not yet a deeply context-aware production copilot. |
| 27 | Release Day / timely-post quick response | WEAK | Idea and post planning utility, standalone depth limited. |
| 28 | Creator Brain, Opportunity & Playbook scoring | WEAK | Deterministic pattern/recommendation engines exist; real end-to-end quality/utility still unproven. |
| 29 | Gemini AI Video Autopsy | WEAK | Provider explicitly disabled for BuildConfig.BACKLOT_LAB; not a functioning Lab feature. |
| 30 | Monetization/revenue analytics | WEAK | Optional YouTube monetary scopes and UI; real Lab completeness and reliability not verified. |
| 31 | Rich inline Workspace media experience | WEAK | File and URL references exist; no embedded video editing or full media preview/annotation workflow. |
| 32 | Automatic new-user guided tour | WEAK | 6-lesson policy and Settings > About > Show app tour exist; owner says automatic first-time tour absent. |
| 33 | Full cross-device Creator sync/conflict reconciliation | WEAK | Cloud snapshot/recovery components exist; two-device conflict and restore safety not owner-certified. |

## Deferred known issue
The six-lesson app tour does not automatically launch for new users per owner feedback. Manual Show app tour route exists in Settings > About. Investigate eligibility and first-run/returning-user flags later, independently from the separate five-page Creator setup.

## Proposed next direction (no code changes authorized yet)
P0: Audit actual first-launch Creator information architecture and one genuine idea-to-publish user journey on signed stable source.
P1: Make Project Workspace the unmistakeable center: one Continue Production call to action; calm layers of Plan, Script, Stage Evidence, Creative Board, Output Craft, Publish and Learn; coherent visual language and truthful draft/saved/published states.
P2: Upgrade creator assets/reference experience; stay focused on production documentation, not an in-app video editor.
P3: Unify Today, Weekly Plan and Calendar around actual deadlines, dependencies and capacity.
P4: Close publication-to-evidence-to-learning loop using official/read-only YouTube data when authorized, user-entered reflections otherwise. AI remains optional and founder-approved only after accurate, costed validation.

No production API writes, paid services, database migrations, app-source modifications, new APKs or Play release from this product audit. Stable branch and owner APK remain the source of truth.

## Key audited code
- ui/ContentWorkspaceActivity.kt; ui/V19ContentWorkspaceAlpha3Ui.kt; ui/V19ContentWorkspaceAlpha4Ui.kt; ui/V19ContentWorkspaceAlpha5Ui.kt; ui/V19ContentWorkspaceAlpha6Ui.kt
- ui/CW3StageDesk.kt; ui/CW4OutputCraftDesk.kt; ui/CW51CreativeBoard.kt; ui/CW52OutputLearningDesk.kt
- ui/V09IdeaVaultUi.kt; ui/V20NewProjectWizard.kt; ui/V18TodayScreen.kt; ui/V08WeeklyScheduleUi.kt
- ui/UX08ECalendarNavigationUi.kt; ui/V11YouTubeInsights.kt; ui/V20OpportunityEngineUi.kt; ui/V144SettingsHub.kt
- data/CreatorContentWorkspace.kt; data/CreatorWorkflowV2.kt; data/CreatorContentBlueprint.kt; data/CreatorOpportunityEngine.kt
- ai/CreatorGeminiVideoAutopsyProvider.kt (Lab-disabled).
