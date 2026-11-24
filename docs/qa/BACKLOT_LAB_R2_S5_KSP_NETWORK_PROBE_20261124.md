# Backlot Lab S5 — KSP artifact connectivity investigation
2026-11-24. Approved continuation; no Android build for this diagnostic.
Android15 QA run 45343275501: source fence passed; disposable app+test APK build failed after ~20s during Gradle configuration because Gradle could not resolve the existing plugin marker `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.1.20-2.0.1`.
This exact plugin previously worked in successful Kotlin compilation 45337219034 and three-class unit run 45340227725. It exists on Maven Central. Do not change Gradle plugin versions or repository URLs on inference.
Perform only small Ubuntu runner curl connectivity check to Maven Central marker, then decide whether to request a single retry of failed build job (not the full earlier pipeline). No Gradle/JVM work, no application source changes, no APK, no Supabase or release.
