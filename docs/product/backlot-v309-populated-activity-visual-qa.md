# V309 populated Activity Center — visual acceptance

**Synthetic UI QA only, not new product code, signed build, or Play Store release.**

The empty-state screenshots from successful V306 run `42187452479` were verified from real Android 15 Compose screenshots. They show coherent dark UI, large readable heading, safe insets and restrained card styling. This branch validates **populated layouts**, which the earlier V306 screenshots did not cover.

## Test plan

Test `V309PopulatedActivityVisualQaTest` runs **only on a disposable cleared Android 15 emulator**. It:
- Issues two real synthetic Creator occurrence tokens using `ReminderOccurrenceStore.issue`, then records an explicit unanswered occurrence and a delivery-failure record through canonical `ReminderAcknowledgementStore`.
- Creates two synthetic Personal reminders in canonical Personal DataStore: one presented current occurrence and one with a synthetic explicit Done receipt.
- Opens the actual production `V306ActivityCenterScreen`, without alternate mock UI or shipping a new feature.
- Captures pixel screenshots of **Needs Action** (3 cards), **All** (3 active + 1 confirmed Done), and **History** (1 confirmed Done).
- Confirms names and statuses are drawn from the proper domain evidence, and confirms UI viewing/filters do not remove Personal records or Done receipts.
- Verifies one-to-one identities, source-priority sorting and legibility through manual visual inspection after screenshots are uploaded.

## Important safety boundary

No owner phone, accounts, actual Android alerts, projects, photos, cloud or backup data are used. The app gets cleared on the throwaway emulator **before** fixture creation. There is no code change to production UI, reminders, DataStore, SQLite, cloud sync or APK signing. This test-only source is compared against the frozen V309 candidate SHA `727d0dd815a622d9b6fcfee9ec64b7ef81b0db92`.

## Release hold

V309 remains unapproved until V306–V308 exact-head CI, screenshot review including this populated fixture, V294→V309 direct signed upgrade and physical-device checks pass. V308.1 is excluded from the frozen V309 build.
