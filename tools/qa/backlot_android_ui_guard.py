#!/usr/bin/env python3
"""Guard screenshot QA against Android ANR/system dialogs using accessibility UI XML.

No OCR or screenshots are read. Executed only on disposable Android emulators.
"""
from __future__ import annotations
import argparse
import re
import subprocess
import sys
import time
from pathlib import Path
from xml.etree import ElementTree as ET

DUMP_PATH = '/sdcard/backlot-phase1-review-ui.xml'
BOUNDS = re.compile(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]')

def cmd(*args: str, timeout: int = 50) -> subprocess.CompletedProcess[str]:
    return subprocess.run(['adb', *args], capture_output=True, text=True,
                          timeout=timeout, check=True)

def read_ui() -> str:
    cmd('shell', 'uiautomator', 'dump', DUMP_PATH, timeout=70)
    return cmd('exec-out', 'cat', DUMP_PATH, timeout=15).stdout

def identify_overlay(xml: str):
    root = ET.fromstring(xml)
    nodes = list(root.iter('node'))
    def label(node):
        return (node.attrib.get('text', '') + ' ' +
                node.attrib.get('content-desc', '')).strip().lower()
    labels = [label(n) for n in nodes]
    anr = any(('not responding' in t or "isn't responding" in t or
               'isn’t responding' in t) for t in labels)
    system_modal = any('close app' in t for t in labels) and any(
        ('wait' in t or 'responding' in t) for t in labels)
    if not (anr or system_modal):
        return False, None
    close = next((n for n in nodes if label(n) == 'close app'), None)
    if close is None:
        close = next((n for n in nodes if 'close app' in label(n)), None)
    if close is None:
        return True, None
    match = BOUNDS.fullmatch(close.attrib.get('bounds', ''))
    if not match:
        return True, None
    x0, y0, x1, y1 = map(int, match.groups())
    if not (x1 > x0 and y1 > y0):
        return True, None
    return True, ((x0 + x1) // 2, (y0 + y1) // 2)

def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument('scene')
    p.add_argument('output_dir', type=Path)
    p.add_argument('--strict', action='store_true', help='reject overlay; never dismiss')
    args = p.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for attempt in range(4 if not args.strict else 1):
        try:
            xml = read_ui()
            overlay, close = identify_overlay(xml)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, ET.ParseError) as e:
            print(f'Cannot prove scene unobstructed: {args.scene}: {e}', file=sys.stderr)
            return 3
        if not overlay:
            print(f'Verified unobstructed Android UI: {args.scene}')
            return 0
        (args.output_dir / f'{args.scene}.system-dialog.xml').write_text(xml)
        if args.strict or close is None or attempt == 3:
            print(f'System ANR overlay blocking screenshot: {args.scene}', file=sys.stderr)
            return 4
        print(f'Dismissing emulator-only system ANR at {close} for {args.scene}')
        try:
            cmd('shell', 'input', 'tap', str(close[0]), str(close[1]), timeout=12)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired) as e:
            print(f'Could not dismiss emulator system ANR: {e}', file=sys.stderr)
            return 5
        time.sleep(3)
    return 6

if __name__ == '__main__':
    raise SystemExit(main())
