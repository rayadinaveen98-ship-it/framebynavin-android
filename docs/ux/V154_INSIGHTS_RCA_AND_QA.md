# Backlot V154 — Insights freshness and separate Quick Create

Status: preview implementation, **physical-device checks pending**. Keep V153 untouched.

## RCA: Why October 11 appears on October 16

V153 uses `YouTubeAnalyticsQueryPolicy.PROCESSING_SAFETY_DAYS = 4`. The date is computed in `America/Los_Angeles`, not India. At Oct 16 morning IST, Pacific time is still Oct 15; Oct 15 minus four days = **Oct 11**. This is the end of the selected *Analytics reporting window*, not necessarily the date of the last successful fetch. The UI labels it FINALIZED and may leave a false impression that Backlot has not checked YouTube for five days.

## V154 changes

- Request the recent Analytics window ending two **Pacific** days before today (e.g., Oct 13 on Oct 16 morning IST). That date may still have processing changes. Never describe recent Analytics or estimated earnings as finalized.
- Present reporting range, last successful check time, and channel lifetime totals from YouTube Data API as distinct concepts.
- Refresh automatically when the expected Analytics end date changes, even if the cached fetch timestamp is under 15 minutes old. Keep the manual Refresh button.
- Retain the last stored snapshot and show an error if a refresh fails; don't fabricate numbers or roll the timestamp forward.
- New Project restores the original 7-stage progressive editor as the default. Projects exposes Quick Create as a distinct optional action. Both save through the original shared draft/validation path. Existing project Edit remains unchanged.

## Known limitations / expected behavior

- YouTube Analytics often lags Studio. **V154 is not realtime for daily views, engagement, watch duration, or earnings**; near-term numbers can revise. YouTube Data API channel totals may also lag Studio and are not a 48-hour realtime feed.
- The end date displayed is the requested reporting window end. It is not proof that YouTube has reported every value for that day.
- UI text, navigation and persistence need on-device verification. CI checks unit tests, lint, instrumentation **compilation**, assembly and artifact upload, not visual/device acceptance.

## Device acceptance checklist

- [ ] Open V154 over V153; Ideas, Projects, reminders, settings and account links remain.
- [ ] Go to Projects → New Project: seven-stage setup opens. Create and edit a project.
- [ ] Go to Projects → Quick Create: quick path opens. Validate title, platform/format reset, due dates and reminder persistence.
- [ ] Refresh Insights online and check report end date, last checked timestamp and lifetime channel totals against YouTube Studio where comparable.
- [ ] Try older cached data, no internet, token expiry and manual Refresh errors; never show refreshed timestamp on failure.
- [ ] Check 7D/28D/90D date windows and Pacific-vs-India midnight boundary.
- [ ] Check estimated revenue disclaimer and that incomplete Analytics is not labeled finalized.
- [ ] Check 100%, 130%, 200% font scale, focus/keyboard, back navigation, permission denied.
- [ ] Confirm screen reader and contrast behavior; no live/device QA claimed by CI.
