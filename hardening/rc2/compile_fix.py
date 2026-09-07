#!/usr/bin/env python3
"""RC2 compatibility fix: preserve Kotlin trailing-lambda updateTask calls."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'app/src/main/java/com/framebynavin/app/data'

def replace_once(path, old, new):
    text = path.read_text(encoding='utf-8')
    if text.count(old) != 1:
        raise RuntimeError(f'Unexpected source for {path}: expected one match, found {text.count(old)}')
    path.write_text(text.replace(old, new), encoding='utf-8')

replace_once(BASE / 'TaskStore.kt',
    'suspend fun updateTask(id: String, transform: (CreatorTask) -> CreatorTask, expectedGeneration: Long? = null): CreatorTask?',
    'suspend fun updateTask(id: String, expectedGeneration: Long? = null, transform: (CreatorTask) -> CreatorTask): CreatorTask?')
replace_once(BASE / 'CreatorViewModel.kt',
    '''    private fun updateTask(
        id: String,
        transform: (CreatorTask) -> CreatorTask,
        after: suspend (CreatorTask) -> Unit = {},
    )''',
    '''    private fun updateTask(
        id: String,
        after: suspend (CreatorTask) -> Unit = {},
        transform: (CreatorTask) -> CreatorTask,
    )''')
replace_once(BASE / 'CreatorViewModel.kt',
    'store.updateTask(id, { current ->',
    'store.updateTask(id, expectedGeneration = epoch, transform = { current ->')
replace_once(BASE / 'CreatorViewModel.kt',
    '            }, expectedGeneration = epoch)',
    '            })')
print('RC2 Kotlin API compatibility fix applied')
