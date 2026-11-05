# Backlot V294 — Evidence-based Audit Findings & First Copy Decisions

**Source baseline:** V292 signed owner preview, commit `69c9e34e66910a871a63853f99c23e716ba0e381`.  
**Audit gate:** [CI run 41967695851](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/41967695851) — **passed**, 6 scanner tests.  
**Scope:** read-only static review. All counts below include potentially dormant older components. This is not a screenshot-verified live screen audit. No installed app or user data changed.

## Measured baseline

| Item | Count | What it means |
|---|---:|---|
| Production Kotlin files scanned | **415** | Full app source, not 415 live screens |
| XML files scanned | **38** | Main resources and manifest |
| Candidate user-visible strings | **2,666** | 2,098 direct UI-call candidates + 568 possible UI strings |
| Font declarations **below 12sp** | **1,061** | Explicit small-text declarations across source, including inactive files |
| Font declarations **below 11sp** | **700** | Strong accessibility audit targets, not necessarily 700 visible labels |
| Long-copy candidates over 170 chars | **26** | Many involve important safety or recovery explanations |
| Potential demo/preset/seed source references | **307** | Source symbols, *not* 307 persisted demo records |
| Composable function declarations | **401** | Many are reusable widgets / older implementations, not routes |
| Activity classes | **13** | Android entry points with some invisible/internal routes |
| Source-traced production destinations | **51 entries** | Inventory in companion route register; screenshot verification outstanding |

Generated, repeatable report includes `copy-candidates.csv`, `source-file-summary.csv`, `microtext.csv`, `sample-source-review.csv`, `composable-and-activity-index.csv`, `long-copy-review.csv`, and `summary.json`. **Neither code credentials nor unclassified arbitrary source literals are emitted to the reports.**

### Priority concentration (raw candidate counts per source file)

| Screen/component | Candidate copy | Under-12sp declarations | Decision |
|---|---:|---:|---|
| Universal Settings `V144SettingsHub.kt` | 107 | 25 | P0: user-neutral IA and reduce helper text |
| Personal hub `V2232PersonalReminderHub.kt` | 96 | 54 | P0: simplify My day and reminder setup |
| My Spaces `V289PersonalSpacesScreen.kt` | 95 | Source report | P0: discoverability, progress and backup copy |
| Ideas `V09IdeaVaultUi.kt` | 77 | 51 | P0: remove duplicated labels and tiny controls |
| Today `V18TodayScreen.kt` | 47 | 43 | P0: action hierarchy and disclosure progressive reveal |
| Cloud Sync `CloudSyncActivity.kt` | 46 | Source report | P0: keep all accurate recovery limitations |
| Project wizard `V20NewProjectWizard.kt` | 56 | Source report | P0: simplify field group structure |
| Insights `V11YouTubeInsights.kt` | Source report | 34 | P1: all metrics must be real, unavailable when disconnected |

Some versioned files (such as `V19ContentWorkspaceAlpha5Ui.kt`) score high for copy density but may be historical. **Do not bulk-redesign until root call graph confirms reachability.**

## Wave 1 — exact copy decisions requiring review

These are **proposed**, not merged. Keep source locations and runtime states linked to the audit catalog. `DROP` means remove redundant narration, never a critical permission or data-loss consequence.

