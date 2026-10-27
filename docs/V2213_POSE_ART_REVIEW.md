# Backlot V221.3 — Companion pose art review

**Status:** Staged vector **concepts**, not approved production replacement.
**Parent:** V221.2 `feature/backlot-v2212-companion-behaviors`.
**Non-negotiable:** The original Spark / FUNNY WebPs, companion selection, historical preferences and `V144RasterGuideCharacter` remain untouched.

## New artwork and what has not happened

There are **27 individually drawn Android VectorDrawable XML assets** in
`app/src/main/res/drawable-nodpi/guide_v2213_{frame,navi,cute}_{pose}.xml`.
Nine distinct head/body/limb/face/decor compositions are authored for each
identity. They are **draft interpretations**, not copies of the approved
V217 WebP concepts; owner aesthetic approval is needed before they can become
production assets. The image generator was unavailable during this checkpoint,
so these are native hand-coded vector concepts rather than generatively
matched illustration packs.

Each identity has:

| Pose | State source | Distinct physical cue |
|---|---|---|
| Welcome | WAVE | Raised greeting arm, wide friendly eyes |
| Listen | LISTEN | Hand at ear, alert pupils |
| Think | THINK / LOOK | Hand near chin, looking up, thought dots |
| Point | POINT | Extended directing arm, arrow gesture |
| Encourage | NOD | Uplifted arms, winking/encouraging expression |
| Present | PRESENT | Two outward open palms and presentation rays |
| Celebrate | SUCCESS / CELEBRATE | Both arms overhead, wide smile and confetti |
| Rest | IDLE / REST | Lowered shoulders and arms, closed eyes |
| Move | WALK | Offset legs and swinging arms, movement lines |

## Isolation and rollout

- `V2213PoseCatalog` maps every legacy `CinePulseState` to one of nine
  semantic poses, then to a unique drawable by guide identity.
- `V2213PoseReviewGallery` previews 9 drawings in a grid for one identity.
- **No production routing is changed.** `V144GuideCharacter` still loads
  the approved V217 single-image artwork for Frame/Navi/Cute and the original
  Spark WebPs for FUNNY. This is a deliberate visual-signoff gate.
- All 27 resources must inflate on Android, have distinct resource IDs, and
  produce nonblank screenshots at default and 900px/1.5× font profiles.
- After owner approves or revises these concepts, swap *only* the Frame/Navi/
  Cute drawable path through a separate PR with real onboarding/Settings/
  guide regression screenshots, no automatic changes to existing selection.

## Art signoff checklist

- [ ] Frame still feels like the chosen FRAME design.
- [ ] Navi still feels like the chosen NAVI design.
- [ ] Cute still feels like the chosen CUTE design.
- [ ] All nine poses visibly differ beyond simple rotation/scale.
- [ ] Transparent vector artworks have no unintended solid backdrop.
- [ ] Spark visuals and saved character preference remain identical.
- [ ] Review default, compact and reduced-motion settings before live routing.
