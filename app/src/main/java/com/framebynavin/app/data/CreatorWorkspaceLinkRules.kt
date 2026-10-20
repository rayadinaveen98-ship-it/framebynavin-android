package com.framebynavin.app.data

import java.net.URI

/**
 * Only URLs with an explicit HTTP(S) host are trusted for external research browsing.
 * Attachments may additionally use SAF content:// URIs. Legacy raw paths and file:// URIs
 * must never be exposed through an implicit cross-app VIEW intent.
 */
object CreatorWorkspaceLinkRules {
    private fun parsed(value: String): URI? {
        val raw = value.trim()
        if (raw.isEmpty() || raw.any { it.isISOControl() }) return null
        return runCatching { URI(raw) }.getOrNull()
    }

    fun isWebUrl(value: String): Boolean {
        val uri = parsed(value) ?: return false
        return (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) &&
            !uri.host.isNullOrBlank() && uri.rawUserInfo == null
    }

    fun isAssetLocation(value: String): Boolean {
        if (isWebUrl(value)) return true
        val uri = parsed(value) ?: return false
        return uri.scheme.equals("content", true) &&
            !uri.rawAuthority.isNullOrBlank() &&
            uri.rawUserInfo == null
    }
}
