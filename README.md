# FrameByNavin Android

FrameByNavin is an Android-first, offline-first Creator OS built with Kotlin and Jetpack Compose.

## Current development

The last verified installable release is `1.8.0-product-alpha23` (versionCode 63), built from commit `4a206ef42697897c9e5c494d679e1be94e5a81a8` by GitHub Actions run `34054266713`.

The `feature/v1.8.1-hardening` branch preserves that history and contains an unverified hardening candidate. The candidate is not a release until its Android build, tests, and APK verification pass. The generated Alpha23 source, not the older checked-in v1.7.5 source, is the starting point for this work.

The stabilization scope covers publication timestamps, local persistence conflicts, backup and restore integrity, safe manual cloud backups, account identity, navigation, and accessibility. No production cloud migration is required by the current candidate.

## Build and verification

GitHub Actions is the authoritative Android build environment. The historical Alpha23 workflow remains available. The hardening workflow reconstructs the exact original audit source, applies a checksum-verified patch, and runs the Android verification gates. No production signing credentials are committed or required for development APK builds.

Do not use an unverified candidate APK for irreplaceable data. Preserve an independent backup before installing development updates.

The RC2 payload is stored in `hardening/rc2/part-01.txt` through `part-09.txt` and is reconstructed by decoding each part independently and concatenating the bytes. The XZ SHA-256 is `a5b92d5614ace5f45dd013987365185f74d086428851b9f324a897d19211e7cb`.
