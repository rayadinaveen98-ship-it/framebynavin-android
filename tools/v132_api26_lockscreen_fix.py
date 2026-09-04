from pathlib import Path

paths = [
    Path('app/src/main/java/com/framebynavin/app/reminders/AlarmActivity.kt'),
    Path('app/src/main/java/com/framebynavin/app/reminders/VoiceReminderActivity.kt'),
]

old = '''        setShowWhenLocked(true)\n        setTurnScreenOn(true)\n        window.addFlags(\n            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or\n                android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON\n        )'''

new = '''        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {\n            setShowWhenLocked(true)\n            setTurnScreenOn(true)\n        } else {\n            @Suppress(\"DEPRECATION\")\n            window.addFlags(\n                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or\n                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON\n            )\n        }\n        window.addFlags(\n            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or\n                android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON\n        )'''

for path in paths:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f'lock-screen block not found in {path}')
    text = text.replace(old, new, 1)
    path.write_text(text)

print('Patched API 26 lock-screen compatibility in both reminder activities')
