# FrameByNavin — Creator MVP Implementation Plan

Status: approved planning baseline, 8 September 2026. This document defines scope and acceptance, not a claim that future milestones have shipped. It incorporates the uploaded Creator Product Strategy Report and the existing FrameByNavin source audits. The exact frozen release is documented in `docs/releases/v1.8.2-reminder-safety.md`.

## Product contract

Mission: Capture → Create → Finish → Publish → Learn → Create Better.

Initial customer: solo video creators with regular publishing ambitions, multiple production steps, and difficulty maintaining execution consistency. Subscriber count is not a qualification. Start with YouTube-first workflows and practical Instagram deliverables; support other platforms through truthful, extensible planning interfaces rather than implying unavailable API integrations.

Core promise: turn an idea into a realistic production plan, show the next useful action, protect publishing commitments, record what actually happened, and help the creator improve the next piece of content. The app is not a general social scheduler, video editor, agency suite, or generic AI chat application.

The existing project, reminder, idea, calendar, analytics, account, and backup systems must be evolved rather than duplicated. Preserve the cinematic visual identity, original personal imagery, offline-first operation, user control, and existing data. Keep execution calm: no invented performance numbers, opaque quality scores, forced AI, artificial streaks, or unlimited notification escalation.

## Delivery and engineering rules

- Work from the exact verified v1.8.2 generated source at commit `91f8f8b266208c9646163a197b2da1932861a67c`, never stale main. Preserve real Git history; no synthetic-history replacement or force pushes.
- GitHub Actions remains the authoritative Android build and verification environment. Every implementation milestone must deliver an installable APK, exact versionCode/versionName, source commit, test results, SHA-256, and signing identity. Do not claim a release from a partial or failed run.
- One canonical integration branch should contain the materialized production source after a verified equivalence migration. Archive historical transformation scripts and artifacts before retiring them. Do not change the source architecture and product model simultaneously.
- Maintain an explicit schema/migration contract, forward/backward compatibility policy, user-data backup, and rollback procedure. Destructive changes require confirmation and testable recovery.
- Separate automated CI acceptance, physical-device acceptance, and public-store release readiness. A debug-signed pilot is not a production release.
- Keep cloud and AI optional. Core planning, editing, and reminders work offline. No required paid AI, unrestricted background publishing, or cross-platform automation in the foundation milestone.
- Implement features only if they improve the core loop or remove a measured user obstacle. Existing capabilities must be inventoried and reused before creating a new module.

## v1.8.3 — Foundation and Trust

Proposed versionName: `1.8.3-foundation-rc1`; next versionCode: `66`. Scope is safety, reproducibility, and usability. Do not add Content Project 2.0 or new AI features here.

### Work package 1: exact-source audit and canonicalization

Reconstruct the source with the historical v1.8.1 scripts, then apply the exact v1.8.2 hotfix. Verify the generated version, source lineage, expected files, and build equivalence against the frozen release. Export a complete source snapshot and hash manifest before changing any application code. Inventory active UI roots, stores, persistence paths, migrations, workers, notification/voice services, OAuth, backup files, and external dependencies. Create a gap ledger: confirmed fixed, confirmed open, requires device validation, or not applicable. Do not blindly reimplement defects reported against Alpha23 or v1.7.5: v1.8.1 already introduced hardening components that must be inspected.

Materialize the exact source into a proper integration candidate. Retain historical scripts and source artifacts in history. Remove dormant/legacy production UI only after behavior-equivalent builds and route tests. Update .gitignore and .env.example, exclude credentials, signing keys, generated build files, local IDE state, and private content. Establish reproducible CI builds from the canonical source. Do not promote to main until the same gates pass and a recovery path exists.

### Work package 2: data and lifecycle safety

Audit the current CreatorDataGate, CreatorPublicationEngine, CreatorDeltaEngine, backup/restore, and cloud implementation. Verify that publication is an independent event from project completion, reminder acknowledgement, and workflow advancement. Validate publication timestamp/link, duplicate prevention, corrections, reopening, rewards, and post-publication checkpoint anchoring. Fix only confirmed gaps.

Consolidate authoritative mutations behind transactional/revision-aware repository operations. Eliminate stale full-list overwrite paths. Test concurrent UI edits, auto-plan/worker mutations, process death, and restore. Define versioned backup manifest, checksum and authenticity requirements, included/excluded data, consistent snapshot boundary, rollback, and restore-worker coordination. Include user-managed artwork or explicitly disclose exclusions. Secrets and OAuth credentials must not be copied into portable backups.

