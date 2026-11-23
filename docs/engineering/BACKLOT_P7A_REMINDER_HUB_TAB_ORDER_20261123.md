# Backlot P7a — Reminder Hub Tab Order Correction

**Founder request:** The Reminder Hub opens Creator by default while the shared switcher shows Personal first and Creator second. Authorized choices: reverse the positions or change the initial destination to Personal.

**Chosen implementation:** Reverse the shared tab order to Creator (first/left) and Personal (second/right). Creator remains default because Backlot is Creator OS first; Personal remains a fully accessible well-being feature. This is the least disruptive correction.

**Status:** SOURCE-STAGED / NO COMPILATION / NO TEST EXECUTION / NO APK / NO MERGE.

- Repo: rayadinaveen98-ship-it/framebynavin-android
- P7 parent: 916a92a38714e9f97dc303b94d2d6f84c51ea148
- Branch: feature/backlot-v337-point07a-reminder-hub-tab-order-after-p7-20261123
- Owner accepted stable V337: 815ba69775f75b6e5f25bb838c2c17b4f09c203e; main unchanged.
- Only BacklotReminderHubChrome.kt changes shared visual tab order. No change to selected enum, callbacks, navigation, reminder engine, cloud, saved data, or onboarding.
- Existing click tags v2232-open-personal / v2232-open-creator remain attached to the same respective destinations.
- Creator Reminders screen still passes selected = CREATOR; Personal Home passes selected = PERSONAL. Their Back/Creator callbacks are unchanged.
- P7 care intro in Personal Home remains intact.
- On the later founder-approved consolidated QA, verify Creator entry selected first, Personal second, switching both ways, Back/Activity links and no onboarding loop.
- No P8 source started.
