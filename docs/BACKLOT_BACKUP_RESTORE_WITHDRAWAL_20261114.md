# Backlot — Withdraw Backup and Restore, Keep Local Erase

Owner decision on 2026-11-14 supersedes all prior Package J unified backup/restore completion plans.

- **Removed from the app interface:** Settings Backup & storage; Local Data & Backup; Cloud Sync backup card; Creator and Personal manual backup/restore destinations; private recovery-copy browser; Backup/Restore buttons under Data & Privacy; old legacy Settings backup cards; startup Backup Tools shortcut.
- **Disabled:** existing periodic and on-demand Creator cloud backup uploads. Any old queued cloud worker runs as a no-op, and the app cancels previously scheduled unique worker instances.
- **Retained:** Advanced > Erase local data, with typed phrase, five acknowledgements, last-moment active-recovery safety inspection and Android's local system clear. No backup UI appears inside Erase.
- **No destructive migration:** Existing on-device data, old backup files, cloud account data, downloaded external files, and existing encrypted recovery evidence are NOT deleted by installing this app update. Only a separately confirmed local erase wipes app-private content.
- **Compatibility:** Canonical backup parsers and recovery journals remain compiled as dormant compatibility/safety internals for existing installations. This withdrawal does NOT claim to delete old records or remove any user data. No restore or backup UI is registered in AndroidManifest.
- **Branch isolation:** Based on pre-finalization `feature/backlot-postcw6-reliability-unified-20261113`, explicitly excluding the later Package J user-facing restore-completion work. Owner-approved local erase safeguards are preserved.
- **Pending validation:** Full JVM suite; Android 15 Settings/Privacy UI absence; manifest absence; guarded erase (no destructive test on real phones); signing/package/version; in-place owner upgrade.
- **Future design:** No new backup/restore mechanism is planned or enabled until owner explicitly chooses a different approach.
