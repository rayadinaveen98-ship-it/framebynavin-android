# Backlot V217 — Companion Art Direction (Review Proposal)

**Status:** visual direction / engineering contract; **not implemented**. Owner selects and approves actual Frame, Navi and Cute artwork before swapping a single active character asset. Spark/\`FUNNY\` is the untouched production benchmark. Depends on scoped engineering acceptance of V216 PR #128. Roadmap: #129, overall package #126.

## Product intent
Every companion must feel like a recognizable assistant for a serious creator, not an infantilizing pet. Premium means high-quality shape design, lighting, balanced material/texture, mature expression, polished animation, and uncluttered staging. Character silhouettes must remain distinct at ~40–48 dp while remaining inviting at the 100–200 dp onboarding and Daily Brief scales.

## Character directions — initial review only

| Identity | Role and movement | Visual constraints |
|---|---|---|
| **Frame** | Film-craft companion: calm, focused, observant, deliberate | Film grammar hinted through framing/aperture/negative space, **not** a literal toy camera, over-detailed film reel, or copy of Spark |
| **Navi** | Creative navigator: curious, solution-oriented, active | Unique compass/trajectory/guide motif, modest asymmetry, slightly quicker head and direction changes; distinct head/body silhouette |
| **Cute** | Warm motivator: gentle, kind, emotionally intelligent | Soft small silhouette and confident clarity; warmth without baby proportions, overloaded pink, candy gloss or oversized cartoon accessories |
| **Spark** | Current approved baseline | **No asset changes, no palette changes, no code-path swap.** Existing \`guide_funny_*.webp\` remain byte-identical |

Initial character explorations should compare 2–3 diverse silhouettes per *new* identity. Do not approve based solely on attractive full-screen renders; include small and large app previews side-by-side against approved Spark.

## Shared animation contract

Source \`V129CinePulseGuide.kt\` defines: \`IDLE\`, \`WALK\`, \`POINT\`, \`PRESENT\`, \`LOOK\`, \`NOD\`, \`WAVE\`, \`THINK\`, \`LISTEN\`, \`SUCCESS\`, \`CELEBRATE\`, \`REST\`.

For each approved character, specify how every state maps to deterministic approved poses plus small smooth Compose animation, rather than relying on a generic welcome pose for all actions. Minimum visual expressions to approve: welcome/idle, attentive listen, thinking, present/talk, pointing, positive success, exuberant but controlled celebration, resting; map minor states consistently. Reward celebration is a later stage, but its pose must be prepared here.

Avoid more than necessary bitmap variants: transparent WebP artwork with uniform visible baseline, character centering and correct alpha, sensible decode size and memory. Use restrained motion with reduced-motion accessibility fallback. Character must not obscure primary button/IME/reminder controls.

## Current code + migration (confirmed 26 Oct 2026)

- \`V144GuideSystem.kt\`: persisted \`V144GuideIdentity\` enum IDs \`FRAME\`, \`NAVI\`, \`FUNNY\` (shown as Spark), \`CUTE\`. **Identity names and persisted selection keys do not change.**
- Current Frame/Navi use procedural \`V137CharacterSystem.kt\` \`BacklotGuideCharacter\`.
- Current Spark and Cute use \`V144RasterGuideCharacter.kt\`. Spark maps welcome/point/present/thinking/celebrate appropriately; Cute currently maps every state to \`guide_cute_welcome\` despite existing legacy listening/thinking/encourage/celebrate images.
- Current asset inventory: five Spark WebPs (~9–13 KB each) and five Cute WebPs (~10–13 KB each). All new assets must preserve acceptable runtime decode weight on a low-memory phone.
- Future implementation must route Frame/Navi/Cute through the reviewed pose renderer while keeping Spark code and assets completely unchanged; remove old resources **only after** verified zero production references, with fallback and backup migration tests.

## Approval checklist before code replacement
1. Distinct silhouette thumbnails at 40–48dp beside Spark. No confusion or excessive visual similarity.
2. Welcome / listen / think / celebrate pose sheet for each, then complete state mapping.
3. Dark Home, companion-choice onboarding, Daily Brief, reward toast, optional chat-launcher and low-end phone context proofs (including compact / 1.5x font).
4. Static/readability review with reduced motion, alpha crop bounds and long Telugu/English labels.
5. Owner approves identities, colors and expressive quality; **only then** replace artwork using a new bounded production PR.

## Excluded from V217 design stage
No alarm / voice particle engine edits, no voice/chat AI integration, no nav reshuffle, no XP runtime hook, no onboarding flow rewrite, no changes to personal-stable branch or user data. Those are separate stages under #126.

## Evidence and release gate
Design review is not a release approval. Implementation requires real emulator PNGs and physical phone/backup acceptance under #101 and whole-app screen matrix #120. Never treat mocked concept illustrations as current Android screenshots.
