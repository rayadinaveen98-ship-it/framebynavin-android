package com.framebynavin.app.ui

import com.framebynavin.app.personal.spaces.V289SpaceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure read-only Home projection; no owner data, Room or OS state is touched. */
class V340HomeSpacesPolicyTest {
    private fun space(
        id: String,
        title: String = "Space $id",
        order: Int = 0,
        at: Long = 1_000L,
        archivedAt: Long? = null,
    ) = V289SpaceEntity(
        id = id,
        title = title,
        type = "READING",
        iconKey = "book-open",
        goalText = "",
        createdAtMillis = at,
        archivedAtMillis = archivedAt,
        displayOrder = order,
    )

    @Test fun activeSpacesAppearInRealOwnerOrderAndArchivedDoNotTakeSlots() {
        val previews = V340HomeSpacesPolicy.preview(listOf(
            space("z", order = 3, at = 1_000L),
            space("archived", order = 0, archivedAt = 2_000L),
            space("a", order = 1, at = 2_000L),
            space("b", order = 1, at = 1_000L),
        ))
        assertEquals(3, previews.activeCount)
        assertEquals(1, previews.archivedCount)
        assertEquals(listOf("b", "a"), previews.featured.map { it.id })
        assertEquals(listOf("b", "a", "z"), previews.active.map { it.id })
    }

    @Test fun overviewIsHonestForNewAndAllArchivedOwners() {
        val empty = V340HomeSpacesPolicy.preview(emptyList())
        assertEquals(0, empty.activeCount)
        assertEquals(0, empty.archivedCount)
        assertTrue(empty.featured.isEmpty())

        val archived = V340HomeSpacesPolicy.preview(listOf(
            space("one", archivedAt = 2_000L),
            space("two", archivedAt = 3_000L),
        ))
        assertEquals(0, archived.activeCount)
        assertEquals(2, archived.archivedCount)
        assertTrue(archived.featured.isEmpty())
    }

    @Test fun routeOnlyOpensExistingActiveIdWithNoFallthrough() {
        val current = listOf(
            space("active", title = "Skin Care"),
            space("archived", archivedAt = 2_000L),
        )
        assertEquals("active",
            V340HomeSpacesPolicy.openableSpaceId("active", current))
        assertNull(V340HomeSpacesPolicy.openableSpaceId("archived", current))
        assertNull(V340HomeSpacesPolicy.openableSpaceId("deleted", current))
        assertNull(V340HomeSpacesPolicy.openableSpaceId("", current))
        assertNull(V340HomeSpacesPolicy.openableSpaceId(null, current))
    }

    @Test fun duplicateDisplayNamesDoNotConfuseExactIdRouting() {
        val current = listOf(space("first", title = "Reading"),
            space("second", title = "Reading"))
        assertEquals("second", V340HomeSpacesPolicy.openableSpaceId("second", current))
        assertEquals("first", V340HomeSpacesPolicy.openableSpaceId("first", current))
    }
}
