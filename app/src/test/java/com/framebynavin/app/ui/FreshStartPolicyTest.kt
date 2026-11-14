package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FreshStartPolicyTest {
    private val clean = V383DeviceResetInspection(true, emptyList())

    private fun approved(
        phrase: String = "ERASE THIS PHONE",
        loss: Boolean = true,
        cloud: Boolean = true,
        media: Boolean = true,
        recovery: Boolean = true,
        finalLoss: Boolean = true,
        preflight: V383DeviceResetInspection = clean,
    ) = FreshStartPolicy.allowed(
        phrase, loss, cloud, media, recovery, finalLoss, preflight,
    )

    @Test fun wrongPhraseOrMissingAcknowledgementCannotErase() {
        assertFalse(approved(phrase = ""))
        assertFalse(approved(phrase = "erase this phone"))
        assertFalse(approved(phrase = "ERASE PHONE"))
        assertFalse(approved(loss = false))
        assertFalse(approved(cloud = false))
        assertFalse(approved(media = false))
        assertFalse(approved(recovery = false))
        assertFalse(approved(finalLoss = false))
    }

    @Test fun recoveryOrUnreadableStorageBlocksEvenFullyAcknowledgedErase() {
        assertFalse(approved(preflight = V383DeviceResetInspection(false, emptyList())))
        assertFalse(approved(preflight = V383DeviceResetInspection(
            true, listOf("backlot-unified-cross-domain-restore-journal-v1"),
        )))
    }

    @Test fun exactPhraseAndAllFiveAcknowledgementsWithCleanPreflightMayErase() {
        assertTrue(approved())
        assertTrue(approved(phrase = " ERASE THIS PHONE "))
    }
}

class V383UnifiedDeviceResetPreflightTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun cleanAppPrivateDirectoryMayOfferReset() {
        val result = V383UnifiedDeviceResetPreflight.inspect(temp.root)
        assertTrue(result.storageReadable)
        assertTrue(result.mayOfferReset)
        assertTrue(result.blockingRecoveryMarkers.isEmpty())
    }

    @Test fun everyKnownActiveRecoveryMarkerBlocksWithoutDeletingIt() {
        val names = listOf(
            "backlot-unified-cross-domain-restore-journal-v1",
            "backlot-unified-owner-recovery-decision-v1",
            "backlot-unified-owner-recovery-escrow-v1",
            "backlot-unified-owner-restore-preparation-v1",
            "backlot-unified-creator-voice-install-v1",
            "backlot-unified-creator-voice-shadow-v1",
            "backlot-unified-creator-original-inverse-v1",
            "backlot-unified-personal-v2-handoff-v1",
            "backlot-unified-personal-v2-reverse-actions-v1",
            "backlot-unified-verified-outcome-v1",
            "backlot-unified-final-release-v1",
            "backlot-personal-active-restore-transaction",
            "backlot-personal-active-restore-transaction-v2",
        )
        names.forEach { name ->
            val marker = File(temp.root, name)
            assertTrue(marker.mkdir())
            val result = V383UnifiedDeviceResetPreflight.inspect(temp.root)
            assertFalse(result.mayOfferReset)
            assertEquals(listOf(name), result.blockingRecoveryMarkers)
            assertTrue(marker.isDirectory)
            assertTrue(marker.delete())
        }
    }

    @Test fun creatorPendingAtomicMarkerBlocksAndIsPreserved() {
        val recovery = temp.newFolder("creator_restore_recovery_v181")
        for (name in listOf("pending.json", "pending.json.new", "pending.json.bak")) {
            val marker = File(recovery, name)
            assertTrue(marker.createNewFile())
            val result = V383UnifiedDeviceResetPreflight.inspect(temp.root)
            assertFalse(result.mayOfferReset)
            assertTrue(result.blockingRecoveryMarkers.any { it.contains("creator_restore") })
            assertTrue(marker.exists())
            assertTrue(marker.delete())
        }
    }

    @Test fun missingOrNonDirectoryPrivateRootFailsClosed() {
        assertFalse(V383UnifiedDeviceResetPreflight.inspect(
            File(temp.root, "missing"),
        ).mayOfferReset)
        val regular = temp.newFile("not-a-directory")
        assertFalse(V383UnifiedDeviceResetPreflight.inspect(regular).mayOfferReset)
    }

    @Test fun completedHistoryByItselfIsNotAnActiveRestoreBlocker() {
        temp.newFolder("backlot-unified-recovery-history-v1")
        assertTrue(V383UnifiedDeviceResetPreflight.inspect(temp.root).mayOfferReset)
    }
}
