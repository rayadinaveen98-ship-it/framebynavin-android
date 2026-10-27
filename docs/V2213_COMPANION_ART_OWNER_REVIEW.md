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
