package com.framebynavin.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorWorkspaceLinkRulesTest {
    @Test fun sourceLinksAllowExplicitHttpAndHttpsOnly() {
        assertTrue(CreatorWorkspaceLinkRules.isWebUrl("https://example.com/article?id=3"))
        assertTrue(CreatorWorkspaceLinkRules.isWebUrl("  HTTP://www.example.org/path  "))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl("javascript:alert(1)"))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl("file:///sdcard/Download/film.mp4"))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl("content://com.android.providers.media/doc/7"))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl("https:///missing-host"))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl("https://user:pass@example.com"))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl("www.example.com"))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl("https://example.com/space here"))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl("https://example.com\nspoof"))
        assertFalse(CreatorWorkspaceLinkRules.isWebUrl(""))
    }

    @Test fun assetsAllowSafContentAndWebButNeverRawLocalPaths() {
        assertTrue(CreatorWorkspaceLinkRules.isAssetLocation("content://com.android.providers.media.documents/document/image%3A123"))
        assertTrue(CreatorWorkspaceLinkRules.isAssetLocation("content://com.android.externalstorage.documents/document/primary%3ADownload%2Fmovie.pdf"))
        assertTrue(CreatorWorkspaceLinkRules.isAssetLocation("https://files.example.org/reference.mp4"))
        assertFalse(CreatorWorkspaceLinkRules.isAssetLocation("content:///no-authority"))
        assertFalse(CreatorWorkspaceLinkRules.isAssetLocation("content:relative-path"))
        assertFalse(CreatorWorkspaceLinkRules.isAssetLocation("/storage/emulated/0/Movies/clip.mp4"))
        assertFalse(CreatorWorkspaceLinkRules.isAssetLocation("C:\\Movies\\clip.mp4"))
        assertFalse(CreatorWorkspaceLinkRules.isAssetLocation("file:///storage/emulated/0/Movies/clip.mp4"))
        assertFalse(CreatorWorkspaceLinkRules.isAssetLocation("intent://example.com/#Intent;scheme=https;end"))
        assertFalse(CreatorWorkspaceLinkRules.isAssetLocation("data:text/plain,test"))
        assertFalse(CreatorWorkspaceLinkRules.isAssetLocation(""))
    }
}
