# Backlot — Project Stages & Content Workspace Deep Audit

**Date:** 2026-11-16  
**Master register:** Final Refinements Before Launch, #06  
**Status:** RESEARCHED / PROPOSED for founder review; **NOT IMPLEMENTED**.  
**Source inspected:** preserved V337 commit `678502f4626150ea1945b81eed1593a362377454` in `rayadinaveen98-ship-it/framebynavin-android`.  
**Method:** Static review of Kotlin/Compose source, workflow/template registry, persistence/transition logic, CW1–CW5 specs, test and workflow inventory, and public 2025–26 creator/competitor documentation. No new build, on-device UI exploration, user-record inspection, CI execution, user interviews or live competitor account trials. Existing tests/docs are evidence of implementation intent, not proof that all devices pass today.
**Change-control:** Documentation-only on a separate branch; no source edit, cloud mutation, merge, deletion, APK or release.

## Executive conclusion

Backlot's **project-stage taxonomy is substantially richer than its everyday Content Workspace**. A creator can select distinct modes/archetypes/platforms/production styles; `CreatorWorkflowV2` has bespoke pipelines, but the Content Workspace mostly exposes a **common Hub, Plan, Script, Publish, Guide and Output Craft**, plus a **shared Stage Desk** with variable *copy*. This is why different formats can feel remarkably alike. The architectural foundation is valuable: stage-owned records, explicit saves, revision-fenced drafts, deliverable-level derivatives, cut/version review, manual stage approval, and the hard separation between publication and workflow progress. Do **not** rebuild or discard these.

**Proposed direction:** Content Workspace 3.0: a common, reliable Project Cockpit; a small library of **real format/intent-aware work modules**; editable, pinned workflow recipes; and deliverable-specific review/publish requirements. Improve specificity of actual inputs, outputs, verification and next action—not colors or card shapes alone.

## 1. Current pipeline, end to end

1. New Project captures title, notes, Creator mode, archetype/content type, production styles, platform, delivery format, due date and reminder preferences. Creator profile provides suggested values; selections are editable. (See `V20NewProjectWizard.kt`, `CreatorContentDna.kt`, `CreatorViewModel.kt`.)
2. `ContentArchetypeRegistry` defines content intent (review/analyze/teach/report/guide/etc.), while `CreatorModeRegistry` provides mode-specific labels and suggested archetypes. `CreatorContentDna` combines mode/archetype/style/platform/format. Account-level defaults do not give permission gates.
3. `CreatorWorkflowEngine.templateFor(task)` asks `CreatorWorkflowV2` for a bespoke template when explicit DNA/archetype is available; otherwise it falls back to older platform-and-format or generic 5-stage pipelines. Both are deliberately backward compatible.
4. Stage route: `StageWorkspaceRoutingPolicy` resolves a stage ID to PLAN, SCRIPT, PUBLISH or HUB. CW3 Stage Desk is always reachable through stage listing, and many formerly HUB stages open Stage Desk as the current action. The route **does not imply an actual format-specific production editor**.
5. Hub (`V19ContentWorkspaceAlpha6Hub`) shows the project, current step, progress, Plan, Script, Publish, Guide, Output Craft, up to two next-useful-move hints and collapsible stage list. Same basic composition for every project.
6. Plan (`V19ContentWorkspaceAlpha3Dialog`) has the shared brief (audience, problem, promise, angle), sources, assets, checklist, deliverables and other common tabs/templates. Script Studio (`V19ScriptStudioAlpha4Dialog`) stores hooks, title options, narration beats, visual/B-roll notes and readiness. Publish Studio (`V19PublishStudioAlpha5Dialog`) stores per-deliverable metadata, variants, gates and creator-confirmed published records.
7. CW3 Stage Desk (`CW3StageDesk.kt`) has the same form structure for nearly every stage: notes, stage task checklist, evidence/timecode entries, linked shared materials, decisions and an explicit save. `CW3StageExperiencePolicy` adjusts heading, text prompts and suggested tasks by stage ID, not a full type-specific data schema.
8. CW4 Output Craft adds independent derivative scripts and cut/version logs, a strong differentiator relative to generic task boards. It stores links/URIs rather than video bytes or rendering real edits.
9. CW5 `CreatorWorkflowTransitionPolicy` enforces deliberate, revision-matched stage-advance approvals and audit receipts, issues advisory warnings about missing materials, and **blocks publication stage completion without separately recorded publication data**. Approval of checklists, scripts, edits and publication remain independent facts.
10. Reminders depend on canonical project stage and schedule. Stage redesign must preserve existing reminder occurrence IDs, rescheduling rules and data consistency.

