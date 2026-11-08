package com.framebynavin.app.personal

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceEntry
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * True nine-domain private data migration on a CLEARED disposable Android 15
 * emulator, not an owner device. All new records, JPEG bytes, old reminders,
 * old 21-day Done timestamps and the release of the durable journal checked.
 */
@RunWith(AndroidJUnit4::class)
class Phase1V2OwnerRestoreDeviceTest {
    @Test fun ownerApprovedEncryptedArchiveRestoresAllPortableDomainsWithoutLosingOwnerHistory() =
        runBlocking {
            HardeningTestEnvironment.requireCiEmulator()
            val app = InstrumentationRegistry.getInstrumentation()
                .targetContext.applicationContext
            val marker = Phase1V2RecoveryRoots.alongsideLegacy(
                StabilizationPersonalMutationGate.journalRootFor(app))
            val personal = PersonalReminderStore(app)
            val habitStore = Phase1HabitStore(app)
            check(!marker.exists() && personal.load().isEmpty() &&
                habitStore.load().isEmpty() &&
                V297ChallengeStore(app).load().isEmpty() &&
                V298ReminderGroupStore(app).load().isEmpty()) {
                "Restore device test requires disposable empty CI emulator"
            }

            val originalReminder = PersonalReminder(
                id = "owner-original", title = "Owner's original reading",
                startDate = LocalDate.of(2026, 11, 9),
                time = LocalTime.of(9, 30),
                createdAtMillis = 100L, updatedAtMillis = 100L,
            )
            val oldDone = Instant.parse("2026-11-07T04:00:00Z")
            val originalHabit = Phase1HabitChallenge(
                id = "owner-habit-21", activity = "Study filmmaking",
                startDate = LocalDate.of(2026, 11, 7),
                zoneId = "Asia/Kolkata",
            ).confirmToday(oldDone)
            personal.create(originalReminder)
            habitStore.create(originalHabit)

            val importedReminder = originalReminder.copy(
                id = "incoming-reminder", title = "Imported note reminder")
            val importedHabit = originalHabit.copy(
                id = "incoming-habit-21", activity = "Read another novel",
                confirmations = emptyMap())
            val spaceId = "045ba519-8686-4d74-8d38-98c8b95c0001"
            val photoId = "045ba519-8686-4d74-8d38-98c8b95c0002"
            val entryId = "045ba519-8686-4d74-8d38-98c8b95c0003"
            val space = V289PersonalSpace(
                id = spaceId, title = "Reading journey",
                type = V289SpaceType.READING,
                createdAtMillis = 1700000000000L,
            )
            val links = listOf(V289SpaceReminderLink(
                spaceId, importedReminder.id, 1700000000001L))
            val entry = V289SpaceEntry(
                id = entryId, spaceId = spaceId,
                kind = V289SpaceEntryKind.NOTE,
                source = V289SpaceEntrySource.MANUAL,
                occurredAtMillis = 1700000000002L,
                createdAtMillis = 1700000000002L,
                note = "First reading journal",
            )
            val spaceState = V289SpaceState(listOf(space), links, listOf(entry))
            val group = V298ReminderGroup(
                id = "incoming-reading-group",
                name = "Reading group",
                reminderIds = listOf(importedReminder.id),
                createdAtMillis = 1700000000000L,
                updatedAtMillis = 1700000000000L,
            )
            val output = ByteArrayOutputStream()
            val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            val jpeg = ByteArrayOutputStream()
            try { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, jpeg)) }
            finally { bitmap.recycle() }
            val photoBytes = jpeg.toByteArray()
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(photoBytes).joinToString("") { "%02x".format(it) }
            val photo = V290SpacePhotoEntity(
                id = photoId, spaceId = spaceId,
                takenAtMillis = 1700000000003L, addedAtMillis = 1700000000003L,
                fileName = photoId + ".jpg", sha256Hex = digest,
                widthPx = 4, heightPx = 4, note = "Private owner image",
            )
            val imported = V300PersonalComponents(
                personal = PersonalCompletionBackupCodec.encode(
                    listOf(importedReminder), emptyList()),
                challenges = V297ChallengeCodec.encode(emptyList()),
                groups = V298ReminderGroupCodec.encode(listOf(group)),
                spaces = V289SpacesMetadataCodec.encode(spaceState),
                singleActivityChallenges = Phase1HabitCodec.encode(
                    listOf(importedHabit)),
            )
            val pass = "ci-owner-only-archive-2026".toCharArray()
            val escrowPass = "ci-owner-only-escrow-2026".toCharArray()
            V300EncryptedPersonalArchive.write(
                imported,
                listOf(V300PhotoSource(photo, photoBytes.size.toLong()) {
                    ByteArrayInputStream(photoBytes)
                }),
                pass, output,
            )
            val encrypted = output.toByteArray()
            val coordinator = Phase1V2OwnerRestoreCoordinator(app)
            val blocked = runCatching {
                coordinator.restoreOwnerApproved(
                    ByteArrayInputStream(encrypted),
                    pass, escrowPass,
                    ownerConfirmedAdditions = false,
                    acknowledgedNonPortableAlarmsAndSongs = true,
                )
            }
            assertTrue("No unauthenticated owner write", blocked.isFailure)
            assertFalse(marker.exists())
            assertEquals(originalHabit, habitStore.load().single())
            assertEquals(originalReminder, personal.load().single())

            val completed = coordinator.restoreOwnerApproved(
                ByteArrayInputStream(encrypted),
                pass, escrowPass,
                ownerConfirmedAdditions = true,
                acknowledgedNonPortableAlarmsAndSongs = true,
            )
            assertTrue(completed.completedAndJournalArchived)
            assertFalse("Durable v2 journal released only after full verification",
                marker.exists())
            val oldHabit = Phase1HabitStore(app).load()
                .single { it.id == originalHabit.id }
            assertEquals(oldDone, oldHabit.confirmations.values.single())
            assertEquals(originalHabit, oldHabit)
            assertTrue(Phase1HabitStore(app).load().any { it.id == importedHabit.id })
            assertEquals(listOf(originalReminder, importedReminder),
                PersonalReminderStore(app).load())
            val verified = V301LocalRecoveryReader(app).readV2()
            assertTrue(verified.localIntegrityVerified)
            assertEquals(completed.installedDigest,
                Phase1V2RestoreFingerprint.calculate(
                    verified.components, verified.photos))
            assertEquals(listOf(group),
                V298ReminderGroupCodec.decode(verified.components.groups))
            val spaces = V303SpaceReceiptArchiveCodec.decode(
                verified.components.spaces,
                PersonalCompletionBackupCodec.decode(verified.components.personal),
            )
            assertEquals(listOf(space), spaces.spaces)
            assertEquals(links, spaces.links)
            assertEquals(listOf(entry), spaces.entries)
            assertEquals(listOf(photo), verified.photos)
            assertArrayEquals(photoBytes,
                File(File(File(app.filesDir, "backlot-personal-space-photos"),
                    spaceId), photo.fileName).readBytes())
            assertTrue(File(app.filesDir,
                "backlot-personal-v2-completed-journals/" +
                    completed.transactionId).isDirectory)
            assertTrue(File(app.filesDir,
                "backlot-personal-v2-encrypted-transactions/" +
                    completed.transactionId +
                    "/original-escrow-encrypted.zip").isFile)
        }
}
