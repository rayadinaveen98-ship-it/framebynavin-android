#!/usr/bin/env bash
set -euo pipefail
root=app/src/main/java/com/framebynavin/app/reminders
ambient="$root/V197AmbientGeometry.kt"
glyph="$root/V195ParticleGeometry.kt"
scene="$root/V195ReminderParticleScene.kt"
motion="$root/V198ParticleMotion.kt"
test -f "$motion"
test -f app/src/test/java/com/framebynavin/app/reminders/V198ParticleMotionTest.kt
grep -Fq 'private const val NORMAL_COLS = 26' "$ambient"
grep -Fq 'private const val NORMAL_ROWS = 42' "$ambient"
grep -Fq 'private const val LOW_POWER_COLS = 17' "$ambient"
grep -Fq 'private const val LOW_POWER_ROWS = 29' "$ambient"
grep -Fq 'palette = (noise(index, 173) * 6f).toInt().coerceIn(0, 5)' "$ambient"
grep -Fq 'depth = (row + 2 * col) % 3' "$ambient"
grep -Fq 'const val DEFAULT_DOTS = 420' "$glyph"
grep -Fq 'const val LOW_POWER_DOTS = 220' "$glyph"
grep -Fq 'CYCLE_MILLIS = 3_800' "$motion"
grep -Fq 'V198ParticleMotion.colorMix(' "$scene"
grep -Fq 'V198ParticleMotion.iconPulse(time)' "$scene"
grep -Fq 'lerp(' "$scene"
grep -Fq 'val glyphColor = if (kind == ReminderCheckInKind.ALARM)' "$scene"
grep -Fq 'if (dot.depth == 2 && !lowPower)' "$scene"
grep -Fq 'if (revealed) Modifier else Modifier' "$scene"
grep -Fq 'if (revealProgress < 1f)' "$scene"
! grep -Fq 'delay(2_100L)' "$root/ReminderCheckInScreen.kt"
echo 'V195.3 ambient density, frame continuity, hue blending and icon-visibility guards passed.'