## 2. Strengths to protect

| Current strength | Verified mechanism | Why retain |
|---|---|---|
| Creator modes and archetypes | `CreatorModeRegistry`, `ContentArchetypeRegistry`, `CreatorContentDna` | Correct domain vocabulary and personalized starting points |
| Different stage lists | `CreatorWorkflowV2` and legacy `CreatorWorkflowEngine` | Distinguishes analysis, reviews, recipes, podcasts, technology reviews, reports, education, etc. |
| Honest editing boundaries | `StageWorkspaceRoutingPolicy` / Hub copy | Does not pretend Android app can edit all media |
| One project authority | `CreatorContentWorkspace`, `TaskStore` | A single source of truth for shared materials |
| Protected stage records | `CreatorStageWorkPolicy` | Stable project+template+stage identity and foreign-asset guards |
| Draft recovery & safe saves | Stage/Output/Editor draft journals, workspace revisions | Avoids stale editors overwriting newer content |
| Output-specific derivatives | `CreatorOutputCraftPolicy` | Short/Reel/clip scripts and independent histories |
| Real publication truth | `CreatorWorkflowTransitionPolicy` / Publish Studio | Stage completion is not mistaken for a published post |
| Old-project compatibility | optional fields, legacy fallback | Essential when changing workflow system |

## 3. Specific shortcomings and risks

**F1 — Template diversity is not workspace diversity (HIGH, UX).** Hub always displays the same Plan/Script/Publish/Guide/Output Craft tools. `CW3StageDesk` changes prompts/suggestions but presents identical notes/tasks/evidence/timecodes/decisions fields. A food creator may see evidence/timecode forms where an ingredients/recipe quantities editor is more useful; an audio podcaster may see visual narration fields rather than guest/run-of-show/chapter tools.

**F2 — Stage routing is keyed largely to IDs, not content capability (HIGH, UX/architecture).** `StageWorkspaceRoutingPolicy` has large ID sets for PLAN/SCRIPT/PUBLISH; other stages route to HUB or shared Stage Desk. Editing/capture/promotion records are notes/logs, not true dedicated workstations. This is honest, but underwhelms the professional workflow promise.

**F3 — Several V2 stage recipes do not materially adapt to duration/format (HIGH, accuracy).** Example: `filmAnalysis(platform,format,archetype)` uses the same ten-stage list for an analysis Short as a long-form video; platform/format changes the displayed label rather than stage requirements. Some workflows are determined by mode+archetype while delivery format is only shown in text. A 30-second vertical reel and 15-minute essay need different artifact/checkpoint depth.

**F4 — Universal focus nudges are sometimes unsuitable (MEDIUM-HIGH, relevance).** `V2285WorkspaceFocusPolicy` suggests narrative/script work when workspace script is absent even for non-narrative projects. New-project selected choices should drive these suggestions, not just generic missing fields.

**F5 — Stage progress communicates position better than readiness (HIGH, trust).** CW5 correctly distinguishes five authorities and warns about incomplete work, but most missing evidence is advisory; no format-specific definition of 'ready to advance' exists. Need optional, transparent stage-specific proof-of-work, with explainable warnings and founder/creator override where appropriate; never treat a checked box as external publication.

**F6 — Script Studio specializes in narration beats (MEDIUM-HIGH).** Strong for video essays and explainers, less natural for newsletters, multi-speaker podcasts, recipe cards, visual portfolio items or pure audio tracks. A project should only show relevant authoring modules and customize wording/fields, while retaining canonical old data.

