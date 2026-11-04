#!/usr/bin/env bash
# All tests are on a disposable CI emulator, NEVER on an owner's phone.
# reactivecircus/android-emulator-runner executes YAML lines separately.
set -euo pipefail

OWNER=output/Backlot-V290-Private-Spaces-Owner-Test.apk
DEBUG=output/app-debug.apk
TEST=output/app-debug-androidTest.apk
for file in "$OWNER" "$DEBUG" "$TEST"; do test -s "$file"; done

echo 'Check signed V290 owner-preview APK is installable'
adb install -r "$OWNER"
adb shell dumpsys package com.framebynavin.app | grep -E -m 1 'versionCode=290'
# Instrumentation uses the identical package and preview signing identity.
mkdir -p app/build/outputs/apk/debug app/build/outputs/apk/androidTest/debug
cp "$DEBUG" app/build/outputs/apk/debug/app-debug.apk
cp "$TEST" app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

echo 'Personal V223-V288 regression classes'
bash .github/scripts/backlot-personal-instrumentation.sh \
  com.framebynavin.app.ui.V288PersonalOsUiTest \
  com.framebynavin.app.ui.V2232PersonalReminderUiTest \
  com.framebynavin.app.ui.V274SelfCareEditorUiTest \
  com.framebynavin.app.ui.V287NapWakeUiTest \
  com.framebynavin.app.ui.V286PersonalReadinessUiTest \
  com.framebynavin.app.ui.V285PersonalLifePlannerUiTest \
  com.framebynavin.app.personal.V285PersonalLifeNotificationsDeviceTest \
  com.framebynavin.app.ui.V284DietFitnessSetupUiTest \
  com.framebynavin.app.personal.V284DietFitnessPlanDeviceTest

echo 'Space persistence, recovery, privacy and V290 photo regression classes'
bash .github/scripts/backlot-v290-photo-instrumentation.sh

echo 'PASS: owner APK installs and all requested Personal, Spaces, photo and recovery tests pass'