Audit cloud first-sign-in reconciliation, latest/daily backup replacement, revisions, device identity, deletion/tombstones, sync cancellation, account deletion, and authorization. Cloud backup must not be described as conflict-aware multi-device sync unless it truly is. Preserve local-only use and safe export/restore. Review Supabase RLS and RPC authorization with positive/negative cross-account tests; distinguish public client keys from server secrets. Verify account display-name ownership and current authentication lifecycle.

### Work package 3: reminder and release reliability

Run regression tests for Done versus project completion, managed checkpoint persistence, stale occurrence rejection, independent projects, reschedule, Smart escalation stage, task-scoped service cleanup, and restore epoch invalidation. Add permission/reliability health reporting and distinguish exact from inexact fallback. Verify locked screen, process death, reboot, time/timezone changes, simultaneous alerts, DND, battery saver, permission denial/regrant, and missed reminders. Record real-device results rather than assuming emulator success guarantees OEM behavior.

Run a screen-by-screen usability pass: back stack, keyboard/insets, empty/error/loading states, readability, 1.5×/2× text, TalkBack, touch targets, reduced motion, and small/large displays. Preserve the cinematic identity while reducing unnecessary operational clutter. Prepare a separate production release signing/OAuth/privacy configuration; do not change the established pilot signing identity without an upgrade/data-migration plan. Validate release build non-debuggable configuration and keep production signing secrets outside the public repository.

### Exit gate

Exact source is archived and reproducible; confirmed trust-critical issues are closed with regression tests; old user data migrates and restores correctly; unit/lint/instrumentation/emulator gates pass; a versionCode66 APK and source/diagnostics artifacts exist; physical-device acceptance is recorded separately. If a gate fails, retain v1.8.2 as the pilot baseline and do not label v1.8.3 released.

## v1.9 — Content Project 2.0

Goal: evolve the existing project into one coherent creator workspace. Do not build a second parallel project system.

Domain: ContentProject, WorkflowStage, WorkItem, Deliverable, PublicationEvent, Reference/Asset, and Review. Retain existing IDs and migration compatibility. A project can have multiple platform-specific deliverables, each with its own deadline, format, status, publication records, and external link. Publishing must not complete the whole project. Acknowledging a reminder must not complete a task. Reopening/correcting records must not duplicate rewards or analytics.

Project workspace: brief (audience, viewer problem, promise, angle, format/pillar), hook, outline/script, research links, assets/thumbnail concepts, editable production checklist, publishing metadata, derivatives, and learnings. Store large media via references/content URIs or explicitly managed local copies rather than filling the database with video files. Preserve access permissions and handle missing/moved external files.

Templates: start with a few excellent YouTube long-form, Short, Reel, commentary/review, and educational formats. Existing generic/platform templates remain editable. Do not hardcode cinema examples for all users. Project creation should generate realistic steps with optional durations and dependencies; users can remove or reorder them. A new creator should reach their first actionable project without mandatory account connection or excessive setup.

Exit: existing projects migrate safely; create/edit/reopen/archive/delete/restore works; a project can publish one deliverable while promotion and other deliverables remain active; scripts/links survive process death and backup/restore; every core action is accessible and tested. Deliver an APK with complete release provenance.

## v2.0 — Today and Execution Intelligence

Goal: make the app useful within seconds of opening it.

Today shows one primary Continue action, a small number of optional tasks, the next publishing commitment, one important blocker, and a clear recovery action. Keep optional insights/rewards secondary. Resume the exact project/stage and preserve editor state. Add an explainable Next Best Action policy using deadline, dependency, priority, estimated effort, available time, energy preference, and user choice. Never imply the app knows the creator's motivation or health.

Capacity planning: user-configured available creator hours, recurring work windows, recording/editing preferences, and optional task estimates. Compare planned effort with available capacity, explain overload, suggest deferrals, and require approval before changing committed deadlines. Avoid treating estimates as certainty. Distinguish suggested and committed schedule changes.

Reminder behavior: reuse the existing safety engine; add transparent snooze reasons, quiet-hours/notification budget, focus-session actions, and recovery plans. Suggestions based on behavior must be editable and opt-in. User-facing deadline changes require approval. No automatic escalation beyond configured bounds and no aggressive reward pressure.

Exit: a creator can see a realistic next action, resume it, adjust an overloaded week, recover a missed commitment, and preserve all project/publication state. Measure task-to-first-action time, useful recommendation acceptance, and reminder fatigue. Deliver verified APK. Begin a small external usability pilot here rather than waiting for every later feature.

## v2.1 — Capture Everywhere

