package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.File
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Package J: CI-emulator-only, NON-DESTRUCTIVE local reset acceptance.
 * NEVER presses final ERASE, clears real user records, or invokes Android's
 * clearApplicationUserData. All synthetic markers live in a disposable cache
 * test root, not Backlot's live recovery or private photo directories.
 */
@RunWith(AndroidJUnit4::class)
class V383DeviceResetSafetyAndroidTest {
    @get:Rule val rule = createAndroidComposeRule<FreshStartActivity>()

    @Test fun typedPhraseAloneDoesNotEnableReset() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.onNodeWithText("Start fresh on this phone").assertExists()
        rule.onNodeWithTag("fresh-start-review").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("fresh-start-confirm-phrase").performScrollTo()
            .performTextInput(FreshStartPolicy.CONFIRM_PHRASE)
        // Five independent owner acknowledgements remain necessary.
        rule.onNodeWithTag("fresh-start-review").performScrollTo().assertIsNotEnabled()
    }

    @Test fun syntheticInterruptedRecoveryIsDetectedWithoutDeletingEvidence() {
        HardeningTestEnvironment.requireCiEmulator()
        val phone = InstrumentationRegistry.getInstrumentation().targetContext
        val fake = File(phone.cacheDir, "v383-non-destructive-qa-" + UUID.randomUUID())
        assertTrue(fake.mkdir())
        try {
            assertTrue(V383UnifiedDeviceResetPreflight.inspect(fake).mayOfferReset)
            val marker = File(fake, "backlot-unified-cross-domain-restore-journal-v1")
            assertTrue(marker.mkdir())
            val pending = File(marker, "active.journal")
            pending.writeText("synthetic-only; NOT a real restore")
            val inspected = V383UnifiedDeviceResetPreflight.inspect(fake)
            assertTrue(inspected.storageReadable)
            assertFalse(inspected.mayOfferReset)
            assertTrue(inspected.blockingRecoveryMarkers.contains(marker.name))
            assertTrue(pending.readText().startsWith("synthetic-only"))
        } finally {
            // Only test-owned files. Never traverse app-wide recovery roots.
            assertTrue(fake.canonicalFile.parentFile == phone.cacheDir.canonicalFile)
            fake.deleteRecursively()
        }
    }
}
