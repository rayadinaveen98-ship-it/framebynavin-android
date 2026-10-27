# Backlot V190 — professional minimal Insights redesign

Based on the **CI-passing V189** candidate `220a1a12413813215baecf5fbcb751d7560efd61`.

## Included
- Remove the app-generated Insights hero, generic welcome explanation and duplicated channel profile block.
- Keep concise, truthful data freshness, account controls and reporting period in the connected-channel header.
- Overview: 2×2 readable core metrics, daily chart, up to two linked evidence highlights, top videos. Audience, Reach and Next Move remain accessible via the explicit *Audience & opportunities* disclosure.
- Content: user-initiated paginated catalog, short search and filter labels, horizontal chips on compact devices, correct unavailable state; no change to the V189 session safety contract.
- Revenue: named metrics, compact earnings trend, format split and top earners. Remove duplicate headers, the redundant Best Day card, empty Shopping panel and explanatory Top 3 concentration paragraph; retain a labeled top-three-share metric when supported by data.
- Creator: progress metrics first; workflow, creative patterns and content types accessible on user demand instead of landing-page narrative.
- Video detail: preserve the full-screen Results / Reach / Retention / Project workspace with readable labels and no explanatory introductions.
- Drilldowns, workflow, patterns and opportunities use concise labels, values and purposeful actions rather than generic instruction paragraphs.
- Text labels use a consistent readable floor in Insights and core drilldowns. Keep the existing cinematic color palette, icons, Home slideshow, personal data and explicit consent/retry/error notices.
- Shared expandable section is keyboard/screen-reader accessible with a meaningful expand/collapse content description.
- Source guard and Android UI regression suites check the key screens and preserve navigation.

## Validation
Build, lint, unit tests, inherited 36 Android emulator regressions, plus 3 new V190 Compose checks. Compare the final visual layout, navigation, font scaling and rendering on a physical Samsung device before stable release.

## Explicitly pending
- V189's page-load/account authorization and video-format classification limitations still apply.
- Revenue and Reach depend on YouTube authorization, permission and reporting availability; do not turn unavailable fields into fake zeros.
- Authentic live YouTube Studio parity, real-device performance and upgrade-without-data-loss tests are still pending. Draft PR / preview APK is **not** stable.
- A Settings → Guide explaining definitions is optional future work and not added during V190.
