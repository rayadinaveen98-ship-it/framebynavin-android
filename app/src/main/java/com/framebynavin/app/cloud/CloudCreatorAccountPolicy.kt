package com.framebynavin.app.cloud

enum class CloudCreatorAccountRoute {
    SIGN_IN_REQUIRED,
    RETURNING_CREATOR,
    CREATOR_ID_REQUIRED,
}

/**
 * Account identity and creator onboarding are separate from cloud-backup health.
 * A returning creator is recognized only by the authenticated Supabase user plus
 * that same user's persisted creator profile/username. Backup availability never
 * decides whether an existing account must be recreated.
 */
object CloudCreatorAccountPolicy {
    fun route(
        session: CloudSession?,
        profile: CloudCreatorProfile?,
    ): CloudCreatorAccountRoute {
        if (session == null) return CloudCreatorAccountRoute.SIGN_IN_REQUIRED
        if (
            profile != null &&
            profile.userId == session.userId &&
            profile.username.isNotBlank()
        ) {
            return CloudCreatorAccountRoute.RETURNING_CREATOR
        }
        return CloudCreatorAccountRoute.CREATOR_ID_REQUIRED
    }
}
