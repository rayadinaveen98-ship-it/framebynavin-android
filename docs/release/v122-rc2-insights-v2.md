# FrameByNavin v122 — RC2 Insights V2 feedback pass

This branch preserves the validated v121 RC1 checkpoint and begins the real-user-feedback pass from actual daily use.

## Slice 1 implemented

- Channel Pulse metrics are interactive: Views, Watch, Subs and Avg View open focused detail views.
- Drill-downs inherit the active 7/28/90-day range and show current vs previous period.
- Views, Watch and Subscriber drill-downs use real daily trend points from the YouTube snapshot.
- Daily Views opens a larger interactive chart; tapping a position reveals the exact date/value.
- Channel Signals are interactive and open focused context instead of forcing all detail into the overview.
- Cards now expose chevrons where appropriate so tap behavior is discoverable.
- Insights remains cache-first. Cached data renders immediately.
- A connected Insights screen silently refreshes data when its current cache is at least 15 minutes old.
- Silent freshness checks never force a Google consent/resolution UI; manual Refresh remains the explicit recovery path.
- Existing video detail, YouTube linking, Opportunity Engine and 7/28/90-day behavior are preserved.

## Data honesty

Daily analytics currently includes views, watch minutes and subscriber movement. Average view duration is period-level in the current model, so RC2 does not fabricate a daily average-view-duration chart.

## Next slices

1. Finish 24H Pulse dedicated drill-down and deeper Channel Signals evidence.
2. Creator tab card drill-downs and creator-friendly slowdown language.
3. Sequential, preference-aware New Project wizard that opens Workspace immediately.
4. Google identity + Supabase automatic creator backup/sync, with Drive demoted to optional import/export.
5. Guided first-run tour after the final screen architecture is stable.
