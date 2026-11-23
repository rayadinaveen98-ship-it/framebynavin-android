# Backlot P2 — Voice Identity Lab Source-Stage Ledger

Status: POINT 2 SOURCE-STAGED / UNCOMPILED / UNTESTED / NOT USER-ACCEPTED.
Approval: founder 2026-11-22 said "Approved bro continue" after P1 stage; proceed to Point 2 source staging only, as part of prior "stage everything first, tests later" decision.

## Source of truth
- Repository rayadinaveen98-ship-it/framebynavin-android.
- Branch feature/backlot-v337-point02-voice-identity-source-after-p1-20261122 (includes staged P1).
- Parent P1 commit 3185011fe5874cee992c4346deedfb42694a3076.
- Frozen V337 P0 reliability base 815ba69775f75b6e5f25bb838c2c17b4f09c203e.
- Rejected V338 recovery source not used as implementation base. No merge to main or stable.

## Verified existing behavior
VoicePersona has eight persisted enum style IDs WARM, YOUNG, MAN, WOMAN, FUNNY, CARTOON, ALIEN, ROBOT. Original VoicePersonaEngine applies different pitch/rate and selects from a limited set of installed local Android TTS IDs; distinct humans are not guaranteed.
Creator VoiceReminderService and settings preview use this engine. PersonalPlaybackService and Phase1HabitVoiceService follow independent speaker playback; P2 leaves both untouched.
The original phrases and Creator task enum storage, repeat/replay/occurrence tokens/music, sound permissions and cloud backup remain unchanged.

## Staged implementation
1. P2OfflineVoiceStore: local-only opt-in mapping from existing Creator style to exact installed OFFLINE voice engine, name and locale; no network voices, cloud API, microphone, recording or paid integration.
2. P2VoiceIdentityLab: optional expandable English/Telugu offline audition view inside existing Settings -> Voice; same-language fixed sentence at neutral pitch/rate for comparable listening. Shows actual installed IDs, does not claim that IDs always sound distinct.
3. USE SPEAKER is explicit, stores an English offline ID for the selected Creator style (current reminder text is English). If voice package/ID/engine is missing or installation changes, fall back to original VoicePersonaEngine styling; reset available.
4. VoiceReminderService and Settings Preview call the new applyWithChosenOfflineSpeaker opt-in adapter; no other voice service or delivery rules modified.
5. Disposal stops/shuts down audition TTS and refuses late TTS-ready callbacks; no personal reminder text is saved or uploaded.
6. Pure policy test source staged but NOT EXECUTED.

## Outstanding quality decisions
- A real eight-voice speaker library DOES NOT EXIST merely by staging this feature. Phone may expose zero, one or many offline English/Telugu voices. IDs may still be audibly similar.
- A future owner-reviewed listening blind test must establish actual voice quality. Premium provider selection (neural cloud/offline model, professionally licensed recordings) is NOT APPROVED. No provider subscription, keys, cost, billing or cloud speech will be introduced without the founder approving price, consent and privacy model.
- Telugu Creator reminder delivery remains English-text-only and Telugu audible previews are listen-only until a language/text quality decision.
- Personal and 21-Day speech quality is not modified by this first Creator eight-persona stage.

## Deferred QA gates
QA0 ensure cumulative P1/P2 changes preserve stable SHA and unrelated production data, cloud, credential, manifest, scheduled actions.
QA1 compile-only Kotlin/Android sources, pinpoint failures without whole JVM reruns.
QA2 run P2VoiceIdentityPolicyTest and selected existing VoicePersonaEngine and reminder speech policy tests only.
QA3 AndroidTest compile, disposable test speaker catalog with 0/1/many devices, engine switching and safe missing offline voice fallback.
QA4 Android 15 emulator actual Settings->Voice previews, offline voice listing, opt-in, reset, lifecycle stop, accessibility.
QA5 Physical user listening trial against the SAME English/Telugu sample sentence with objective distinctness ratings and real reminder routing, no unwanted network TTS.
QA6 Signed owner-preview APK only after founder later approves integration QA and build gate.

Do not run any test or APK CI on this staging branch until separately authorized.
