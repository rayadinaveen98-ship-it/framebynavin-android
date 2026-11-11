package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure display-only checks. Creator and Personal reminder engines are unchanged. */
class V331RevealStyleTest {
    @Test fun fourContextsHaveDefinedVisualAccents() {
        val variants = V329ReminderDomain.entries.flatMap { d ->
            V329ReminderChannel.entries.map { c -> d to c }
        }
        assertEquals(4, variants.size)
        variants.forEach { (domain, channel) ->
            val color = V331RevealStyle.accent(domain, channel)
            org.junit.Assert.assertTrue(color.alpha > 0f)
        }
    }

    @Test fun creatorTimeRendersOnlyIfPresentInRealDueText() {
        assertEquals("7:30 AM",
            V331RevealStyle.timeFromCreatorDueLabel("Tomorrow · 7:30 AM"))
        assertEquals("9:05 pm",
            V331RevealStyle.timeFromCreatorDueLabel("Today at 9:05 pm"))
        assertEquals("",
            V331RevealStyle.timeFromCreatorDueLabel("Tomorrow"))
    }

    @Test fun missingPersonalOccurrenceDoesNotInventTime() {
        assertEquals("", V331RevealStyle.timeLabel(0L))
    }
}
