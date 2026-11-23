# Backlot P10P — transition versus Reminder Hub tap: no-build mid-test screenshots

Prior evidence:
- P10M run 45202539065: app and AndroidTest APKs build PASS; test reaches Home Reminder Hub but fails at line 114 awaiting Creator Hub Personal tab.
- P10N run 45208442130: real Android system Quickstep launcher ANR modal obstructs Backlot while MainActivity is resumed, making that attempt inconclusive.
- P10O run 45209811923: improved emulator 4 cores/4096M RAM and warmup avoids visible Quickstep modal during early preparation, yet exact JUnit test still fails at the same line 114 waiting for Personal tab; end-of-test screenshot is launcher after JUnit teardown, not evidence of actual in-app state during the failed click.
- The app's P6CreatorEntryTransition is composed last after successful five-page setup and can consume one user tap to skip its full-screen presentation. It is **hypothesized**, not yet shown, that the test's premature Home tap dismisses this entry overlay without opening Reminder Hub.

P10P reuses byte-for-byte the successful P10M debug+AndroidTest APKs from run 45202539065 and 4-core Android15 emulator hardware profile; does **not** rebuild or edit production/test code. Captures screen at 4/7/11/17/25 seconds after unchanged JUnit starts, plus Activity hierarchy/logcat, **before** JUnit test teardown. Diagnostic runner terminates test after captures; that is not app failure/test pass. Evaluate actual screen and gate only a precise test or product correction backed by evidence.

No full JVM, merges, owner APK, live credentials, hosted data or release; protected stable V337 and main untouched.
