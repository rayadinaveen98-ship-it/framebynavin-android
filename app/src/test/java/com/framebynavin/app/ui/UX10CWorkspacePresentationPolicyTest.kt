package com.framebynavin.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-authored UX10C presentation contracts.
 * DEFER ALL EXECUTION until the approved consolidated UX release gate.
 */
class UX10CWorkspacePresentationPolicyTest {
    @Test
    fun `small creator workspaces use one source-backed tool card per row`() {
        assertTrue(UX10CWorkspacePresentationPolicy.stackWorkspaceTools(320, 1f))
        assertTrue(UX10CWorkspacePresentationPolicy.stackWorkspaceTools(379, 1f))
        assertEquals(1, UX10CWorkspacePresentationPolicy.columnsPerRow(360, 1f))
    }

    @Test
    fun `normal phone and tablet keep two columns without changing tool count`() {
        assertFalse(UX10CWorkspacePresentationPolicy.stackWorkspaceTools(393, 1f))
        assertEquals(2, UX10CWorkspacePresentationPolicy.columnsPerRow(393, 1f))
        assertEquals(2, UX10CWorkspacePresentationPolicy.columnsPerRow(800, 1f))
        assertEquals(4, UX10CWorkspacePresentationPolicy.pageSizeForToolCards())
    }

    @Test
    fun `accessibility font scale forces readable single column on wide displays`() {
        assertEquals(1, UX10CWorkspacePresentationPolicy.columnsPerRow(800, 1.3f))
        assertEquals(1, UX10CWorkspacePresentationPolicy.columnsPerRow(393, 2.0f))
        assertEquals(2, UX10CWorkspacePresentationPolicy.columnsPerRow(393, 1.0f))
    }

    @Test
    fun `invalid scale fails safely to a single column rather than compressed labels`() {
        assertTrue(UX10CWorkspacePresentationPolicy.stackWorkspaceTools(
            800, Float.NaN
        ))
        assertTrue(UX10CWorkspacePresentationPolicy.stackWorkspaceTools(
            800, Float.POSITIVE_INFINITY
        ))
    }

    @Test
    fun `project wizard action targets meet the shared design floor`() {
        assertTrue(V173ProjectUiMetrics.touchTarget >= 48.dp)
        assertTrue(V173ProjectUiMetrics.actionMinHeight >= 56.dp)
        assertTrue(V173ProjectUiMetrics.reviewRowMinHeight >=
            V173ProjectUiMetrics.touchTarget)
    }

    @Test
    fun `project wizard helper sizes preserve readable labels`() {
        assertTrue(V173ProjectUiMetrics.supportingText.value >= 11f)
        assertTrue(V173ProjectUiMetrics.choiceText.value >= 11f)
        assertTrue(V173ProjectUiMetrics.bodyText.value >=
            V173ProjectUiMetrics.supportingText.value)
        assertTrue(V173ProjectUiMetrics.reviewValueText.value >= 12f)
    }

    @Test
    fun `existing idea editor footer remains responsive without writer changes`() {
        assertTrue(V227IdeaFooterPolicy.stacked(320f, 1f))
        assertTrue(V227IdeaFooterPolicy.stacked(430f, 1.3f))
        assertFalse(V227IdeaFooterPolicy.stacked(430f, 1f))
    }
}
