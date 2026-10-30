# Backlot V189 — Video catalog and detailed analysis candidate

Baseline: V188 automated-test-passing source commit `9fa8e88b51d129a3710aaa1ecbc59c6b26ac09f2`.

## Implemented
- A video catalog in Content that fetches actual uploads in pages of 25 from the selected channel, beyond the previous top-10/recent-20 sample.
- Uses the same read-only YouTube authorization; verifies channel identity and playlist before returning results. Page replies are discarded if local session, channel, reporting range or creator-data generation changes.
- Page results remain transient, not mixed into the persisted creator project database. Search/filter operates on **loaded pages**, and the UI labels loaded counts rather than claiming a full-channel search.
- Search by title, filter All / Linked / Unlinked / Reported, and sort by publication date or available reporting views. Unreported values are missing rather than zero.
- One full-screen video workspace, available with or without a linked project: Results, Reach, Retention and Project. Data unavailable is displayed as a dash.
- Existing project linking is preserved. Home and Overview video navigation use the same full-screen detail.
- Dangerous unverified "above/below your usual" comparisons are not shown inside the new catalog/detail. Format-matched age-equalized cohorts remain unavailable until API provides authoritative format and upload-age measurement.
- No extra introductory or technical paragraphs on new screens. Existing global Insights copy gets its full audit/redesign under V190.

## QA gates
Full source compatibility guards, JVM tests, lint, debug + androidTest compile and personalStable APK; Android 11 emulator: inherited 23 + 8 + 3, plus V189 Compose navigation tests. Then physical-phone check, pagination with a large channel, slow network, token expiration and live authorized comparison.

## Open limitations
- Search applies only to fetched pages; no unverified claim of server-wide search.
- Shorts classification and same-format age-normalized comparisons require authoritative platform data; currently avoid guessing from video duration.
- Retention from the stored first sampled videos is shown, but on-demand retrieval for arbitrary catalog videos is not yet added. Missing retention remains unavailable.
- App-wide minimal copy, typography, unified screen layout and other cross-feature analytics remain under V190.
- Draft candidate is not merged, device verified or stable. Preserve Home slideshow, creator history and signing.
