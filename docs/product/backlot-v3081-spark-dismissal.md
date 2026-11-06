# Backlot V308.1 — Spark individual notification dismissal

**Stacked development draft; not in V309 signed-owner QA candidate or an approved APK.**

## What is now implemented
- Individual Spark activity rows offer a **Hide this Spark update** control.
- This calls the existing `V221CompanionDismissalStore` for a 24-hour presentation cooldown, using a namespaced, SHA-256-hashed activity key. No Personal title or raw reminder ID is persisted to that preference.
- Only Spark's displayed preview is filtered. Existing Creator and Personal reminder records, incoming Android alerts, acknowledgement/Done/snooze paths, and the V306 Activity Center remain unchanged and fully accessible.
- If a user hides the top row, the next verified activity enters the top-three preview. A newly issued occurrence gets a distinct key and is not suppressed by a prior dismissed one.
- Spark user opt-out remains authoritative; this does not create background notifications or any new scheduler.
- Empty list remains empty, no invented content.

## Tests
- Six JVM tests for hashed key privacy, deterministic dismissal, new-occurrence refresh, top-three reflow, history fallback and canonical Activity Center isolation.
- One Android 15 emulator interaction/screenshot test verifying the close button, reflow, Personal-title concealment, See All link and non-destructive behavior.
- Restrictive `backlot-v3081-spark-dismissal.yml` workflow compiles the full app and tests while forbidding any data, cloud, reminder engine, schema or signing change.

## Status and release
- Experimental branch based on V308 PR #306. It is NOT included in V309 QA PR #307, whose source is deliberately frozen for signed upgrade testing.
- Only after both existing V306–V309 and this branch's checks are green should a new **separately versioned** signed candidate be evaluated. Do not mutate or silently replace the V309 verified source.
