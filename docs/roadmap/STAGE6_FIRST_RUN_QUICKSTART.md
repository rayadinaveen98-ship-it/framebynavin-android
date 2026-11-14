# Stage 6 — Personal-first, Creator-first, or Both onboarding

Status: **implemented in source; combined QA/release candidate pending**.

## New first-run journey
- Normal launch keeps Backlot's existing cinematic ident.
- A genuinely fresh install chooses what it wants to do first: Creator, Personal, or Both.
- Account connection is deferred: the user explicitly continues locally through focus selection, without a Google sign-in wall.
- Both selectable companions (Spark and Mira) follow the initial choice using the existing persisted guide preference.
- Personal-first users open the **existing Personal hub** immediately; no fabricated creator goal/profile is saved and no new Personal scheduler is introduced.
- Creator/Both users select only three required profile fields: primary creator mode, platform, and primary goal. Those exact explicit choices are written using the existing atomic `completeCreatorSetup` method before the first project composer opens.
- Creator setup is only requested of Personal-first users if they later enter a Creator-only surface; Personal reminders and challenges work without it.
- No notification/exact-alarm/full-screen permissions on the first-run choice page; prompt at first relevant reminder action.
- The guided tour remains replayable; new quick-start users begin with their first useful action. Existing users with a partially completed legacy setup keep the original V18 five-page flow.

## Compatibility safeguards
- Existing complete accounts/creator profiles bypass the new focus selector and retain stored work and guide identity.
- Legacy partial setups do not receive an automatic profile rewrite.
- CreatorModeRegistry and CreatorPlatformRegistry are the source of truth; no assumptions about genre or platform.
- No back/restore/delete, widget, reminder event, or media storage changes.

## Final integration QA
- Fresh Creator, Personal and Both paths on phone; app restart at every screen; process-death after focus selection.
- Personal-only: no forced Creator fields, immediate Personal hub, challenge create and Snooze.
- Personal-only -> tap Projects -> Creator setup -> existing projects still intact.
- Creator/Both: explicit fields durable, first project composer opens only after successful save.
- Returning user: no new onboarding. Partial legacy profile: old five-page setup retained.
- Companion selected survives first-run, settings changes, app restart, expanded notifications.
- Account connect available later in Profile; permissions requested at point of use.
- Real-device layout, 1.5x text, TalkBack and screen inset review.
