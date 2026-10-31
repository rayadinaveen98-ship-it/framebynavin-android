# Backlot V275 — real widget previews (draft)

This branch is based on verified immutable Build270 Phase1 source and does NOT alter existing widget providers, pinned widget contents, APK signer or real Creator/Personal data.

## In Backlot settings: Your widgets

The existing widget migration / Add to Home Screen screen now displays a read-only visual preview immediately above each Add action for all three supported modern widgets:

1. Quick Capture — directly inflates `R.layout.widget_v224_capture`.
2. Today Focus — directly inflates `R.layout.widget_v224_today`, with synthetic creator/project labels.
3. My Day — directly inflates `R.layout.widget_v224_myday`, with counts-only synthetic Creator/Personal summaries (no personal titles or notes).

These are the same Android RemoteViews layouts declared by the actual widget provider XML via `android:previewLayout`, not mock screenshots. They use example data, do not invoke any pinned widget action, and do not inspect user records. If a launcher rejects preview inflation, the UI gracefully displays a descriptive fallback message rather than crashing.

Android preview inflation is tested with an Android11 instrumentation suite on synthetic data. Existing request-pin actions remain opt-in and never declare success until the launcher accepts the request.

## Release blockers

This feature branch remains draft; no new owner's fully integrated APK has been built or distributed. Must test actual launcher widget pin previews on Samsung/Pixel and compact phone/tablet, accessibility labels and resizing, and integrate with separate audio + hourly reminder work before owner sideload. Older legacy widget styles remain listed in the migration screen; no automatic removal or mutation is authorized.

No paid API, cloud, external domain or credential required.
