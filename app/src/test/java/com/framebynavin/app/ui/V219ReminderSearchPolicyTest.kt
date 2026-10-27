package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderCenterGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class V219ReminderSearchPolicyTest {
    private fun task(
        id: String, title: String,
        platform: String = "YouTube", contentType: String = "Video", notes: String = "",
    ) = CreatorTask(
        id = id, title = title, platform = platform,
        contentType = contentType, dueLabel = "Soon", notes = notes,
    )

    private val filming = task("shoot", "Shoot opening sequence", notes = "Studio lighting plan")
    private val editing = task("edit", "Edit cinema analysis", contentType = "Short", notes = "Cut B-roll")
    private val telugu = task("te", "సినిమా విశ్లేషణ", platform = "తెలుగు")
    private val grouped = linkedMapOf(
        ReminderCenterGroup.NEEDS_ATTENTION to listOf(filming),
        ReminderCenterGroup.TODAY to listOf(editing, telugu),
        ReminderCenterGroup.SNOOZED_PAUSED to emptyList(),
        ReminderCenterGroup.UPCOMING to emptyList(),
    )

    @Test fun blankQueryReturnsSameCanonicalGroupsWithoutReordering() {
        assertSame(grouped, V219ReminderSearchPolicy.filter(grouped, ""))
        assertSame(grouped, V219ReminderSearchPolicy.filter(grouped, "  \t\n "))
    }

    @Test fun searchByTitleNotesPlatformAndContentTypeIsCaseAndWhitespaceInsensitive() {
        fun ids(query: String) = V219ReminderSearchPolicy.filter(grouped, query)
            .values.flatten().map { it.id }
        assertEquals(listOf("shoot"), ids(" sHOOT   oPENING "))
        assertEquals(listOf("shoot"), ids(" STUDIO  lighting "))
        assertEquals(listOf("edit"), ids("analysis  short"))
        assertEquals(listOf("shoot", "edit"), ids("YOUTUBE"))
        assertEquals(emptyList<String>(), ids("purple elephants"))
        // Tokens can match different meaningful fields of the *same* record.
        assertEquals(listOf("edit"), ids("Cinema B-ROLL"))
    }

    @Test fun preservesTeluguAndCanonicalUnicodeCombiningMarks() {
        fun ids(q: String) = V219ReminderSearchPolicy.filter(grouped, q)
            .values.flatten().map { it.id }
        assertEquals(listOf("te"), ids("సినిమా"))
        assertEquals(listOf("te"), ids("తెలుగు విశ్లేషణ"))
        // NFC harmonizes equivalent composed/decomposed Latin forms.
        val cafe = task("cafe", "Café Film")
        val withCafe = grouped + (ReminderCenterGroup.UPCOMING to listOf(cafe))
        val results = V219ReminderSearchPolicy.filter(withCafe, "Cafe\u0301")
        assertEquals(listOf("cafe"), results[ReminderCenterGroup.UPCOMING]!!.map { it.id })
    }

    @Test fun categoryAndSearchOnlyExposeMatchingOriginalTaskIds() {
        val matches = V219ReminderSearchPolicy.filter(grouped, "youtube")
        assertEquals(setOf("shoot", "edit"),
            V219ReminderSearchPolicy.visibleIds(matches, V219ReminderHubFilter.ALL))
        assertEquals(setOf("edit"),
            V219ReminderSearchPolicy.visibleIds(matches, V219ReminderHubFilter.TODAY))
        assertEquals(emptySet<String>(),
            V219ReminderSearchPolicy.visibleIds(matches, V219ReminderHubFilter.POSTPONED))
        assertTrue(matches[ReminderCenterGroup.TODAY]!![0] === editing)
        assertTrue(grouped[ReminderCenterGroup.TODAY]!!.size == 2)
        assertFalse(V219ReminderSearchPolicy.terms("  ").isNotEmpty())
    }
}
