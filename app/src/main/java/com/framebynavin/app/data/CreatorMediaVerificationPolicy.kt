package com.framebynavin.app.data

import java.net.URI

/**
 * Converts collected evidence into Backlot's public verification label.
 *
 * Verification is intentionally conservative:
 *  - any official first-party source is verified;
 *  - two independent trade/reputable-news publishers can corroborate a claim to verified;
 *  - one strong non-official publisher remains developing;
 *  - secondary-only evidence remains rumor.
 *
 * Multiple links from the same publisher count once so syndication/reposts cannot manufacture
 * corroboration.
 */
object CreatorMediaVerificationPolicy {
    fun classify(evidence: List<CreatorMediaEvidence>): CreatorMediaVerification {
        val independent = evidence
            .filter { it.id.isNotBlank() || it.label.isNotBlank() || it.url.isNotBlank() }
            .distinctBy(::publisherKey)

        if (independent.any { it.tier == CreatorMediaEvidenceTier.OFFICIAL }) {
            return CreatorMediaVerification.VERIFIED
        }

        val strongPublishers = independent.count {
            it.tier == CreatorMediaEvidenceTier.TRADE ||
                it.tier == CreatorMediaEvidenceTier.REPUTABLE_NEWS
        }
        return when {
            strongPublishers >= 2 -> CreatorMediaVerification.VERIFIED
            strongPublishers == 1 -> CreatorMediaVerification.DEVELOPING
            else -> CreatorMediaVerification.RUMOR
        }
    }

    fun normalize(signal: CreatorMediaSignal): CreatorMediaSignal =
        signal.copy(verification = classify(signal.evidence))

    internal fun publisherKey(evidence: CreatorMediaEvidence): String {
        val host = evidence.url.trim().takeIf(String::isNotBlank)?.let { raw ->
            runCatching { URI(raw).host.orEmpty() }.getOrDefault("")
                .lowercase()
                .removePrefix("www.")
        }.orEmpty()
        if (host.isNotBlank()) return host

        val label = evidence.label.trim().lowercase()
        if (label.isNotBlank()) return label
        return evidence.id.trim().lowercase()
    }
}