| Source | Current text | Decision / candidate | Why |
|---|---|---|---|
| Settings, around line 507–514 | `Backlot, your way` plus `Choose what you'd like to change.` | **DROP** intro; one `Settings` heading | Duplicate instruction |
| Settings, around line 518 | `My Profile` plus live creator details | **MOVE** profile into `Account` | Settings should be universal |
| Settings, around line 525 | `Personalization` (appearance + Spark + voice) | **RESTRUCTURE** to `Appearance` and `Notifications & alarms` | Voice defaults aren't theme |
| Settings, around line 528 | `Reminders & Alerts` + condition sentence | **RENAME** `Notifications & alarms`; a concise status only if action required | No huge description |
| Settings, around line 535 | `Planning & Calendar` | **RENAME** `Planning & integrations` | Contains more than calendar |
| Settings, around line 541 | `Backup & Data` | **RENAME** `Backup & storage` | User-neutral |
| Settings, around line 410 | `Keep your Backlot data recoverable` | **REMOVE** blanket guarantee; `Backup & storage` | Current Spaces/private photo backup incomplete |
| Settings, around line 119–120 | `Add Quick Capture, Today Focus and My Day; safely replace pinned old widgets` | **SHORTEN** `Manage home-screen widgets`; show migration warning on relevant action | Too much on a row |
| Settings, around line 132 | `These choices change how Backlot looks and sounds. Existing projects stay unchanged.` | **DROP** | Ordinary settings are reversible and self-describing |
| Settings, around line 149 | Long Companion suggestion explanation | **MOVE** exact behavioural explanation to Companion details | Primary screen too text-heavy |
| Settings, around line 404 | Long Auto Plan + Calendar explanation | **MOVE** under Auto Plan enable confirmation and Calendar permissions | Permission details at decision |
| Personal, around line 185 | `My day` | **KEEP** | Clear page title |
| Personal, around line 188–198 | Icon-only `Personal Spaces` | **REPLACE** with labelled `My Spaces` | Actual user could not discover feature |
| Personal, around line 251 | `Keep your day simple. Customize every detail here.` | **DROP** | Empty narration |
| Personal, around line 416–423 | `Nothing to remember yet` + multiple supporting sentences | **SHORTEN** `No reminders yet` and `Add reminder` action | Clear empty state |
| Personal, around line 746–783 | `Your personal rhythm`, numbered sections, large intro | **REDUCE** to `New reminder`, clear time, repeat, alert groups | Less ceremony |
| Personal, around line 969–972 | Android fullscreen fallback explanations | **KEEP** the truth, but conditionally display only when eligibility changes | Important risk but not always |
| Spaces, around line 257 | `Your habits, skills and little wins — in one place.` | **DROP** | Unnecessary on populated screen |
| Spaces, around line 274–280 | `Make room for your progress` + long empty text | **SHORTEN** `No spaces yet`; `Create space` | Single purpose |
| Spaces, around line 320 | `START ANOTHER SPACE` | **RENAME** `New space` | Clear and compact |
| Spaces, around line 335–338 | `YOUR JOURNEY` and `One step at a time.` | **DROP** generic filler; display the actual goal or nothing | Never invent motivation |
| Spaces, around line 392–402 | `N logs` / `Your recorded activity`; `N active days` / `Past 7 days` | **DROP** first subtitle; **KEEP** time window | Metrics with useful context only |
| Spaces, around line 422–426 | `Activity`, `Only things you explicitly record appear here.`, local-only note | **DROP** redundant sentence; keep a **short device-only alert** in Backup and Photo entry | Safety disclosure still required |
| Spaces, around line 828–832 | `Log what you did. Optional measures are saved only when you choose.` | **DROP** intro; keep field `What did you do?` | UI self-describing |
| Spaces, around line 526–538 | Photo privacy + original source + no backup details | **PRESERVE** all three material facts, structured near save confirmation | Cannot sacrifice privacy for minimalism |
| Spaces, around line 606–610 | Metadata export has no photos and is plaintext | **PRESERVE** plaintext/non-photo warnings | Data loss risk |
| Ideas, around line 149–150 | `Ideas` / `Save it now. Make it later.` | **DROP** second sentence | Header only |
| Ideas, around line 228–232 | `Your vault is empty` + sentence | **SHORTEN** `No ideas yet` / `Add idea` | Remove empty-state poetry |
| Ideas, around line 472–474 | `New idea`, `Save your idea`, `Write or record an idea…` | **CONSOLIDATE** one title, one first field | Eliminate repetitive instructions |
| Today, around line 491–539 | `NEXT MOVE`, `WHY THIS MOVE`, `SIGNALS`, extra details | **MOVE** rationale to optional Details | Focus on actionable next move |
| Today, around line 353–368 | Repeated weekly focus metrics and support strings | **MERGE** progress presentation | Avoid multiple competing summaries |

## Demo and default-data provenance — *do not delete blindly*

1. **Current clean install:** `WeeklySchedule.defaultSlots() = emptyList()` and `WeeklyScheduleStore.loadOrSeed()` explicitly avoid a user-specific seeded content schedule.
2. **Historical untouched demo:** `V230LegacyStarterProjectPolicy` can recognize the **exact original full-value** `Frame Breakdown` record; it intentionally preserves edited or ambiguous records. Retain this conservative gate.
3. **Optional templates:** `CreatorProjectTemplates.presets` and `contentTemplates` are definitions, not proof persisted user projects exist. Do not remove helpful opt-in project templates when asked to clean user data.
4. **Self-care preset helpers:** `PersonalSelfCarePresets` define optional routines; don't treat these as saved owner reminders.
5. **Legacy post-publish records:** `CreatorPostPublishEngine.isLegacyTask()` checks `sourceRefId.startsWith("post-publish:")`. `CreatorViewModel` migrates this domain into checkpoints. **HIGH RISK:** audit the migration and preservation tests, do not call it sample deletion.
6. **307 source hits** largely identify named presets, audio-piano samples, template names and compatibility code—not 307 existing records.

**Release rule:** Only delete a proven untouched historical demo record; never clear app data, remove accounts, delete private photos, reset synced reminders or guess by name.

## Screenshot/real-route work still required

- Capture all 51 source-traced destinations across representative empty, populated and error states on a disposable emulator (not a real owner's only copy).
- Compare headers/CTA typography at 1.0x and 1.3x font scales and compact display; classify text clipping, stacking and tap targets.
- Validate which historical versioned components are actually reached. Prioritize *reachable* high-density screens.
- Determine final keep/move/merge/drop choices; complete Manual Wave 2 for Creator Projects, Calendar, Insights, Widgets, alerts and all remaining notification copy.
- Apply one reusable component specification, then implement a coherent UI revision and run signed in-place upgrade preservation.

**No APK is produced by this audit branch; V292 signed sideload preview is unchanged.**
