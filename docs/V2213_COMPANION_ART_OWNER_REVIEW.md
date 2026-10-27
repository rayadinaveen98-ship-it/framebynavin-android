# Backlot V221.3 — companion artwork owner review

**Status: review concepts only; not production-approved or routed into the live companion.**

This document records findings from *real Android screenshots* in [Actions #40518155073](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40518155073), not from an AI illustration of a future UI. The 12 real screenshots are available in [art artifact #12741690979](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/40518155073/artifacts/12741690979).

## What exists and is verified

- 27 separate Android VectorDrawable expression studies: nine each for **Frame**, **Navi**, and **Cute**, in `app/src/main/res/drawable/guide_v2213_*.xml`.
- Expressions are `IDLE`, `WELCOME`, `LISTEN`, `THINK`, `POINT`, `PRESENT`, `ENCOURAGE`, `CELEBRATE`, `REST`.
- `V2213PoseCatalog` maps the historical `CinePulseState` enum to these expression studies and maps Spark/FUNNY to **null**.
- Android CI decoded and rasterized every drawable, detected 27 unique pixel fingerprints, required meaningful visible pixels, and captured galleries on 1080-wide/default and 900-wide/1.5× text screens.
- The original `V217LiveCompanionCharacter` and `V144GuideCharacter` in onboarding, Settings, and the real app still load the earlier full-color companion illustrations. **Spark's original art and behavior were not modified.**

## Visual review: facts versus approval

Actual screenshot inspection confirms all 27 designs are readable and distinct as **2D vector studies**. However, they **do not preserve the three-dimensional finish, materials, facial depth, or detailed silhouettes of the existing V217 character illustrations**. This is particularly visible in:

| Guide | Existing production art | V221.3 draft | Art-direction decision |
| --- | --- | --- | --- |
| Frame | Rounded dark metallic figure with copper/gold details and depth | Gold-outlined flat cartoon robot | **Not approved for live replacement** |
| Navi | Full-color youthful explorer with navy/dark outfit and dimensional body | Flat head-and-jacket icon | **Not approved for live replacement** |
| Cute | Cream-colored fluffy kitten with modeled fur and gentle face | Flat cream cat icon with colored eyes | **Not approved for live replacement** |
| Spark | Original FUNNY raster assets, user-selected and already live | No new vector study | **Keep unchanged** |

Uniqueness tests prove that the drawings are different, **not** that they meet the original visual quality bar. A model-generated status slide is not a screenshot or proof of the shipped Backlot UI.

## Required art-production gate before V221.4

1. **Identity lock:** new poses preserve each character's face, palette, recognizable proportions and material finish relative to the existing V217 reference. Spark is never restyled as part of this phase.
2. **Nine independent performances per character:** facial expression plus meaningful gaze, limb and posture changes. Never one picture rotated or scaled nine times. Preserve the identical character's continuity across all states.
3. **Deliverable quality:** independently reviewable transparent assets at useful Android display sizes; clean contours, no halos/cropping, sufficient contrast on Backlot dark backgrounds, predictable decode cost and byte size.
4. **Semantic accuracy:** `LISTEN`, `THINK`, `POINT`, `PRESENT`, `ENCOURAGE`, `CELEBRATE`, `REST`, `WELCOME`, `IDLE` read clearly at small rendered sizes without ambiguous props.
5. **Owner aesthetic approval:** view original and replacements at the *same rendered size* and on compact Android. Sign off each guide's 9-pose sheet before touching `V217LiveCompanionCharacter`.
6. **Integration tests:** only after approval wire the existing guide identity and state to new artworks, with real onboarding/Settings/Home tests, reduced-motion support and no saved-profile or reminder changes.

## Explicit release decision

**V221.3 pose catalog and vector review gallery are accepted as development/reference assets only. Production artwork replacement is BLOCKED on art approval.** Do not automatically merge this PR or start visual replacement in V221.4. The read-only V221.1/V221.2 decision and dismissal logic can remain independently tested.

The user's V220.4.1 device/backup acceptance remains a separate release gate; this review does not claim it is closed.


## Identity-locked review checkpoint — 27 October 2026 (continuation)

The current approved V217 art was retrieved and inspected directly. Its defining visual identity is:

- **Frame:** compact chibi metallic robot, rounded black glass face with bright amber eyes, copper/gold armor rings, distinctive segmented head/ear cups, aperture mark on its torso and substantial robotic boots. **Never reinterpret Frame as a human boy.**
- **Navi:** single young explorer with tousled dark hair, navy blue hooded jacket, orange accents, a small compass/star insignia, dark cargo trousers and sneakers. Preserve this specific young explorer's face, clothing and proportions through every performance. **Never switch Navi into a different person or gender presentation.**
- **Cute:** fluffy cream kitten with large violet eyes, subtle grey tabby markings, pink nose/collar, small gold charm and a long fluffy curved tail. **Never replace Cute with a robot or change species.**
- **Spark:** all five original `guide_funny_*.webp` images remain unchanged; do not create or route replacement assets.

A generated premium-style exploration did **not** maintain these identities (human Frame, different Navi, robotic Cute) and is therefore **rejected as character artwork**. Lighting quality alone is not an acceptance criterion. Never treat a generated concept board as proof of in-app integration.

### Implemented review-application safeguards

- Each of the nine expressions now displays **the approved original and the draft pose side-by-side at exactly 96dp** in the same review row. The original is intentionally repeated for all nine poses so that facial identity, silhouette, materials and costume remain directly comparable at eye level. This replaces the earlier 3x3 grid that showed a single, differently sized original.
- Existing Android emulator UI tests assert an **identical rendered width and height** for original and candidate at both first and last poses, as well as genuine normal/compact-window screenshots. These tests verify comparison fairness and rendering; they do **not** assert artistic similarity.
- CI now checks the exact **Git blob identity of eight untouched original WebPs** (three V217 designs and five Spark images). A changed original fails immediately, preventing accidental artwork drift. Those reference values are fixed from the approved source branch, not calculated from whatever new files are submitted.
- The review-gallery code is isolated, and the CI continues to reject accidental wiring of the V221.3 experimental catalog into the existing V144/V217 production renderer.

### Remaining blocked work

**The 27 vector drawings remain unapproved flat 2D studies.** We have not yet authored or integrated an identity-faithful final 27-frame premium illustration pack.

For an acceptable art production pass, create the Frame/Navi/Cute poses using the originals as direct visual references; validate identical face, silhouette, palette and costume/material for every state; render transparent previews at a consistent scale; and compare all nine poses per identity on normal and compact Android before requesting owner approval. After approval, convert to optimized display assets, add lightweight transitions with reduced-motion fallback and take real Home/onboarding/Settings screenshots on-device. Spark must remain byte-identical. V221.4 integration and the V220.4.1 owner device/backup gate are still separate.

**Verification status:** source and tests committed to the development branch; new Android CI acceptance is required for this new checkpoint. Do not label it green until the exact commit run completes. Main/stable and owner-installed APK remain untouched.
