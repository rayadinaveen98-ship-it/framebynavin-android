# FrameByNavin v122 — RC2 Insights V2 feedback pass

This branch preserves the validated v121 RC1 checkpoint and continues the real-user-feedback pass from actual daily use.

## Slice 1 — Insights V2 interaction and freshness

- Channel Pulse metrics are interactive: Views, Watch, Subs and Avg View open focused detail views.
- Drill-downs inherit the active 7/28/90-day range and show current vs previous period.
- Views, Watch and Subscriber drill-downs use real daily trend points from the YouTube snapshot.
- Daily Views opens a larger interactive chart; tapping a position reveals the exact date/value.
- Channel Signals are interactive and open focused context instead of forcing all detail into the overview.
- Cards expose chevrons where appropriate so tap behavior is discoverable.
- Insights remains cache-first. Cached data renders immediately.
- A connected Insights screen silently refreshes data when its current cache is at least 15 minutes old.
- Silent freshness checks never force a Google consent/resolution UI; manual Refresh remains the explicit recovery path.
- Existing video detail, YouTube linking, Opportunity Engine and 7/28/90-day behavior are preserved.

## Slice 2 — Creator drill-downs and honest workflow language

- Creator cards are explorable: Completed 30D, Active, Finished %, Videos Connected and Ideas Ready.
- Workflow, Workflow Intelligence and Creative Intelligence also open focused drill-downs.
- Historical bottleneck language describes measured elapsed time between workflow transitions; it does not claim the creator was actively working for the whole interval.
- Current active-project pile-up is described as present pressure and is not promoted into a repeated historical pattern without sufficient completed-transition evidence.

## Slice 3 — Personalized sequential New Project wizard

- New projects use a seven-step creator-friendly flow: project name, content type, platform, format, deadline, support level and review.
- Recommendations use the creator profile, creator mode, production style and platform defaults without hiding the full set of choices.
- The wizard stores normalized `CreatorContentDna`.
- Attention choices remain OFF / LIGHT / GUIDED and are separate from reminder scheduling.
- A new project still starts with `ReminderMode.NONE` and no reminder timestamp.
- Existing project editing remains on the established editor path.

## Slice 4 — 24H Pulse evidence and deeper Channel Signals

- The interactive `24H PULSE` signal now opens a dedicated evidence view instead of generic period context.
- The 24H view exposes the real stored-sample window, sampled views gained, subscriber movement, preceding comparable window when available, momentum and top-moving videos.
- Copy explicitly states that the 24H view is a sample-to-sample counter comparison, not a fabricated hourly analytics curve.
- Pulse-derived signals such as Momentum, Video Picking Up and New Subscribers reuse the same real 24H evidence.
- Matched Idea explains that the suggestion comes from meaningful-word overlap with the local Idea Vault and is not proof of demand.
- Top-video signals show measured period views, channel view share, watch time and the descriptive recent-video baseline multiple without presenting it as a quality score or causal explanation.
- Average-view signals show current vs previous period AVD only; no daily AVD series is invented.
- Workflow evidence is explicitly identified as local project-state evidence rather than YouTube performance evidence.
- The slice is UI/data-read-only: no persistence schema, reminder, authentication or security-foundation changes.

### Focused validation

Implementation commit: `7b285cd1f45f301ac226e17e9ef6ee5ea96d4e7f`

Validation workflow commit: `a74b205e91b07fc228b1058f8c31e6bc0d80b2f3`

GitHub Actions run: `34872489845` — `RC2 24H Pulse Signal Evidence Validate` — success.

Validated gates:

- `:app:testDebugUnitTest`
- `:app:compileDebugKotlin`
- `:app:assembleDebug`
- debug APK artifact upload

Artifact: `FrameByNavin-v2.0-rc2-24h-signal-evidence` (`10358843642`), workflow artifact digest `sha256:9855c1556fd08562c5ea3a15d47d70abc69acdd29bdc45dc462bd01a4011c4a4`.

This is focused RC2 slice validation. It is not a claim that the full RC release gates or physical-device acceptance matrix are complete.

## Data honesty

Daily analytics currently includes views, watch minutes and subscriber movement. Average view duration is period-level in the current model, so RC2 does not fabricate a daily average-view-duration chart. The 24H Pulse is independently derived from stored refresh samples and never pretends to be YouTube hourly analytics.

## Next slices

1. Google identity + Supabase automatic creator backup/sync, with Drive demoted to optional import/export.
2. Guided first-run tour after the final screen architecture is stable.
3. Run holistic RC2 CI/release validation after the remaining feedback slices are integrated.
4. Complete production-signing/OAuth/App Check and physical-device acceptance gates before public-release readiness.
