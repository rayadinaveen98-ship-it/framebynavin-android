from pathlib import Path

ROOT = Path('.')
ROOT_UI = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
CALENDAR = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V15ContextUi.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


root = ROOT_UI.read_text()
root = replace_once(
    root,
    '    val focusTask by remember { derivedStateOf { vm.tasks.firstOrNull { it.id == focusTaskId } } }',
    '''    val focusTaskState = remember { derivedStateOf { vm.tasks.firstOrNull { it.id == focusTaskId } } }
    val focusTask = focusTaskState.value''',
    'focus task smart-cast compatibility',
)
ROOT_UI.write_text(root)

calendar = CALENDAR.read_text()
calendar = replace_once(
    calendar,
    'import androidx.compose.runtime.Composable\n',
    '''import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
''',
    'calendar runtime imports',
)
CALENDAR.write_text(calendar)

print('Applied Alpha14 Compose/Kotlin compile compatibility fix')
