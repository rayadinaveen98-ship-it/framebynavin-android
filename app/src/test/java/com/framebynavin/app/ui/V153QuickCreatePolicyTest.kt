package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V153QuickCreatePolicyTest {
    private fun eligible(
        title: String = "A new film analysis",
        archetypeId: String = "analysis",
        platform: String = "YouTube",
        format: String = "Long",
        dueAt: Long = 10_000L,
        now: Long = 1_000L,
        supportReady: Boolean = true,
    ) = V153QuickCreatePolicy.canCreate(
        title = title,
        archetypeId = archetypeId,
        platform = platform,
        format = format,
        dueAtMillis = dueAt,
        nowMillis = now,
        supportReady = supportReady,
    )

    @Test fun validQuickCreateCanSaveWithoutOpeningEveryAdvancedStep() {
        assertTrue(eligible())
    }

    @Test fun missingOrWhitespaceOnlyTitleCannotCreate() {
        assertFalse(eligible(title = ""))
        assertFalse(eligible(title = "   "))
    }

    @Test fun missingRequiredProjectMetadataCannotCreate() {
        assertFalse(eligible(archetypeId = ""))
        assertFalse(eligible(platform = ""))
        assertFalse(eligible(format = ""))
    }

    @Test fun expiredDueDateCannotCreate() {
        assertFalse(eligible(dueAt = 1_000L))
        assertFalse(eligible(dueAt = 999L))
    }

    @Test fun invalidCustomReminderCannotBeBypassed() {
        assertFalse(eligible(supportReady = false))
    }
}
