package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V200ReminderLayoutPolicyTest {
    @Test fun compactModeFitsSmallPhones() {
        assertTrue(V200ReminderLayoutPolicy.isCompact(719))
        assertFalse(V200ReminderLayoutPolicy.isCompact(720))
        assertFalse(V200ReminderLayoutPolicy.useHorizontalActions(389))
        assertTrue(V200ReminderLayoutPolicy.useHorizontalActions(390))
    }

    @Test fun duplicateOrEmptyDirectionsDisappear() {
        assertNull(V200ReminderLayoutPolicy.supportingAction("", "Call editor"))
        assertNull(V200ReminderLayoutPolicy.supportingAction(" call EDITOR ", "Call editor"))
        assertNull(V200ReminderLayoutPolicy.supportingAction("  ", "Call editor"))
    }

    @Test fun usefulDirectionsRemainReadable() {
        assertEquals("Gather notes", V200ReminderLayoutPolicy.supportingAction(" Gather notes ", "Call editor"))
    }
}