**F7 — Stage evidence is mostly textual (MEDIUM).** Text and timecodes, source links and asset IDs are useful but cannot yet capture structured shot setups, take logs, measured experiments, guest questions, ingredients/weights, corrections, approvals, proof links, alt-text/accessibility checks or rights clearance in format-specific shapes.

**F8 — Output handoff is not one seamless editorial journey (MEDIUM).** Output Craft is powerful, but the creator must reason about generic Plan, Script, Publish and Output Craft cards. Main-vs-derivative ownership and last verified revision should be clearer when switching outputs.

**F9 — A multi-project or format change needs version pinning (HIGH, data safety).** Stage data uses template ID+stage ID. An engine evolution or edited Creator DNA may yield a new template. Current old data is intentionally preserved, not automatically reassigned. Future custom/template revisions need explicit version pinning and compatibility/migration decisions. Do not silently relabel or reinterpret saved work.

**F10 — Real-platform verification and media portability have natural limits (MEDIUM).** 'Published' is a creator-confirmed record rather than necessarily a verified live platform API event. Device media references may not work after reinstall/restore. UI must disclose truth; avoid fake upload or full remote restoration claims.

## 4. Research — creator needs (2025–26)

- Adobe's June 2026 multi-country creator survey (16,000+ respondents) reports **85% want final creative decisions kept with creators**; transparency and ability to review/edit/undo matter for agentic tools. Implication: templates guide, they do not decide or auto-publish. Source: https://news.adobe.com/news/2026/06/creators-toolkit-report-2026
- Adobe's 2025 global creator study reports **72% frequently create on mobile**; the India 2026 creator release reports strong mobile participation in its surveyed population. Implication: fast phone-first creation and credible tablet/desktop continuity matter, but don't imply universal representativeness. Sources: https://news.adobe.com/news/2025/10/adobe-max-2025-creators-survey and https://news.adobe.com/en/apac/news/2026/08/adobe-india-creator-survey-toolkit-2026
- Small creator survey discussion (self-selected; **not** representative) reports tool fragmentation, organization and accessing current files across phone/tablet/laptop as problems. Use as a qualitative hypothesis, not population percentage: https://www.indiehackers.com/post/everything-that-s-broken-about-video-content-planning-our-survey-of-creators-c42ad35581
- Podcast creators in a community thread describe combining Airtable, calendars, Google Docs, recording, editing, mastering, and hosting tools rather than one complete solution. Anecdotal but revealing: https://www.reddit.com/r/podcasting/comments/1feav01/what_tools_do_you_use_for_production/
- Product research implication (hypothesis to validate with actual test creators): people want to know **What do I do now? What good output is expected? Where is the current file/version? What is blocking me? What happens if I press Done?**; fewer irrelevant fields and less switching between tools.

## 5. Competitive landscape (desktop/web and professional scopes differ)

| Product | Verified meaningful strength | Backlot lesson | Scope limit/position |
|---|---|---|---|
| **StudioBinder** | Production-centric scripts, shot lists, storyboards, schedules, call sheets, contacts and tasks | Real professional production objects beat labels-only stages | Deep filmmaking/production, heavier than solo creator phone use |
| **Celtx** | Script-integrated shot list fields (shot size, camera movement, setup, equipment) | Specific structured fields tied to the stage's real task | Screen/film production focus |
| **Milanote** | Visual research boards, moodboards, preproduction, storyboards and shot plans | Make references/creative decisions spatially and visually meaningful | Does not inherently solve every reminder/distribution need |
| **Frame.io** | Frame-accurate/timecode comments and version review | Revision evidence and approvals should name exact cuts/versions | Review/media collaboration specialist, not full idea-to-release system |
| **Planable** | Native-like social previews, approval workflows and scheduling | Accurate channel formatting and output-specific signoff | Social workflow depth rather than full film production |
| **Notion / ClickUp** | Flexible, connected content pipelines or production project templates | Customizable workflow without forcing one industry pattern | Requires configuration; less specialized stage guidance by default |
| **Airtable** | Linked production assets/calendars/approvals and system integrations | Traceability between request, media asset, deliverable and outcome | General relational workspace with setup overhead |