Extend existing Quick Capture, Idea Vault, and widgets. Add Android share-sheet ingestion for text/URLs, voice-to-idea using on-device/system capabilities where appropriate, and optional screenshot/reference attachments. Save first; structure later. Preserve source URL, timestamp, original text, and user ownership. Parse metadata when permitted and available; show unknown data honestly. Do not scrape restricted sites or assume transcripts are accessible. Optional AI tagging/summarization must preserve raw source and require consent for cloud processing.

Exit: a creator can share an idea/reference from another app into FrameByNavin and recover it offline with correct source attribution. Duplicate suggestions must not silently delete ideas. Deliver verified APK.

## v2.2 — Publish and Learn

Build on the publication/deliverable model. Strengthen authorized YouTube analytics, freshness labels, metric definitions, cache/error handling, project linking, 24-hour/7-day/28-day reviews, and creator-defined content pillars. Separate official metrics from manually entered data, calculations, and hypotheses. Use comparable formats/age windows and minimum evidence thresholds; do not invent unsupported metrics or infer a cause from a small correlation.

Implement a Next Best Action loop: evidence → interpretation → proposed experiment → editable project → publication → review of result. Add manual creator lessons and a weekly review that prioritizes useful decisions over charts. Keep analytics read-only by default. Publishing permissions, if later introduced, must be optional and separately consented. Repurposing creates child deliverables rather than replacing specialist editing software.

Exit: a published item links to real performance data, produces an explainable recommendation, and can create a new project/experiment without fabricating results. Test revoked credentials, expired tokens, missing metrics, network failures, and source freshness. Deliver verified APK.

## v2.3 — Creator Brain

Introduce the structured creator content graph: ideas, projects, deliverables, publications, topics, pillars, formats, hooks, assets, experiments, performance, and user-approved lessons. Keep provenance and user-editable links. Start with deterministic search, related-content suggestions, saved lessons, and clear filters. Add optional AI retrieval/reasoning only after accuracy, privacy, cost controls, and evaluation are established. Avoid generic chat as the main product. All generated suggestions require user confirmation before modifying production plans or publishing.

Exit: the creator can find related work, reuse a past lesson, identify an unrepurposed successful item, and trace every recommendation to supporting data. Deliver verified APK.

## Later expansion, not committed MVP scope

Official Instagram professional-account analytics and additional platform adapters; optional publishing/scheduling with separate permissions; sponsor deliverables; revenue attribution; team collaboration; and creator commerce. Prioritize using pilot demand and API feasibility. No complete social inbox, full video editor, course platform, CRM, or every-platform integration in the initial MVP.

## Architecture and implementation direction

Keep Android native Kotlin + Jetpack Compose, Hilt, coroutines/Flow, MVVM with modular domain/data/UI boundaries, AlarmManager for eligible user-facing exact reminders, WorkManager for deferrable work, NotificationManager, Android TTS/media, and an optional Supabase backend. Use Room/SQLite for transactional core relational data when migrating from existing stores; preserve DataStore for small settings and preferences. The exact current persistence implementation must be inventoried before migration—do not claim a Room migration is already complete.

Suggested modules: core/model, core/database, core/preferences, core/designsystem, feature/today, feature/ideas, feature/projects, feature/calendar, feature/reminders, feature/insights, feature/profile, data/cloud, data/youtube, and app/navigation. Introduce modules incrementally after source canonicalization. Avoid a large rewrite. Domain services should expose explicit commands with stable IDs, revision/occurrence tokens, and idempotent event handling. Keep external APIs behind adapters and persist source/freshness/provenance metadata.

The source of truth for operational state must be explicit. Use one authoritative mutation path and transactional boundaries for related changes. UI consumes observable state and does not asynchronously overwrite whole snapshots. Background workers must be idempotent and revision-aware. Backup/restore requires coordinated locks, versioned manifests, validation, rollback, and migrations. Publication events, completion state, reminder acknowledgements, and rewards must remain separate.

## Pilot and commercial validation

Recruit an initial 15–30 relevant creators using real production workflows. Measure activation, first real project/action, idea-to-publication completion, weekly retention, accepted useful recommendations, reminder fatigue, and self-reported time saved. Use privacy-preserving opt-in product telemetry, with account-independent local mode and no invented analytics. Record a pre-pilot baseline and evaluate against it. Do not equate app opens, XP, or a green build with product-market fit. Test willingness to pay only after repeat usage and demonstrated value. No fixed subscription price or public-launch date is committed.

## Immediate next action

Start v1.8.3 with the exact-source recovery/audit and a current-state gap ledger. Produce a source archive, migration inventory, test matrix, and prioritized confirmed findings before touching new product features. Preserve v1.8.2 as the rollback baseline. The next Android implementation must use versionCode66 and must not be declared complete without its installable APK and passing verification evidence.
