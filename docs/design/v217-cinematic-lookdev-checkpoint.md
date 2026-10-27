# Backlot V217 — Cinematic look-development review checkpoint

**Date:** 2026-10-26  
**Status:** design-only; owner review pending; **NOT app-integrated / NOT release accepted**  
**Parent:** [V217 design draft PR #130](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/130), [companion overhaul issue #129](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/129).

## Narrowed look-development directions

- **Frame / F2 Aperture**: gold, black face panel, angular aperture-inspired silhouette, calm deliberate film-craft personality. Avoid generic toy-robot styling; ensure icon at 48dp is still intelligible.
- **Navi / N1 Navigator**: tousled-haired human-like young creative navigator, black hoodie with restrained **coral** accents matching current selection color, attentive/organised expression, distinct from real Spark's glasses-wearing character.
- **Cute / C3 Comet Kitten**: soft cream kitten, purple eyes and ear/paw feline cues, pink/violet accents; deliberately preserve the cat species seen in existing Cute source.
- **Spark / FUNNY** remains EXACTLY the existing glasses-wearing character wearing a black hoodie, with existing source artwork, state mapping, persistent identity and resources untouched.

One AI-generated 1672×941 concept contact sheet has hero illustrations, illustrated idle/listen/think/celebrate states and illustrative UI context. A standalone offline HTML look-dev viewer offers comparison, states and size/theme simulations using **crops** derived from that sheet. A zipped review bundle includes its viewer and cropped illustrative WebPs, a review manifest and README. The concept sheet and local review bundle were generated **outside GitHub**; they are currently conversation artifacts, not shipped Android drawable resources, and this checkpoint does not commit those binaries. Crops are **not** independent consistent pose renders, transparent production sprites, actual small-size device screenshots or quality validation at real 48dp.

## Review gate

1. Creator reviews each identity's shape, proportion, wardrobe and material quality and gives feedback or approval. No character is declared approved from a generated board alone.
2. Generate genuinely independent pose artwork, clean transparent alpha / consistent crop / visual scale; avoid context/background cropping. Ensure Cute retains kitten likeness; avoid revising Spark.
3. Define unambiguous mappings for all existing 12 `CinePulseState` states with idle, listening, thinking, pointing, presentation, celebration, rest and walking fallback where needed.
4. Test 48/96/200dp **on actual Android** across compact 900×1600 1.5×, default 1080×2400, dark/light, reduced-motion, and on original companion-choice/setup screens. Use real pixel output, not generated mockups.
5. Verify no changes to stored identities (`FRAME/NAVI/FUNNY/CUTE`), `backlot_character_identity_v151`, `selected_character` or backup/restore; test restart and restore.
6. Only after owner artwork approval produce separate bounded runtime PR; keep current V217 docs PR draft; preserve all Spark assets and do not merge main/stable until release gates #101/#120.

## Scope integrity

The actual repo renderer presently uses procedural Frame/Navi while Cute uses a single welcome raster for every semantic state; that inconsistency is the reason for the pending implementation. No actual app logic, reminder/alarm engine, user data, existing source art or stable branch was changed by this checkpoint.

**Next:** creator visual feedback → independent alpha pose production → Android drawable/renderer PR → emulator + physical QA.


## Second look-development pass — alpha-transparent concept cutouts (26 October 2026)

After the first contact-sheet review, a **separate RGBA 1774×887 design image** was generated showing only Frame F2, Navi N1 and Cute C3 on a genuinely transparent background. Alpha range checked in the source image: **0–255**; no Spark image was generated. The separate image deliberately puts Navi in a navy/coral creator jacket, making it more distinguishable from the original glasses-wearing Spark in black. Cute remains clearly a cream kitten with amethyst eyes. Frame uses a more literal metallic aperture visor and needs final simplification to feel less toy-like.

Three **transparent full-body concept WebPs** and three **transparent small-avatar concept WebPs** were prepared from that independent image for a revised standalone browser look-development prototype. The four expression previews and in-app vignettes still come from the earlier contact sheet, so their poses are **not** yet production-consistent. The revised offline prototype uses transparent cutouts against changing dark/light stages and keeps the original identity names; it never writes app selection.

Browser-only smoke acceptance on the revised standalone prototype: Chromium / Playwright at **1360×1000 desktop** and **390×844 mobile**, all three cutouts and compact avatars decoded, three-character switching, celebration note, light-theme interaction, and **no page horizontal overflow**. This is **not** Android screenshot or installed APK acceptance, and the source images have not been committed as Android assets. Conversation-delivered standalone HTML and ZIP contain the latest media; the repository contains the written engineering/design review gate.

**Remaining**: review aesthetic vs real Spark, independently produce a true matched 9-pose set for each approved identity, prove all 12 state mappings, then bounded Android PR/CI and physical acceptance. Preserve V144 persisted IDs/keys, existing Spark WebPs and unchanged alarm/reminder system. [See state-by-state acceptance matrix](./v217-pose-mapping-acceptance-matrix.md).
