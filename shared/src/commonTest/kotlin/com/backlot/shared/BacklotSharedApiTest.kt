package com.backlot.shared

import com.backlot.shared.cloud.BacklotIdentityProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BacklotSharedApiTest {
    @Test
    fun techReviewIsResolvedEntirelyInCommonCode() {
        val template = BacklotSharedApi.resolveWorkflow(
            creatorModeId = "tech",
            archetypeId = "review",
            archetypeLabel = "Review",
            platform = "YouTube",
            deliveryFormat = "Long-form",
            legacyContentType = "Long-form",
        )

        assertNotNull(template)
        assertEquals("v150_tech_review", template.id)
        assertEquals("Test Plan", template.stages.first().label)
        assertTrue(template.stages.any { it.id == "published" })
    }

    @Test
    fun projectWithoutContentDnaDoesNotInventAWorkflow() {
        val template = BacklotSharedApi.resolveWorkflow(
            creatorModeId = "",
            archetypeId = "",
            archetypeLabel = "",
            platform = "YouTube",
            deliveryFormat = "",
            legacyContentType = "Long-form",
        )
        assertNull(template)
    }

    @Test
    fun legacySignedInSessionRemainsProviderOnly() {
        val session = BacklotSharedApi.signedInSession(
            provider = BacklotIdentityProvider.GOOGLE,
            providerSubject = "google-user",
            email = "creator@example.com",
            displayName = "Creator",
            avatarUrl = "",
        )

        assertTrue(session.isSignedIn)
        assertEquals("google-user", session.identity?.providerSubject)
        assertEquals("", session.identity?.cloudAccountId)
    }

    @Test
    fun cloudSignedInSessionCarriesCanonicalAccountId() {
        val session = BacklotSharedApi.signedInCloudSession(
            provider = BacklotIdentityProvider.APPLE,
            providerSubject = "apple-user",
            cloudAccountId = "canonical-cloud-user",
            email = "creator@example.com",
            displayName = "Creator",
            avatarUrl = "",
        )

        assertTrue(session.isSignedIn)
        assertEquals("apple-user", session.identity?.providerSubject)
        assertEquals("canonical-cloud-user", session.identity?.cloudAccountId)
    }
}