Source documentation: https://support.studiobinder.com/en/articles/10951295-what-s-included-with-the-professional-plan ; https://support.celtx.com/hc/en-us/articles/218530327-Shot-Lists ; https://milanote.com/templates/filmmaking ; https://help.frame.io/en/articles/9105251-commenting-on-your-media ; https://help.planable.io/hc/en-us/articles/21715462785180-Approvals-and-Approval-Workflows ; https://www.notion.com/templates/creator-content-os-plan-schedule-track-your-business ; https://clickup.com/blog/video-production-project-management/ ; https://www.airtable.com/use-cases/creative-production-calendar-asset-management .

**Market positioning conclusion:** Apps absolutely exist with significant or greater depth in individual areas. It would be unsupported to claim 'no app offers this scope' without an exhaustive feature/pricing and hands-on trial comparison. The plausible differentiated niche is an **Android-first personal creator operations system combining actionable format-specific workflow, premium visual creative guidance, trustworthy reminders and output/version lineage**, without trying to replace DAWs, video editors or studio schedulers. More scope alone is not a competitive advantage.

## 6. Proposed Content Workspace 3.0

### 6.1 The contract: one core + contextual modules

**Core Project Cockpit (same place every time):** Title and real platform/format; progress with `current stage` vs `stage tasks` vs `published outputs` kept distinct; visible current-stage goal; up to 3 honest next actions; nearest deadline; last saved status; output chooser; stable access to shared sources, media links and project history.

**Contextual Stage Kit:** Every stage must state (1) purpose, (2) required inputs, (3) actual editing/capture/research tool, (4) expected real output, (5) meaningful validation, (6) how it links to the next stage, (7) optional help/examples. Stage kits bind by content intent + archetype + production style + target output format + stage capability, not stage label string alone.

**Common structured module families:** Brief/decision; source and claim evidence; shot/storyboard or scene plan; script/outline/run-of-show; capture/take logs; edit review and version compare; visual/caption packaging; verified release; post-publish learning. **Not** separate source-of-truth databases or a screen for every archetype.

**Context-driven UI:** A podcast project can open guest preparation and run-of-show, while an analysis project opens thesis-to-scene evidence, and an Instagram carousel opens slide structure. Same Backlot typography/brand/nav; different tools and microcopy only when real tasks require them.

**Clear tools boundary:** Backlot is a production planner, evidence/version/revision manager and trustworthy reminder/launch tracker. It does not claim to edit video, mix music, publish to unsupported APIs, download footage or host original media without actual implementation. Deep-link to external editing tools and files safely.

### 6.2 Five illustrative pilot workflows (not actual saved user projects)

| Example project | Stage-specific content and artifacts to add | Key QA/release checks |
|---|---|---|
| **Cinematic analysis, YouTube long-form** | thesis/central question; frame/scene evidence with source/timecode and rights note; argument outline; narration beats; recording plan; cut review; title/thumbnail/chapters; publication link | Claims tie to evidence; use of licensed/transformative source understood; exact cut revision clear |
| **Short-form Reel or Short** | hook and payoff; 15–60s beat map; 9:16 capture/graphic safe area; subtitles; cover/frame choice; output-specific caption & music-rights check | Not a long-form pipeline shrunk into tiny text; tap to preview target format |
| **Podcast audio episode** | guest/solo brief; background verification and consent; interview question stack/run-of-show; recording checklist; audio take/log; audio QC and chapters/show notes; distribution; clips | No forced video thumbnail editor or shot list when audio-only |
| **Educational tutorial** | measurable learning outcome; prerequisite checklist; ordered demo steps; capture/reference examples; mistake/safety notes; comprehension check; publish metadata | Every step can be verified; no generic script-only default |
| **Instagram carousel / social design post** | slide-by-slide story, visual hierarchy, crop/alt text, claims/proof, caption/CTA, design revisions, native-target preview | No irrelevant audio/video stage; each slide/output version is traceable |

**Missing high-value family:** A true **original narrative short film** pathway needs concept, screenplay, production breakdown, storyboard/shot list, schedule, location/cast/crew/rights, shoots/takes, picture/sound/color finish, deliverables, release/festival plan. This should be a separate opt-in format kit, not applied to ordinary YouTube explainers. Documentary, animation, livestream and original music can be later specialized kits.

