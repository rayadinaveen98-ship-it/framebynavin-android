package com.framebynavin.app.ui

import com.framebynavin.app.youtube.YouTubeVideoSnapshot
import org.junit.Assert.*
import org.junit.Test

class V189CatalogPolicyTest {
    private fun video(id: String, title: String, published: Long, views: Long, reported: Boolean) =
        YouTubeVideoSnapshot(id, title, published, views, views, 10L, 30L, 0L, 0L, 0L, 0L,
            hasPeriodData = reported)

    @Test fun searchOnlyMatchesLoadedTitlesWithStableDeduplication() {
        val a = video("A", "Cinema breakdown", 10L, 500L, true)
        val b = video("B", "Behind the camera", 20L, 300L, false)
        val r = V189CatalogPolicy.visible(listOf(a, b, a), "cinema",
            V189CatalogFilter.ALL, V189CatalogSort.RECENT, emptyMap())
        assertEquals(listOf("A"), r.map { it.videoId })
    }

    @Test fun reportedFilterDoesNotTreatMissingRowsAsZeroViews() {
        val zero = video("zero", "Reported zero", 30L, 0L, true)
        val missing = video("missing", "Not processed", 40L, 0L, false)
        val all = V189CatalogPolicy.visible(listOf(zero, missing), "",
            V189CatalogFilter.REPORTED, V189CatalogSort.VIEWS, emptyMap())
        assertEquals(listOf("zero"), all.map { it.videoId })
    }

    @Test fun linkedAndUnlinkedFiltersNeverModifySource() {
        val a = video("A", "Alpha", 1L, 40L, true)
        val b = video("B", "Beta", 2L, 100L, true)
        val input = listOf(a, b)
        val links = mapOf("B" to "local-project")
        assertEquals(listOf("B"), V189CatalogPolicy.visible(input, "",
            V189CatalogFilter.LINKED, V189CatalogSort.RECENT, links).map { it.videoId })
        assertEquals(listOf("A"), V189CatalogPolicy.visible(input, "",
            V189CatalogFilter.UNLINKED, V189CatalogSort.RECENT, links).map { it.videoId })
        assertEquals(input, listOf(a, b))
    }

    @Test fun viewsSortPlacesUnavailableLastWithoutPretendingZero() {
        val unknown = video("u", "Unreported", 40L, 0L, false)
        val zero = video("z", "Reported zero", 30L, 0L, true)
        val views = video("v", "With views", 20L, 100L, true)
        val sorted = V189CatalogPolicy.visible(listOf(unknown, zero, views), "",
            V189CatalogFilter.ALL, V189CatalogSort.VIEWS, emptyMap())
        assertEquals(listOf("v", "z", "u"), sorted.map { it.videoId })
    }
}
