package com.framebynavin.app.personal

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpaceEntryEntity
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable in-memory Room only; never creates the user's Spaces DB. */
@RunWith(AndroidJUnit4::class)
class V303SpaceReceiptDeviceTest {
    @Test fun realRoomDoneRowRequiresMatchingPersonalReceiptAndNeverWritesOwnerData() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val spaceId = "571ce188-e3bf-4543-8e5c-dce55962e40a"
        val reminderId = "synthetic-private-reminder"
        val doneAt = 2_500L
        val occurrence = 2_000L
        val evidence = PersonalCompletionBackupSnapshot(
            reminders = listOf(PersonalReminder(
                id = reminderId, title = "Private owner action",
                startDate = LocalDate.of(2026, 11, 12),
                time = LocalTime.of(9, 0),
                createdAtMillis = 1000L, updatedAtMillis = 1000L,
            )),
            completions = listOf(PersonalCompletionEvent(
                reminderId, occurrence, doneAt,
            )),
        )
        try {
            runBlocking {
                db.spaces().insertSpace(V289SpaceEntity(
                    id = spaceId, title = "Private Space", type = "CUSTOM",
                    iconKey = "sparkles", goalText = "", createdAtMillis = 1000L,
                    archivedAtMillis = null, displayOrder = 0,
                ))
                db.spaces().insertEntry(V289SpaceEntryEntity(
                    id = "6b8cc81d-50a0-4bb2-96aa-3c93dcb1e568",
                    spaceId = spaceId,
                    kind = V289SpaceEntryKind.CHECK_IN.name,
                    source = V289SpaceEntrySource.CONFIRMED_REMINDER_DONE.name,
                    occurredAtMillis = doneAt,
                    createdAtMillis = doneAt + 10,
                    note = "", metricName = null, metricValue = null,
                    metricUnit = null, reminderId = reminderId,
                    originalOccurrenceMillis = occurrence,
                ))
                val syntheticPhoto = V290SpacePhotoEntity(
                    id = "2072a69b-f03b-420e-a0e3-8d68cb936f44",
                    spaceId = spaceId,
                    takenAtMillis = 1500L, addedAtMillis = 1600L,
                    fileName = "2072a69b-f03b-420e-a0e3-8d68cb936f44.jpg",
                    sha256Hex = "a".repeat(64), widthPx = 400,
                    heightPx = 600, note = "",
                )
                db.spaces().insertPhoto(syntheticPhoto)
                val consistent = V303SpaceSnapshotReader(db).readWithPhotos()
                val snapshot = consistent.state
                assertEquals(1, snapshot.entries.size)
                assertEquals(listOf(syntheticPhoto), consistent.photos)
                val archive = V303SpaceReceiptArchiveCodec.encode(snapshot, evidence)
                val restored = V303SpaceReceiptArchiveCodec.decode(archive, evidence)
                assertEquals(snapshot, restored)
                assertThrows(IllegalArgumentException::class.java) {
                    V303SpaceReceiptArchiveCodec.decode(
                        archive, evidence.copy(completions = emptyList()),
                    )
                }
            }
        } finally {
            db.close()
        }
    }
}