### 6.3 Workflow recipe system, preserving V337 data

Propose versioned recipe objects specifying `recipeId`, `recipeVersion`, supported archetypes, production styles, platforms, formats, stage stable IDs, optional checks, suggested structured forms and output requirements. **Pin** the effective recipe+version at project creation. An old project keeps its recipe, stage identifiers and saved stage data until the creator explicitly requests a reviewed conversion. Unknown formats use the existing generic fallback and truthfully say so.

Layer extensions/additive `stageArtifacts` on top of current `CreatorContentWorkspace` without replacing `stageWork`, `scriptStudio`, `outputScripts`, `editVersions`, `stageTransitions`. Use structured schema IDs with explicit versions, export/import and source/data epochs, no plain freeform writes that could invalidate old data. All scripts and output associations keep one canonical authority and stable IDs.

Template editing by user should be optional; for first release only let users add/skip optional stages or choose alternative recipes with explicit staged migration/preview. Do not silently change old projects when user edits setup preferences.

### 6.4 Stage readiness and publication accuracy

- Provide **Recommended before advancing** checklist that includes stage-specific evidence or deliverables (e.g. a linked shot plan) and the status of any hard requirements.
- Distinguish `Not started`, `In progress`, `Ready for review`, `Creator approved`, `Published (manually recorded)`; never conflate them or auto-advance on checklist completion.
- Keep existing hard publish verification gate and explicit revision-matched approval; add non-destructive warnings and a conscious review/override path for optional creative checks.
- Show which exact output and cut revision are approved; don't confuse an approved edit with a live post.
- For linked external media, show file availability and source device; no promise that URI automatically survives reinstall.
- Platform-specific release guardrails should validate editable asset metadata, accessibility and format requirements when available, with no invented auto-posting.

### 6.5 Design language and usability

