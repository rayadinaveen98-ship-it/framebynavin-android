# Backlot V217 — Nine identity/silhouette explorations (review, not implementation)

**Status:** Visual exploration / awaiting owner design selection. No Android runtime assets, persisted settings, companion IDs, reminder scheduling, backup data, or stable branches are changed by this document.

## Source truth checked 26 October 2026

- Approved **Spark** is `FUNNY`, using existing `app/src/main/res/drawable-nodpi/guide_funny_*.webp`. Its actual welcome artwork is the glasses-wearing creator character in a black hoodie, **not** a star mascot or robot. All five Spark WebPs remain untouched.
- Existing **Cute** artwork is a light-colored **kitten**, not a dog. The active raster renderer currently maps every Cute state to `guide_cute_welcome` despite other legacy pose assets.
- **Frame** and **Navi** use the Compose procedural character renderer in `V137CharacterSystem.kt`. The V144 route maps Frame/Navi to vector and Spark/Cute to raster.
- Current persisted IDs and choice key: `FRAME`, `NAVI`, `FUNNY`, `CUTE` under `backlot_character_identity_v151/selected_character`. Do not change these IDs/keys.
- Current **UI selection accents**: Frame `#FFC857`, Navi **coral** `#F04F54`, Spark `#8176E8`, Cute `#E99AAF`. A violet Navi or lilac Cute exploration is a **proposed palette study only**; it does not authorize changing app accent colors.

## First-round silhouette shortlist

These nine *deliberately simplified* vector explorations are a **shape and legibility test**, not Spark-quality production CGI, drop-in Android assets, or verified screenshots. A local standalone review prototype explores idle, listening, thinking and celebration expressions, selection of one option per identity, and 48/96/200dp size studies. Its prototype selections are ephemeral; they do not write app preferences.

| Identity | Option | Idea | Critical review point |
| --- | --- | --- | --- |
| Frame | **F1 Viewfinder** | Wide rectangular film-frame head, amber rim, calm focused eyes | Best recognizability; avoid resemblance to cheap robot/toy |
| Frame | **F2 Aperture** | Faceted/octagonal aperture shape, compact body | Strong silhouette; ensure film craft is not a literal prop |
| Frame | **F3 Director** | Tall asymmetric face / scene-marker cue | Professional but needs stronger distinction from F1 |
| Navi | **N1 Navigator** | Asymmetric hair, small compass marker, intentional gestures | Distinct from Spark at 48dp; confirm current coral vs proposed violet |
| Navi | **N2 Pathfinder** | Soft rounded bob, composed and friendly movement | Good calm expression but simplified shape may lose identity |
| Navi | **N3 Scout** | Headband / swept hair, purposeful explorer motion | Ensure hair never obscures eyes or small-size face |
| Cute | **C1 Studio Kitten** | Soft pale kitten, clear paw/ear landmarks, lilac tones | Feline silhouette and expression consistent with current species |
| Cute | **C2 Moon Kitten** | Squat/restful cat, slightly unusual ears | Risk of overly similar silhouette to C1 or canine interpretation |
| Cute | **C3 Comet Kitten** | More asymmetric ears and energetic feline stance | Expressive but avoid babyish exaggerated accessories |

**Status of options:** no owner selection yet. These variants are not approved for production replacement.

## Concept-quality corrections documented

Early AI inspiration boards were rejected as faithful identity references because they depicted a fictional star/robot Spark and a puppy Cute, neither matching actual source artwork. Do not use those boards as source-of-truth character sheets. Future image-generation briefs must be anchored to the **actual existing Spark/Cute assets**, not the generated fakes.

## Next design-production stage (approval gate before runtime)

1. Owner reviews silhouettes and selects or requests revisions to F1–F3, N1–N3, C1–C3. A hybrid may be preferred; do not force one choice.
2. Develop **2–3 final-quality 3D/CG look-dev variants per selected direction**, matching real Spark's *rendering quality*, not necessarily copying its body type. Show current Spark unchanged as the actual reference asset.
3. Review actual visual crop/alpha bounds at 48dp / 96dp / 200dp, in dark/light Home, restored original guide-choice onboarding, Daily Brief, idea and reward contexts.
4. Create reviewed 8+ pose equivalents for each selected identity with a deterministic mapping for the 12 `CinePulseState` values. Do not leave Cute on one welcome image.
5. Verify 900×1600 + 1.5x font, default 1080×2400, long Telugu, accessibility / reduced motion, memory/decode performance, selection-resume and backup regressions.
6. Only after explicit owner acceptance create a separate Android implementation draft PR; preserve Spark sprite checksums and V144 prefs. No stable/main promotion without #101 and #120.

## Scope boundaries

- Visual review only; **no** V217 production artwork approved yet.
- No alarm/voice wave engine, navigation reshuffle, XP hook, speech or AI changes here. They remain separate stages under [#126](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/126).
- This exploration follows [V217 PR #130](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/130) and [issue #129](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/129); the owner-ready whole-app visual matrix remains [#120](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/120).
