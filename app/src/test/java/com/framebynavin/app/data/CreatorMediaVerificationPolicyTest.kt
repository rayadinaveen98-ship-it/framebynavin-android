package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorMediaVerificationPolicyTest {
    @Test
    fun `official evidence verifies immediately`() {
        assertEquals(
            CreatorMediaVerification.VERIFIED,
            CreatorMediaVerificationPolicy.classify(
                listOf(evidence("official", CreatorMediaEvidenceTier.OFFICIAL, "https://www.netflix.com/title/1"))
            ),
        )
    }

    @Test
    fun `two independent strong publishers corroborate to verified`() {
        val evidence = listOf(
            evidence("trade", CreatorMediaEvidenceTier.TRADE, "https://variety.com/story"),
            evidence("news", CreatorMediaEvidenceTier.REPUTABLE_NEWS, "https://indianexpress.com/story"),
        )

        assertEquals(CreatorMediaVerification.VERIFIED, CreatorMediaVerificationPolicy.classify(evidence))
    }

    @Test
    fun `multiple links from same publisher count once`() {
        val evidence = listOf(
            evidence("trade-1", CreatorMediaEvidenceTier.TRADE, "https://www.variety.com/story-one"),
            evidence("trade-2", CreatorMediaEvidenceTier.TRADE, "https://variety.com/story-two"),
        )

        assertEquals(CreatorMediaVerification.DEVELOPING, CreatorMediaVerificationPolicy.classify(evidence))
    }

    @Test
    fun `single strong source remains developing`() {
        assertEquals(
            CreatorMediaVerification.DEVELOPING,
            CreatorMediaVerificationPolicy.classify(
                listOf(evidence("trade", CreatorMediaEvidenceTier.TRADE, "https://deadline.com/story"))
            ),
        )
    }

    @Test
    fun `secondary only evidence remains rumor`() {
        assertEquals(
            CreatorMediaVerification.RUMOR,
            CreatorMediaVerificationPolicy.classify(
                listOf(evidence("secondary", CreatorMediaEvidenceTier.SECONDARY, "https://example.com/post"))
            ),
        )
    }

    @Test
    fun `normalization replaces an overconfident incoming label`() {
        val signal = CreatorMediaSignal(
            id = "signal",
            title = "Unconfirmed casting report",
            summary = "",
            kind = CreatorMediaSignalKind.PRODUCTION_UPDATE,
            publishedAtMillis = 1L,
            verification = CreatorMediaVerification.VERIFIED,
            evidence = listOf(
                evidence("secondary", CreatorMediaEvidenceTier.SECONDARY, "https://example.com/post")
            ),
        )

        assertEquals(
            CreatorMediaVerification.RUMOR,
            CreatorMediaVerificationPolicy.normalize(signal).verification,
        )
    }

    private fun evidence(id: String, tier: CreatorMediaEvidenceTier, url: String): CreatorMediaEvidence =
        CreatorMediaEvidence(
            id = id,
            label = id,
            url = url,
            tier = tier,
        )
}
