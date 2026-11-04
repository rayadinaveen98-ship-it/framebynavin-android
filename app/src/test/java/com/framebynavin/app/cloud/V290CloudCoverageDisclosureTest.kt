package com.framebynavin.app.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** UI promises must not overstate actual cross-device backup guarantees. */
class V290CloudCoverageDisclosureTest {
    @Test fun publicCopyIncludesOnlyExistingSupportedDomains() {
        val text = V290CloudCoverageDisclosure.INCLUDED
        assertTrue(text.contains("Creator projects"))
        assertTrue(text.contains("Personal Reminders"))
        assertTrue(text.contains("completion history"))
        assertFalse(text.contains("everything"))
        assertFalse(text.contains("all data"))
    }

    @Test fun userIsExplicitlyWarnedThatSpacesAndPhotoBytesAreNotSynced() {
        val notice = V290CloudCoverageDisclosure.EXCLUDED
        assertTrue(notice.contains("NOT yet"))
        assertTrue(notice.contains("Personal Spaces"))
        assertTrue(notice.contains("check-in notes"))
        assertTrue(notice.contains("private photo journals"))
        assertTrue(notice.contains("photo bytes stay on"))
        assertTrue(notice.contains("export Spaces metadata separately"))
    }

    @Test fun greenCloudStatusCannotBeDescribedAsCompleteBackup() {
        val explanation = V290CloudCoverageDisclosure.SUCCESS_SCOPE
        assertTrue(explanation.contains("supported Creator and Personal"))
        assertTrue(explanation.contains("does not confirm a complete Backlot backup"))
        assertTrue(explanation.contains("photo recovery"))
    }
}
