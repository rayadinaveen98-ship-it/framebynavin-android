# Backlot V227 — Device optimization foundation: Idea Save footer

## Evidence and scope
- Earlier V220.3/V220.4 changes replaced New Idea's dialog with an Activity-owned overlay and added real Android keyboard screenshot verification on both standard/compact window sizes. Do not revert those repairs.
- The **existing Idea Edit dialog** retained two fixed-width Save/Cancel buttons. Both were only 11sp or smaller. Large accessibility fonts and narrow viewport widths risk squeezed labels or partial button content even when the row remains technically inside the Compose root.
- Device differences must be validated in actual Android windows, not just Compose click semantics. V227 adds bounded layouts and more emulator profiles while the user's V226 phone backup acceptance remains open.

## Implemented
1. New `V227IdeaEditorActionFooter`: width/font-scale adaptive action row. Compact screen (<380dp) or font scale >=1.3 shows full-width SAVE then CANCEL vertically. Normal devices preserve the existing horizontal row. Both have at least 52dp touch height, accessible 13sp two-line text and unchanged `v216-edit-save` / `v216-edit-cancel` test IDs.
2. Existing Idea Editor Save callback is unchanged: draft, identity, notes, recording and reminders follow their established storage path. Only footer layout composition has changed.
3. New Idea's Activity-owned SAVE IDEA action now uses minimum 52dp rather than a hard fixed height so increased Android font size can grow without cropping text; 12sp label, maximum two lines. Keep the Activity-owned keyboard-safe layout.
4. Add pure unit policy checks and real Compose UI geometry checks for narrow 320dp, wide 440dp, and 1.7 font scale; both actions remain inside parent and real click callbacks fire.
5. Add Android 11 API30 pixel_6 emulator with normal, 900x1600 / 1.5 font profile and real IME New Idea regression, and Android 15 API35 emulator with gestural system bar + real IME screenshot regression. Preserve previous 28 Creator/Personal backup/recovery/widget regression tests.
6. Full test-only sideload application versionCode 249, same package and prototype signature as V226 versionCode 248; not production Google Play signing.

## Release gates still open
- Real-device phone OEM keyboard (Gboard, Samsung etc), gesture nav, 1x/1.5x/2x font, display zoom, TalkBack, landscape/large screens/tablets, reminder Doze and V226 backup provider acceptance.
- **Legacy widgets are not deleted or disabled** in this phase. Preserve pinned launcher state until user-approved migration.
- Do not claim full V227 or Play Store readiness on API30/API35 emulator green alone.
