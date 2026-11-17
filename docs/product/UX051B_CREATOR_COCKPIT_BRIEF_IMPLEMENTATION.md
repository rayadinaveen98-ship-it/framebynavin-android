# Backlot UX05.1B — Creator Cockpit and Creative Brief 2.0

**Status: SOURCE STAGED ONLY; NOT COMPILED, TESTED, CERTIFIED, MERGED OR RELEASED.**

Parent UX05.1A draft PR #398 at commit `1009a7f8ca52b1847a5d4fed035dffd69a818efe`.
Feature branch `feature/backlot-ux051b-cockpit-brief-20261117`.
Protected owner baseline `stable/backlot-v337-premiere-owner-testing-20261114` remains untouched.

## Approved goal / implementation

1. **Truthful contextual actions.** The existing read-only `V2285WorkspaceFocusPolicy` now gives stage-sensitive advice grounded in the actual current workflow stage and saved project stage records. Evidence/research stages inspect saved sources and individual evidence links. Script stages inspect actual Script Studio beat status. Editing stages inspect actual output-cut records and approvals. Packaging/publishing stages inspect existing deliverables and readiness. Other stages inspect saved notes, evidence and unfinished checklist items. It does not infer activity duration, source verification, live platform status or approvals.
2. **Direct navigation.** The hub's contextual actions open the canonical Stage Desk, Script Studio, Output Craft, Publish Studio, or a specific existing Plan tab (Sources, Tasks, Outputs), with a direct Brief route. The existing primary Continue routing policy, stage progress, work flags and publication semantics remain untouched. Focus hints are advisory, non-mutating and capped at two.
3. **Brief 2.0.** Retain existing audience, viewer problem, promise and creative angle. Add optional objective, success criteria and constraints to the *same* `CreatorContentWorkspace`; these are not new independent documents or workflow gates. Plan → Brief reveals the extra fields on demand; Cockpit reads the saved facts in a compact collapsible section. Empty optional fields do not prevent work or stage approval.
4. **Data safety.** New string properties are appended after the legacy constructor parameters, with empty defaults. Both canonical `TaskStore` JSON and local editor draft JSON read/write these properties and decode absent older fields as empty. Held draft previews display their recoverable text and the existing guarded recovery route permits the new labels. `normalizeWorkspace` trims optional strings. Publish Studio save preserves current canonical optional fields, rather than overwriting them with defaults from a different editor.
5. **Adaptive layout.** The Cockpit panel is collapsed by default; the brief header separates status from its action row to reduce crowding on compact screens. Existing visual tokens and navigation are retained.

## Changes by file

- `data/CreatorContentWorkspace.kt`: three optional appended fields and `isEmpty` protection.
- `data/TaskStore.kt`: authoritative JSON round trip.
- `data/CreatorEditorDraftStore.kt`: local unsaved draft round trip and held-draft previews.
- `ui/V19ContentWorkspaceAlpha3Ui.kt`: optional editable brief and safe initial Plan tab.
- `ui/ContentWorkspaceActivity.kt`: direct Plan tab/Brief routing, historical text recovery, non-Plan ownership fence, normalization.
- `ui/V2285WorkspaceFocusPolicy.kt`: stage-aware advisory suggestions and explicit action destinations.
- `ui/V19ContentWorkspaceAlpha6Ui.kt`: collapsible brief, linked contextual actions.
- `docs/product/UX051_ADVANCED_CREATOR_WORKSPACE_ROADMAP.md`: status update.
- This implementation handoff.

## Preservation and rollback

All original project IDs, workflow template IDs, stage IDs, script beat IDs and output IDs remain unchanged. No new repository dependency, cloud service, permission, video renderer, reminder behavior, publishing automation, schema migration script, remote write or payment integration. No old data is deleted. The new fields are additive to JSON, **not** an assertion that an older running app can preserve unknown fields through a later downgrade; downgrade/parallel-version interoperability needs explicit certification.

If UX05.1B must be removed, drop this unmerged stacked branch/PR; do not revert or rewrite the protected baseline.

## Required checks after UX10, not performed here

- Kotlin/Compose compile and lint; verify new callback signatures and defaults, Material icon availability, enum/when exhaustiveness, and no duplicate symbol.
- Existing PR #393–#398 regression; in-place V337 to cumulative candidate; old records without new keys remain readable.
- TaskStore encode/decode and backup/restore round trip for new fields, including literal unicode, long text and absent keys. Confirm unified creator archive includes TaskStore payload.
- Unsaved Plan draft journaling, historical draft recovery, stale generation, save conflict and process death.
- Publish Studio edits do not erase objective/success criteria/constraints.
- Source/evidence actions open the intended Stage Desk or Plan Sources; scripts open Script Studio; cuts open Output Craft; packaging opens Publish; Brief opens Plan Brief; no activity launch mutates project state.
- Real project fixtures across film analysis, news, tech, gaming, podcasts, education, food and travel.
- Case matrix: no sources, sources with zero evidence, unlinked evidence, all linked evidence, no script, draft beats, ready beats, no output, no cuts, unapproved cuts, published vs not published, skipped/archived/finished, missing audience, optional fields absent.
- Small 320dp phones, 1.3x–2x font scale, TalkBack, landscape, tablets, long titles/briefs, keyboard overlay, scroll and interaction reachability; accessibility is not certified from source review.
- Run comprehensive integration, unit, instrumentation, CI, emulator and physical-device acceptance after UX10, then repair and rerun affected checks. No merge or release without explicit owner approval.

## Status / handoff

**Source-staged** is not **tested**, **certified**, **merged**, or **released**. UX05.1C (Creative Board and contextual modules), UX05.1D (output-specific review and learning), UX05.1E (integration/sync contract) remain pending.
