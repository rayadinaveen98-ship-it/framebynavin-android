package com.framebynavin.app.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class CreatorGeminiVideoAutopsyProviderTest {
    @Test
    fun `evidence ids are extracted uniquely in response order`() {
        val ids = CreatorGeminiVideoAutopsyProvider().extractEvidenceIds(
            "Strong opening [project.selected_hook], then a drop [retention.10]. The hook [project.selected_hook] remains relevant."
        )
        assertEquals(listOf("project.selected_hook", "retention.10"), ids)
    }
}
