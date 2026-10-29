# Backlot — Privacy Policy (OWNER REVIEW DRAFT — DO NOT PUBLISH)

> **Not a published privacy policy or compliance certification.** Required fields and unverified claims are explicitly marked `[VERIFY]` or `[OWNER INPUT REQUIRED]`. This draft is based on the frozen Android Build267 source and read-only inspection of the connected Supabase project as of 30 October 2026. It must be reviewed, completed and checked against live app behavior, Google Play Data Safety submissions, processor terms, and the chosen production distribution model before publication.

**App:** Backlot (Android application ID `com.framebynavin.app` in the preview)  
**Developer / legal controller:** [OWNER INPUT REQUIRED — legal name or business identity]  
**Effective date:** [OWNER INPUT REQUIRED — actual publication date]  
**Privacy contact:** [OWNER INPUT REQUIRED — monitored public support email or URL]  
**Public deletion-request page:** [OWNER INPUT REQUIRED — live URL with verified web flow]  
**Public policy URL:** [OWNER INPUT REQUIRED — live HTTPS URL]

## Overview

Backlot helps creators organize film and content projects, ideas, reminders, and optional integrations. Much of the work is stored locally on the user's Android device. Some features allow users to connect accounts or send data to external providers. Which information is handled depends on which functions users choose to use.

**[VERIFY]** Confirm whether any SDK automatically collects device/app technical metadata at startup, whether analytics/crash reporting is enabled, and whether additional processors beyond those listed below receive data.

## Information used by Backlot

**Creator projects and ideas.** The app can store project titles, notes, schedules, descriptions, idea content, progress and media references locally. When optional Creator cloud backup is enabled, user-associated Creator snapshots may be stored in the configured Supabase project. Voice Idea recordings may be uploaded to the private Supabase Storage bucket `creator-voice-ideas` to support cloud recovery.

**Personal Reminders.** Personal reminder titles, notes, recurrence, scheduled times and completion state are stored separately from Creator projects. An optional exported Personal backup is a portable JSON file that **is not encrypted by the app**. Protect exported files and choose secure storage destinations. **[VERIFY]** Any platform-managed diagnostic collection or remote sync not evident from current source must be disclosed if present.

**Google account and sign-in.** If the user connects a Creator account, Backlot uses Google sign-in with Supabase Auth and may handle account identity, email, profile information, identifiers and authentication tokens. The app may cache session information on the device. **[VERIFY]** Token storage/encryption, provider retention and operational access controls.

**YouTube integrations.** If authorized, Backlot reads selected YouTube account/channel and analytics data using Google OAuth scopes including read-only YouTube information and read-only analytics. Separate monetization analytics access may be requested. **[VERIFY]** Exactly which results are stored locally or synced, storage duration and third-party SDK access.

**Calendar export.** With the user's permission and selected calendar, Backlot can read calendar metadata and add/update selected Creator deadlines through the Android Calendar provider. Entries added to a Google-backed calendar may also be synchronized by Google according to the user's Google account settings. Calendar permission is requested contextually.

**Microphone and voice.** Users may record Voice Ideas and initiate optional Spark push-to-talk recognition. Spark's current implementation prefers on-device speech recognition and requires an explicit microphone action. Voice Idea recordings are user content and may later be included in user-requested backups. **[VERIFY]** Any Android/platform speech service behavior and SDK data handling on supported devices.

**Optional AI analysis.** If a user invokes Video Autopsy, evidence and a video reference or associated text may be sent to Firebase AI / Google AI services to generate analysis. **[VERIFY]** User confirmation, data categories, retention, model-improvement policies and processor terms before release.

**Device and app operation.** Notifications, reminders, alarms, foreground audio, calendar scheduling, restart recovery, widgets and app settings use Android APIs. **[VERIFY]** Whether Firebase App Check, Google Play services or other SDKs collect device integrity, diagnostic identifiers, crash/usage data or network metadata.

## How information is used

