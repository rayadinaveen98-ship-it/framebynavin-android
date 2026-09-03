# FrameByNavin Android — A2 Today Prototype

Native Android / Kotlin / Jetpack Compose prototype for the locked **A — Cinematic Editorial** direction.

## What is implemented
- Real Compose `TodayScreen`
- Cinema-black + REC-red + warm-ivory + muted-gold visual tokens
- Editorial hero: `MAKE THE FRAME COUNT.`
- Publish card + aperture motif
- Current production task + progress
- Next action + publishing buffer
- Focus Mode CTA
- Weekly completion strip
- Bottom navigation
- Android Studio `@Preview` at 390×844dp
- GitHub Actions workflow that builds a debug APK automatically

## Production realism
There are no premium UI assets, remote images, generated fantasy layers, or paid dependencies.
The visual effects are Compose primitives: gradients, Canvas arcs, borders, shapes, typography, and standard Material icons.

## Build
With Android Studio: open this folder and run the `app` configuration.

With GitHub: pushes to `main` build `app-debug.apk` and upload it as an Actions artifact.
