package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorGuidedTourStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V207GuidedTourPolicyTest {
    @Test fun routeIsFourLessonsAndNoProjectCreationRequired() {
        var current: CreatorGuidedTourStep? = CreatorGuidedTourStep.TODAY
        val route = mutableListOf<CreatorGuidedTourStep>()
        while (current != null) {
            route.add(current)
            current = V207GuidedTourPolicy.next(current)
        }
        assertEquals(
            listOf(
                CreatorGuidedTourStep.TODAY,
                CreatorGuidedTourStep.IDEAS,
                CreatorGuidedTourStep.PROJECT,
                CreatorGuidedTourStep.INSIGHTS,
            ),
            route,
        )
        assertEquals(4, route.size)
        assertTrue(V207GuidedTourPolicy.isLast(CreatorGuidedTourStep.INSIGHTS))
    }

    @Test fun backReturnsPriorLessonRatherThanMarkingTourComplete() {
        assertNull(V207GuidedTourPolicy.previous(CreatorGuidedTourStep.TODAY))
        assertEquals(CreatorGuidedTourStep.TODAY, V207GuidedTourPolicy.previous(CreatorGuidedTourStep.IDEAS))
        assertEquals(CreatorGuidedTourStep.IDEAS, V207GuidedTourPolicy.previous(CreatorGuidedTourStep.PROJECT))
        assertEquals(CreatorGuidedTourStep.PROJECT, V207GuidedTourPolicy.previous(CreatorGuidedTourStep.INSIGHTS))
        assertFalse(V207GuidedTourPolicy.isLast(CreatorGuidedTourStep.PROJECT))
    }

    @Test fun legacySavedTourStatesContinueSafely() {
        assertEquals(2, V207GuidedTourPolicy.indexOf(CreatorGuidedTourStep.WORKSPACE))
        assertEquals(3, V207GuidedTourPolicy.indexOf(CreatorGuidedTourStep.CONTROL))
        assertEquals(CreatorGuidedTourStep.INSIGHTS, V207GuidedTourPolicy.next(CreatorGuidedTourStep.WORKSPACE))
        assertNull(V207GuidedTourPolicy.next(CreatorGuidedTourStep.CONTROL))
    }
}
