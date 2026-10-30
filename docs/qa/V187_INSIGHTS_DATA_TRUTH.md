# V187 — Insights data-truth candidate

Based on verified V186 source commit `c312652ec36fe31eda74507d949130c6ad6a799a`.

- Persist summary and prior-summary row presence, numeric metric keys and actual daily row dates. An absent metric displays as unavailable (—), not zero; a genuinely reported zero remains zero.
- Absent prior summaries no longer produce fabricated prior-period zeros. Legacy caches are marked for refresh before metric verification.
- Only an active, matching channel/request/creator-generation and core snapshot may publish audience foundation results. Tests must cover delayed updates and restores.
- Preserve genuine zero optional summary values without replacing them with values from another report.
- Remove visually misleading minimum-height bars for zero days. Keep functional status messages concise. Full minimal-copy UI redesign is V190.
- Preserve Home slideshow, project links and all creator work.

Pending and out of scope: independent Revenue and Reach Reporting atomic commits, financial metric/period contracts (V188), video catalog/cohort comparison (V189), complete four-tab minimal UI (V190), physical-device and live YouTube Studio parity.
