from pathlib import Path

UI_TEST = Path('app/src/androidTest/java/com/framebynavin/app/ui/V18CoreInteractionUiTest.kt')

text = UI_TEST.read_text()
needle = '            PTodayScreen(\n'
count = text.count(needle)
if count != 2:
    raise SystemExit(f'Expected 2 PTodayScreen test calls, found {count}')

text = text.replace(
    needle,
    needle + '                creatorName = "Test Creator",\n',
)
UI_TEST.write_text(text)

print('Applied Alpha9 instrumentation compatibility fix')