Depending on enabled features, information is used to store and display Creator projects, record ideas, deliver reminders, restore backups, connect accounts, display authorized YouTube analytics, write chosen calendar entries, and generate optional AI results. **[VERIFY]** Provider-specific analytics, service protection, diagnostics and operational-security purposes.

## External services and sharing

Possible service providers include Google Sign-In and YouTube APIs, Google Calendar via Android, Supabase Auth/Database/Storage, Google Drive where users explicitly select manual backup, Firebase App Check and optional Firebase AI. Information is sent to the relevant service for the requested purpose; these providers may process data under their own terms and privacy notices.

We do not assert that every listed integration is enabled for every user. **[VERIFY]** Whether advertising SDKs, cross-app tracking, crash analytics, other third parties, or app-store billing components operate in the actual signed release.

## Storage, security and retention

The app disables Android automatic app-data backup through its manifest. It offers separate Creator and Personal manual export and restore options, and optional Creator account cloud backup. Supabase public Creator tables currently have row-level security enabled, while the voice-recording bucket is private; these controls alone are not a comprehensive security certification.

**[OWNER INPUT REQUIRED]** Specify actual cloud retention rules, operational backups, logs, data encryption in transit and at rest, security controls, data retention exceptions, processor retention, incident-contact policy and any retention deadlines. Do not promise a deletion SLA until it is operationally verified.

## Choices and permissions

Users can manage account connections and relevant Android runtime permissions in the app and Android settings. Refusing certain permissions may limit functions such as microphone recording, calendar export or precise reminders. **[VERIFY]** Permission denial/revocation fallback, in-app privacy-link location and sign-out behavior.

## Requesting access or deletion

**[NOT YET IMPLEMENTED/VERIFIED]** Full Backlot account deletion is separate from “delete Creator cloud sync data” and from “sign out.” The current sync-data function clears Creator snapshots and heads only; it does not remove the entire authenticated Creator account or all voice recordings.

Before the app can be published to a Google Play track where account deletion applies, Backlot must provide both:
- A discoverable in-app request route; and
- A working, public account-deletion request page that users can access without reinstalling the app.

The deletion process must securely verify account ownership, handle the user-associated Creator profile, snapshots, Auth account and private voice-media objects, and communicate actual completion and permitted retention exceptions. Deleting a Backlot app account does **not** delete the user's underlying Google account. Personal files exported outside the app remain in their chosen destination; clearly disclose what happens to local content.

**[OWNER INPUT REQUIRED]** Add a monitored contact, actual app/web deletion URLs, request handling SLA, retained-data exceptions and support procedure before publication.

## Children's privacy

**[OWNER INPUT REQUIRED]** Specify intended audience and age eligibility based on actual app distribution, onboarding and data processing. Do not claim the app is not directed at children without completing Play target-audience review.

## International processing

**[VERIFY]** Determine actual provider regions, international transfers, applicable legal basis and user rights for the developer's locations and Play distribution markets. Supabase project region alone does not establish every service's processing region.

## Changes to this policy

**[OWNER INPUT REQUIRED]** Define how material changes are communicated and where the live effective date is displayed.

## Contact

**[OWNER INPUT REQUIRED — actual support/privacy contact]**

---

## Publication checklist (not part of public copy)

- [ ] Developer legal identity, address where needed and active support email verified
- [ ] Live privacy URL externally reachable and linked in-app + Play Console
- [ ] Live deletion request URL externally reachable without the app, verified in-app route and functioning secure workflow
- [ ] SDK and actual data collection/sharing reviewed, including Google/YouTube, Supabase, Firebase AI/App Check, Android speech/provider, analytics, logs
- [ ] Retention, security, encryption and legal requirements verified with owner and processors
- [ ] Correct age audience, geographic rights, contact and effective date
- [ ] Owner/legal approval; privacy policy text matches Play Data Safety declarations
- [ ] No `[VERIFY]` or `[OWNER INPUT REQUIRED]` tokens left in the published document

**This file must not be deployed directly as a final privacy policy.**
