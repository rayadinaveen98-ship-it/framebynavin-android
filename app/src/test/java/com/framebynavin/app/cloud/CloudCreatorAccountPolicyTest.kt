package com.framebynavin.app.cloud

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudCreatorAccountPolicyTest {
    private fun session(userId: String = "user-a") = CloudSession(
        userId = userId,
        email = "creator@example.com",
        displayName = "Creator",
        avatarUrl = "",
        accessToken = "access",
        refreshToken = "refresh",
        expiresAtMillis = Long.MAX_VALUE,
    )

    private fun profile(userId: String = "user-a", username: String = "creator") = CloudCreatorProfile(
        userId = userId,
        displayName = "Creator",
        username = username,
        avatarUrl = "",
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
    )

    @Test
    fun signedOut_requiresGoogleSignIn() {
        assertEquals(
            CloudCreatorAccountRoute.SIGN_IN_REQUIRED,
            CloudCreatorAccountPolicy.route(null, null),
        )
    }

    @Test
    fun sameAuthenticatedUserWithUsername_isReturningCreator() {
        assertEquals(
            CloudCreatorAccountRoute.RETURNING_CREATOR,
            CloudCreatorAccountPolicy.route(session(), profile()),
        )
    }

    @Test
    fun authenticatedUserWithoutCreatorProfile_needsCreatorId() {
        assertEquals(
            CloudCreatorAccountRoute.CREATOR_ID_REQUIRED,
            CloudCreatorAccountPolicy.route(session(), null),
        )
    }

    @Test
    fun profileWithoutUsername_isNotTreatedAsExistingCreatorId() {
        assertEquals(
            CloudCreatorAccountRoute.CREATOR_ID_REQUIRED,
            CloudCreatorAccountPolicy.route(session(), profile(username = "")),
        )
    }

    @Test
    fun cachedProfileFromAnotherUser_cannotBypassOnboarding() {
        assertEquals(
            CloudCreatorAccountRoute.CREATOR_ID_REQUIRED,
            CloudCreatorAccountPolicy.route(session("user-a"), profile("user-b")),
        )
    }
}
