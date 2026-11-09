package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalBackupTransfer
import com.framebynavin.app.personal.Phase1HabitStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Actual Android DataStore, read-only: no cloud calls, no imports, no owner
 * account login and no alarms. A modern remote envelope must never be treated
 * as the old reminder-only payload even when this device has no habits.
 */
@RunWith(AndroidJUnit4::class)
class TrustT1ALegacyCloudSafetyDeviceTest {
    @Test fun modernRemoteRefusedWithoutChangingAnyLocalPersonalState() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val personal = PersonalBackupTransfer(context)
        val habits = Phase1HabitStore(context)
        val originalPersonal = personal.exportForCloud()
        val originalHabits = habits.exportJson()

        val modernRemote = PersonalCloudPortableSnapshotCodec.encode(
            originalPersonal, originalHabits,
        )
        val rejected = runCatching {
            PersonalCloudLegacyRemoteGate.requireLegacyOnly(modernRemote)
        }
        assertTrue(rejected.exceptionOrNull() is PersonalCloudNewerBackupRequiresReview)
        assertTrue(rejected.exceptionOrNull()?.message.orEmpty().contains(
            "No cloud or phone records were changed",
        ))
        assertEquals("A rejected remote snapshot never changed Personal reminders",
            originalPersonal, personal.exportForCloud())
        assertEquals("A rejected remote snapshot never changed habit Done receipts",
            originalHabits, habits.exportJson())

        // Original reminder-only cloud records are still supported as before.
        PersonalCloudLegacyRemoteGate.requireLegacyOnly(originalPersonal)
        assertEquals(originalPersonal, personal.exportForCloud())
        assertEquals(originalHabits, habits.exportJson())
    }
}
