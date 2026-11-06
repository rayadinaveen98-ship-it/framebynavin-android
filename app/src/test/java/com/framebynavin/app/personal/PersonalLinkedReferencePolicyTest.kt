package com.framebynavin.app.personal

import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

/** No real local records, just pure link-preservation decisions. */
class PersonalLinkedReferencePolicyTest {
    @Test fun emptyDeviceHasNoLinkBlocker() {
        PersonalLinkedReferencePolicy.requireRetained(
            emptySet(), emptySet(), emptySet())
    }

    @Test fun identicalGroupAndSpaceLinksAllowPersonalReplacement() {
        PersonalLinkedReferencePolicy.requireRetained(
            setOf("r1", "r2", "r3"), setOf("r1", "r2"), setOf("r3"))
    }

    @Test fun missingGroupLinkedReminderBlocksCloudCopy() {
        assertThrows(PersonalLinkProtectionException::class.java) {
            PersonalLinkedReferencePolicy.requireRetained(
                setOf("r2"), setOf("r1"), emptySet())
        }
    }

    @Test fun missingSpaceLinkedReminderBlocksCloudCopy() {
        assertThrows(PersonalLinkProtectionException::class.java) {
            PersonalLinkedReferencePolicy.requireRetained(
                setOf("r2"), emptySet(), setOf("r1"))
        }
    }

    @Test fun missingIdsInBothDomainsAlsoBlockReplacement() {
        assertThrows(PersonalLinkProtectionException::class.java) {
            PersonalLinkedReferencePolicy.requireRetained(
                emptySet(), setOf("r1"), setOf("r2"))
        }
    }

    @Test fun failureMessageContainsNoPrivateReminderIds() {
        val error = assertThrows(PersonalLinkProtectionException::class.java) {
            PersonalLinkedReferencePolicy.requireRetained(
                setOf("safe"), setOf("highly-private-123"), emptySet())
        }
        assertTrue(error.message.orEmpty().contains("Nothing was restored"))
        assertTrue(!error.message.orEmpty().contains("highly-private-123"))
    }
}
