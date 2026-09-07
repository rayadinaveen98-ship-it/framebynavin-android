# Piano Studio professional foundation

This is the isolated piano application at `piano-ci/`. It is not the FrameByNavin application in the repository root. The existing Git history and the unrelated main branch must remain intact.

P0 modules: `:app` (application and legacy compatibility adapters), `:core:designsystem` (one theme and tokens), `:core:music` (music primitives), `:core:data` (settings and practice-session persistence). The application still contains the R2 player implementations; they are preserved for regression stability and will be consolidated in P2. The Android native engine remains in the application module pending the unified audio migration. These are known migration boundaries, not claims of completed feature modularization.

The existing application ID, database file/schema and DataStore keys are intentionally unchanged. Do not wipe user data or change the signing key as part of architecture cleanup. Release builds require a stable signing identity; debug builds are for testing only.

The single production route set for P0 is StudioHomeActivity, LearningHubActivity, R2FreePianoActivity, R2PracticeActivity and R2LessonActivity. Superseded activities are removed from source and manifest. The R2 JNI bridge is retained until one audio interface replaces it; all native calls must be verified against the packaged library and exercised in an emulator or real device.

Do not mix compiler/dependency migration with a speculative UI redesign. Each release must report its exact commit, version, build result, native verification, runtime/screenshot checks and SHA-256. A successful Gradle build does not establish real-device audio quality or visual approval.
