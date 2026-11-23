# Backlot Lab R2 — one-shot compile request

Date: 2026-11-23
Approved by owner: continue to the next R2 verification stage; do not merge stable, do not build owner APK, do not touch user data or backend.

Exact frozen application source: `1577669bc328153a365661a7f3d36fb1558e891c` on `integration/backlot-lab-v337-p10q-reconciled-20261123`.

This marker is the ONLY trigger for one focused QA pass, running the source-contract script and `:app:compileDebugKotlin -PbacklotLab=true` in GitHub Actions. All source paths under `app/` must match the frozen R2 commit byte-for-byte. If failing, inspect logs and repair only the failing stage rather than rerunning the full suite.

No release, upload, signing key change, user-data change, database migration, APK generation or stable promotion is authorized by this marker.

QA workflow-only correction: repaired shell command-substitution quoting in the no-build source fence; previous check failed before executing any Gradle task. This request retries that same one-shot gate without modifying app source.
