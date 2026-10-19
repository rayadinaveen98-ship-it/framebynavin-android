#!/usr/bin/env bash
set -euo pipefail
reminders=app/src/main/java/com/framebynavin/app/reminders
ui=app/src/main/java/com/framebynavin/app/ui
scene="$reminders/V195ReminderParticleScene.kt"
motion="$reminders/V202PremiumParticleMotion.kt"
main="$ui/FrameByNavinV101BApp.kt"
test -f "$motion"
test -f "$ui/V203ReminderSetFeedback.kt"
test -f "$ui/V203ReminderSavedChip.kt"
test -f app/src/test/java/com/framebynavin/app/reminders/V202PremiumParticleMotionTest.kt
test -f app/src/test/java/com/framebynavin/app/ui/V203ReminderSetFeedbackTest.kt
grep -Fq 'NORMAL_COUNT = V197AmbientGeometry.NORMAL_COUNT * 2' "$motion"
grep -Fq 'LOW_POWER_COUNT = V197AmbientGeometry.LOW_POWER_COUNT * 2' "$motion"
grep -Fq 'const val DEFAULT_DOTS = 840' "$reminders/V195ParticleGeometry.kt"
grep -Fq 'const val LOW_POWER_DOTS = 440' "$reminders/V195ParticleGeometry.kt"
grep -Fq 'V202InfiniteBounce.position(' "$scene"
grep -Fq 'V202InfiniteBounce.colorBlend(' "$scene"
grep -Fq 'withFrameNanos' "$scene"
grep -Fq 'drawCircle(pigment.copy(alpha = .88f * scrimmed * depth)' "$scene"
grep -Fq 'snapshotFlow {' "$main"
grep -Fq 'vm.durableProjectSave(pending.taskId)' "$main"
grep -Fq 'receipt.revision > pending.baselineRevision' "$main"
grep -Fq 'persist(committedProjectId = task.id)' app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt
grep -Fq 'persist(committedProjectId = updated.id)' app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt
grep -Fq 'durableProjectSaves[taskId] = DurableProjectSave(nextRevision, saved)' app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt
grep -Fq 'vm.writeError' "$main"
grep -Fq 'V203ReminderSetFeedback.message(' "$main"
grep -Fq 'V203ReminderSavedChip(' "$main"
grep -Fq 'reminderEnabled' "$main"
! grep -Fq 'delay(2_100L)' "$reminders/ReminderCheckInScreen.kt"
echo 'V195.5 smooth 2x particles and durable reminder countdown contracts passed.'
