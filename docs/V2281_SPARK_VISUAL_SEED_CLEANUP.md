# Backlot V228.1 — Spark-only companion and safe sample removal

Base: V227.3 full app build 252. V228.1 engineering build 253; main/stable unchanged.

- Onboarding + Settings expose Spark alone. Frame/Navi/Cute legacy identity enums, imagery and stored choices remain for compatibility. Their current runtime visual falls back to Spark, with no deletion of the persisted preference.
- Spark artwork unchanged. Home portrait 48→72dp, internal image 43→64.5dp, onboarding 96→144dp, Settings 82→123dp, suggestion card 54→81dp. Removed duplicated sheet portrait+label; empty state shows 78dp Spark.
- Vibrance: selectively stronger glow, lighter reveal scrim and more saturated backdrops on Creator Voice/Alarm; improved Personal wave halo and dot opacity. Retain dot positions, reduced-motion, low-power budgets and reminder controls.
- New install weekly schedule remains empty. Existing **entire unchanged** FrameByNavin V5 preset is removed. Modified, partial and mixed legacy schedules are preserved. Never erase existing user projects, account data, backup, creator rewards or personal reminders.
- Tests must confirm first install, exact legacy preset, and modified schedules, existing voice/alarm delivery and new companion layout at compact/200% text devices.
- Device/Play approval still requires full CI, physical phone, screenshot and upgrade tests. No APK is considered verified merely from source commits.
