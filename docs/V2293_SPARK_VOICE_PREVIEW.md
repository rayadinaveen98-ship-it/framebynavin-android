# Backlot V229.3 — Optional Spark voice (Build 260 preview)

## User flow
- Existing Spark Home avatar > Ask Spark Offline, then tap **MIC** to request microphone permission just-in-time. No passive listening, background recording or automatic prompt sending.
- Uses Android API 31+ **on-device-only SpeechRecognizer.createOnDeviceSpeechRecognizer** when the device reports it available. Android 11 or devices lacking the on-device recognizer keep the mic disabled and retain fully functional typing. Never falls back to an unspecified cloud recognizer.
- Transcription enters the editable text box; the user must review and press SEND. No automatic editing, reminders, publishing, project or navigation actions.
- A Spark reply exposes **READ ALOUD**, manually triggered. TTS chooses only an installed voice with **isNetworkConnectionRequired == false**, matching the user's language. If unavailable, show the text and explain the limitation. Speaking can be stopped; no autoplay.
- Compose-scoped controller destroys recognizer and TTS on closing the sheet or Activity ON_STOP. No transcript, audio recording, speech logs or voice prefs are stored in project JSON, SharedPreferences, backups or analytics.
- Reuses existing Android RECORD_AUDIO manifest permission (requested per tap); no new manifest permission or background service. On-device speech recognizer and voices are supplied by Android/OEM, which may not support every phone or language.

## QA gates
- API 30 and API 35 emulator UI tests check that typing remains available, mic does not silently activate and read-aloud is opt-in. JVM tests check API gating, input truncation and no cloud fallback policy.
- Full JVM/lint/build regression and Build 260 full owner APK with package and certificate upgrade test from verified Build 259.
- Real-phone microphone permission, speech model availability, locale/Telugu, voice playback, keyboard collisions, app background, DND, accessibility, rotation and service failures must be tested before stable release.
- PR draft, stable unchanged.
