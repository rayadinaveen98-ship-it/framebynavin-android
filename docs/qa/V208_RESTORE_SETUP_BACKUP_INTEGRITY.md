# Backlot V208 — Restore full first-run setup and harden backup integrity

Build 232: 2.8.2-v208-setup-backup-preview. Stacked on V207-F. Draft, no stable promotion.

## Full first-run experience restored
- Account sign-in remains optional and cloud conflict recovery stays explicit.
- New creators are offered the original companion selection gate, with Frame, Navi, Spark and Cute, rendered using existing vector/raster assets.
- Previously saved companion IDs remain unchanged on upgrade. Returning users with completed setup are not forced through the gate.
- Original five creator setup pages are back: primary and secondary modes; publishing platforms; optional production styles; up to three goals + weekly target; optional reminder permission setup and summary.
- Original animated companion tip strip is shown on every setup page.
- Profile saving uses profile.copy() so existing auxiliary fields are preserved rather than replaced with defaults.
- All four-step optional in-app onboarding-guide lessons introduced in V207-D stay unchanged.
- Removed obsolete V207-C three-step setup code and tests; restored the five-page emulator acceptance flow.

## Backup integrity discovery and fix
- The previous portable ZIP preview read only the first backlot.json entry, then checked its JSON checksum. It did not stage or validate voice-media manifests, audio lengths or SHA-256 hashes until the irreversible restore action.
- Backup preview now stages the full archive, checks every voice-media checksum and ensures referenced Voice Idea IDs match the actual media entries. Failed validation always cleans staging files and never alters creator data.
- Restore performs the same checks again (the file may have changed after preview); legacy plain-JSON backup compatibility is kept.
- Helpful privacy-safe error messages distinguish mismatched media/checksums/unsupported versions from generic storage issues. No error copy exposes file paths, OAuth tokens, ideas or other private data.
- Corrupt/tampered backup files continue to fail closed. No checksum bypass, auto-delete or forced restore.

## CI / device acceptance
- JUnit companion roster/persisted selection tests and original five-page Compose onboarding regression tests.
- New Android tests: portable backup roundtrip, unexpected media, tampered voice checksum, plus existing backup restore/journal coverage.
- Full inherited unit, lint, signed APK verification, V200–V207 UI/emulator matrix on exact V208 commit; all old icon assets stay removed.
- Samsung new-install check: companion choice, every setup page, skip permission, guided tour, create reminder.
- Samsung upgrade check: existing accounts and saved guide kept, projects/reminders unchanged; export a fresh backup, preview it, and verify error details for any old failing backup.
- Never overwrite an existing backup file. If original backup validation still fails, obtain the exact on-screen failure step and ideally the non-sensitive error text; do not weaken authenticity or integrity to force import.

## Stop-ship
Any app/build/emulator failure, deleted creator data, lost guide identity, permission regression, bad checksum accepted as valid, or failed non-destructive upgrade prevents stable promotion.