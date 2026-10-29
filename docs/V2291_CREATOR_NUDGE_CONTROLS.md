# V229.1 — Existing Creator Nudge Controls

Backlot already has an opt-in `CreatorContextNudgeWorker`, the V201 9:00–20:00 daytime window, 24-hour global cooldown, 72-hour repeated-project-key cooldown, V204 freshness, and V205 notification "Pause 7 days" action. The new slice does **not** create another notification scheduler.

Adds a visible PAUSE 7 DAYS / RESUME control in Automation > Helpful reminders, with an accurate paused/blocked/active/off status. Both actions reuse the existing private `creator_context_nudge_state` preference key, so pause survives restarts and the worker already respects it. Resume clears only the pause deadline; it does not reset cooldown history, auto-enable nudges, turn on permissions, or immediately send a notification.

Existing real Creator Task and Personal Reminders do not consult this pause flag and must continue unaffected. Existing opt-in default remains off.

JVM tests verify status, pause duration, blocked priority and boundaries. Android UI tests verify the pause/resume callback and label. Build 258 stacked on V228.5; same app ID/signature, no schema or backup migration. Physical-device Doze, lockscreen and OEM notification permission tests remain necessary before stable release.
