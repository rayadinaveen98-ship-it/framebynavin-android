package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshStartPolicyTest {
    @Test fun missingOrWrongAcknowledgementCannotErase() {
        assertFalse(FreshStartPolicy.allowed("", true, true))
        assertFalse(FreshStartPolicy.allowed("ERASE THIS PHONE", false, true))
        assertFalse(FreshStartPolicy.allowed("ERASE THIS PHONE", true, false))
        assertFalse(FreshStartPolicy.allowed("erase this phone", true, true))
        assertFalse(FreshStartPolicy.allowed("ERASE PHONE", true, true))
    }
    @Test fun exactPhrasePlusBothAcknowledgementsRequired() {
        assertTrue(FreshStartPolicy.allowed("ERASE THIS PHONE", true, true))
    }
}
