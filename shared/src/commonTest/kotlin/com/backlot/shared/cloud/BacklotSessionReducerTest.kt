package com.backlot.shared.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BacklotSessionReducerTest {
    private val accountA = BacklotIdentityRef(
        provider = BacklotIdentityProvider.GOOGLE,
        providerSubject = "google-account-a",
        email = "a@example.com",
        displayName = "Creator A",
    )

    private val accountB = BacklotIdentityRef(
        provider = BacklotIdentityProvider.APPLE,
        providerSubject = "apple-account-b",
        cloudAccountId = "cloud-account-b",
        email = "b@example.com",
        displayName = "Creator B",
    )

    @Test
    fun signedOutSessionContainsNoIdentity() {
        val session = BacklotSessionReducer.signedOut()

        assertEquals(BacklotSessionStatus.SIGNED_OUT, session.status)
        assertNull(session.identity)
        assertFalse(session.isSignedIn)
    }

    @Test
    fun resolvingSessionCannotExposePreviousAccountIdentity() {
        val previous = BacklotSessionReducer.authenticated(accountA)
        assertEquals(accountA, previous.identity)

        val resolving = BacklotSessionReducer.beginResolution()

        assertEquals(BacklotSessionStatus.RESOLVING, resolving.status)
        assertNull(resolving.identity)
        assertFalse(resolving.isSignedIn)
    }

    @Test
    fun authenticatedSessionContainsOnlyCompletedIdentity() {
        val session = BacklotSessionReducer.authenticated(accountB)

        assertEquals(BacklotSessionStatus.SIGNED_IN, session.status)
        assertEquals(accountB, session.identity)
        assertEquals("cloud-account-b", session.identity?.cloudAccountId)
        assertTrue(session.isSignedIn)
    }

    @Test
    fun accountSwitchCannotCarryPriorIdentityForward() {
        val accountASession = BacklotSessionReducer.authenticated(accountA)
        assertEquals(accountA, accountASession.identity)

        val resolving = BacklotSessionReducer.beginResolution()
        assertNull(resolving.identity)

        val accountBSession = BacklotSessionReducer.authenticated(accountB)
        assertEquals(accountB, accountBSession.identity)
        assertFalse(accountBSession.identity == accountA)
    }

    @Test
    fun signOutClearsAuthenticatedIdentity() {
        val signedIn = BacklotSessionReducer.authenticated(accountA)
        assertTrue(signedIn.isSignedIn)

        val signedOut = BacklotSessionReducer.signOut()

        assertEquals(BacklotSessionStatus.SIGNED_OUT, signedOut.status)
        assertNull(signedOut.identity)
    }

    @Test
    fun blankProviderSubjectCannotBecomeAuthenticatedSession() {
        val invalidIdentity = accountA.copy(providerSubject = "  ")

        assertFailsWith<IllegalArgumentException> {
            BacklotSessionReducer.authenticated(invalidIdentity)
        }
    }

    @Test
    fun whitespaceCloudAccountIdCannotBecomeAuthenticatedSession() {
        val invalidIdentity = accountB.copy(cloudAccountId = "   ")

        assertFailsWith<IllegalArgumentException> {
            BacklotSessionReducer.authenticated(invalidIdentity)
        }
    }

    @Test
    fun legacyIdentityMayRemainLocalOnlyDuringMigration() {
        val session = BacklotSessionReducer.authenticated(accountA)

        assertEquals("", session.identity?.cloudAccountId)
        assertTrue(session.isSignedIn)
    }

    @Test
    fun impossibleSnapshotIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            BacklotSessionSnapshot(
                status = BacklotSessionStatus.RESOLVING,
                identity = accountA,
            )
        }
    }
}