- Design **one** premium shell consistent with Director's Cut and Light; context changes the content and tool order, not the whole navigation.
- Phone: current stage + action above fold; useful linked materials and tasks under two collapsible contextual panels; fixed Save/Back; progressive disclosure. Tablet/landscape: optional dual-pane preview/evidence desk. Always preserve plain text labels, accessibility/large fonts/reduced motion.
- No stage-wall of 10 equally important buttons, generic prompts for every project, or duplicate editors competing for truth.
- Show explicit 'last saved' and 'unsaved draft' states, errors, retries and recovery. Defer heavy visual/asset rendering; use offline-first native components.
- New optional onboarding templates (#02/#03) fill suggestions; final project overrides always win.

## 7. Stage-wise implementation and approval gates

| Work package | Scope | Deliverable | Gate |
|---|---|---|---|
| **A: Frozen inventory & journeys** | Map current 5 example projects, all workflow IDs, routing and persistence; record screenshots/phone obstacles in owner-test install | Current-state matrix + migration inventory | Evidence signed off; no code change |
| **B: Field research & interactive prototypes** | 5–8 target solo creators across short, long, audio, social and film; prototype current vs proposed on Android sizes | Journey/time-to-next-action tests + UI kit | Founder design approval; do not infer preferences |
| **C: Versioned recipe spec** | Formal schemas, stage requirements, capability matrix, migration, compatibility, route contract | Architecture/data migration ADR | Tests for old project/backup integrity approved |
| **D: Pilot stage kits** | Long-form analysis + Short/Reel + Podcast + tutorial + carousel, using existing data authority | 5 format-aware working modules + stable shell | Unit/Compose/device test per journey |
| **E: Output pipeline integration** | Main+derivative selection, evidence/version review, format-aware Publish, reminder links | Accurate output handoff and stage readiness | Publication and reminder regression suites |
| **F: Scale to other creator modes** | True original short film, music, news, lifestyle, travel, fitness etc. after pilots | New optional recipe packs | No template sprawl or claim of coverage before build |
| **G: Integrated quality/release** | V337 compatibility, backup/restore *data integrity under authorized test only*, crash/reliability, phone/tablet/OS/accessibility, signed upgrade | Test evidence and owner APK only when gates pass | Separate merge/release authorization |

### Pilot acceptance criteria (measurable, proposed targets, not tested facts)

1. Five example projects get **meaningfully different current-stage tools and expected outputs**, not merely renamed stage headings.
2. A new user finds the next real action in <=2 taps from Project Cockpit and understands what 'complete stage' will do.
3. Users can switch between five projects without cross-project draft/data leakage; unsaved edits are explicitly recovered or intentionally discarded.
4. Legacy projects open with identical stage history, outputs, media links and reminders as before; recipe upgrades are opt-in.
5. Evidence, script, cut review, stage approval and publication truth remain separate; unsupported media operations are never represented as performed.
6. All needed Creator data serializes/deserializes across test upgrade & export round trips with fixed project/asset/output identity.
7. Accessibility, tablets, 320dp phones, 200% text, offline/low-memory and back/rotation cases are certified.
8. No unapproved paid data service, AI API, video hosting or third-party lock-in; all core planning/editable metadata remains offline-first.

## 8. Product assessment and business positioning

**Differentiation:** A creator-aware, action-oriented production OS rather than a generic Kanban or another editing app. Combining adaptive workflow, companion hints (optional), dependable reminders, idea-to-output traceability and mobile ease may be appealing to Indian/social-first independent creators. The core value is that it understands *what work is actually needed next*.

**Cost:** Native Compose with local structured templates and existing JSON/TaskStore records is the least-expensive viable route. Avoid video processing/cloud storage, commercial voice/AI calls, hosted collaboration, automatic posting integrations or paid analytics as prerequisites. Future team sync can be an explicit commercialization decision.

**Top risks:** Data migration/template drift, misleading 'ready/published' statuses, over-complex first-run experience, untested per-format copy, file-link loss on reinstall, renderer performance, long-term template maintenance, and licensed footage provenance.

**Non-goals for this refinement:** Embedded timeline video editor, DAW, universal social autoposting, Frame.io-grade hosted video review, full StudioBinder crew operations, paid AI scripts, or replacing stable reminder/alarm infrastructure.

## 9. Decision recommendation

**Approve the direction of Workspace 3.0 design/research, not an immediate rewrite or merge.** Preserve V337 and the reliable CW1–CW5 authority. Prioritize a capability-aware current-stage UI and five real format pilots. Keep creator control and honest statuses; commission physical-device and targeted user validation before broad implementation. User approval of a product direction is **not** evidence of implemented, tested or shipped code.

**Next founder decision:** approve either (a) a pilot-first adaptive shell + five stage kits (recommended) or (b) a full new custom workspace architecture (higher cost/migration risk, not justified by inspected evidence yet).

---

## Inspection references (repository at preserved V337 commit)

- `app/src/main/java/com/framebynavin/app/data/CreatorWorkflowV2.kt`
- `app/src/main/java/com/framebynavin/app/data/CreatorWorkflow.kt`
- `app/src/main/java/com/framebynavin/app/data/CreatorContentDna.kt`
- `app/src/main/java/com/framebynavin/app/data/CreatorContentBlueprint.kt`
- `app/src/main/java/com/framebynavin/app/data/CreatorContentWorkspace.kt`
- `app/src/main/java/com/framebynavin/app/ui/StageWorkspaceRoutingPolicy.kt`
- `app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt`
- `app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha3Ui.kt`
- `app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha4Ui.kt`
- `app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha5Ui.kt`
- `app/src/main/java/com/framebynavin/app/ui/CW3StageDesk.kt`
- `app/src/main/java/com/framebynavin/app/ui/CW4OutputCraftDesk.kt`
- `app/src/main/java/com/framebynavin/app/data/CreatorStageWork.kt`
- `app/src/main/java/com/framebynavin/app/data/CreatorWorkflowTransitionPolicy.kt`
- `docs/roadmap/CW1_UNIFIED_STAGE_ROUTING_ACCEPTANCE.md`, `CW2_STAGE_WORK_DATA_FOUNDATION.md`, `CW3_CONTEXTUAL_STAGE_DESK.md`, `CW4_OUTPUT_CRAFT_ACCEPTANCE.md`, `CW5_WORKFLOW_PUBLICATION_GATE.md`.

