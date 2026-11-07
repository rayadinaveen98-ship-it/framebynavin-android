package com.framebynavin.app.personal

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Entirely synthetic JPEG test, after an explicitly cleared Android 15 emulator. */
@RunWith(AndroidJUnit4::class)
class B05VerifiedPhotoForwardResumeDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun verifiedJpegAlreadyCopiedButMissingRoomRowIsReattachedAfterOwnerApproval() =
        runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val root = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!root.exists()) { "Test requires independently cleared CI emulator profile" }
        val owner = PersonalReminder(
            id = "b05-photo-owner", title = "Original untouched reminder",
            startDate = LocalDate.of(2026, 12, 11), time = LocalTime.of(9, 0),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val incoming = PersonalReminder(
            id = "b05-photo-import", title = "Imported synthetic photo reminder",
            startDate = LocalDate.of(2026, 12, 12), time = LocalTime.of(10, 0),
            createdAtMillis = 200L, updatedAtMillis = 200L,
        )
        PersonalReminderStore(app).create(owner)
        val previous = V301LocalRecoveryReader(app).read()
        val spaceId = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
        val photoId = "729d2bfa-af37-4836-af9e-c14c3d836421"
        val jpeg = ByteArrayOutputStream().also { out ->
            val bitmap = Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.CYAN)
                check(bitmap.compress(Bitmap.CompressFormat.JPEG,88,out))
            } finally {bitmap.recycle()}
        }.toByteArray()
        val sha = MessageDigest.getInstance("SHA-256").digest(jpeg)
            .joinToString("") { "%02x".format(it) }
        val photo = V290SpacePhotoEntity(
            id = photoId,spaceId = spaceId,
            takenAtMillis = 300L,addedAtMillis = 301L,
            fileName = "$photoId.jpg",sha256Hex = sha,
            widthPx = 16,heightPx = 16,note = "Synthetic jpeg",
        )
        val portable = PersonalCompletionBackupSnapshot(listOf(incoming),emptyList())
        val import = V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(
                portable.reminders,portable.completions,
            ),
            challenges = V297ChallengeCodec.encode(emptyList()),
            groups = V298ReminderGroupCodec.encode(emptyList()),
            spaces = V303SpaceReceiptArchiveCodec.encode(
                V289SpaceState(spaces=listOf(V289PersonalSpacePolicy.newSpace(
                    V289SpaceType.FITNESS,300L,spaceId,
                ))),portable,
            ),
        )
        val password = "Synthetic-B05-Photo-Recovery-2026".toCharArray()
        val bytes = ByteArrayOutputStream().also { out ->
            V300EncryptedPersonalArchive.write(
                import,
                listOf(V300PhotoSource(photo,jpeg.size.toLong()) {
                    ByteArrayInputStream(jpeg)
                }),
                password,out,
            )
        }.toByteArray()
        val prepared = V304ArchivePreparationCoordinator(app)
            .prepareOwnerRequested(ByteArrayInputStream(bytes),password,true)
        val escrow = V305RollbackEscrowCoordinator(app)
            .createOwnerApprovedEscrow(password,ownerRequested = true)
        val barrier = V305CommitBarrierPolicy.check(prepared,escrow,previous)
        val target = StabilizationRestoreTargetPolicy.calculate(
            previous,import,listOf(photo),
        )
        StabilizationPersonalMutationGate.withOwnerMutation(app) {
            val journal=StabilizationRestoreWriteAheadJournal(root)
            val first=journal.prepare(
                barrier,target.targetDigest,true,System.currentTimeMillis(),
            )
            StabilizationPersonalMutationGate.withPreparedRecovery(root,first) {
                suspend fun advance(
                    from:StabilizationRestoreWriteStep,to:StabilizationRestoreWriteStep,
                ) {journal.checkpoint(from,to,System.currentTimeMillis())}
                advance(StabilizationRestoreWriteStep.PREPARED,
                    StabilizationRestoreWriteStep.BEFORE_PERSONAL)
                PersonalReminderStore(app).replaceFromApprovedImport(target.merged.personal)
                advance(StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                    StabilizationRestoreWriteStep.PERSONAL_WRITTEN)
                advance(StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_CHALLENGES)
                V297ChallengeStore(app).replaceFromApprovedImport(target.merged.challenges)
                advance(StabilizationRestoreWriteStep.BEFORE_CHALLENGES,
                    StabilizationRestoreWriteStep.CHALLENGES_WRITTEN)
                advance(StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_GROUPS)
                V298ReminderGroupStore(app).importMissingAfterApproval(target.merged.groups)
                advance(StabilizationRestoreWriteStep.BEFORE_GROUPS,
                    StabilizationRestoreWriteStep.GROUPS_WRITTEN)
                advance(StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_SPACES)
                B03RestoreDomainWriter(app).writeSpaces(target.merged)
                advance(StabilizationRestoreWriteStep.BEFORE_SPACES,
                    StabilizationRestoreWriteStep.SPACES_WRITTEN)
                advance(StabilizationRestoreWriteStep.SPACES_WRITTEN,
                    StabilizationRestoreWriteStep.BEFORE_PHOTOS)
                // File bytes were durably copied, but app death struck BEFORE
                // the separate Room photo row transaction. This should be
                // safe to replay without rewriting or losing any JPEG byte.
                V300EncryptedPersonalArchive.stageAndVerify(
                    ByteArrayInputStream(bytes),password,app.cacheDir,
                ).use { staged ->
                    val installer = StabilizationVerifiedPhotoInstaller(
                        File(app.filesDir,"backlot-personal-space-photos"),
                    )
                    assertEquals(
                        StabilizationPhotoInstallResult.INSTALLED,
                        installer.installVerified(
                            photo,staged.photos.single().stagedFile,
                            jpeg.size.toLong(),
                        ),
                    )
                }
            }
        }
        val originalJournal=checkNotNull(
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()
        )
        assertEquals(StabilizationRestoreWriteStep.BEFORE_PHOTOS,originalJournal.step)
        val db = V289SpacesDatabase.open(app)
        try {assertNull(db.spaces().getPhoto(photoId))}
        finally {db.close()}
        val file=File(File(File(app.filesDir,
            "backlot-personal-space-photos"),spaceId),"$photoId.jpg")
        assertArrayEquals(jpeg,file.readBytes())
        val consent = B04OwnerRecoveryConsent(
            transactionId=originalJournal.transactionId,
            preparedId=originalJournal.preparedId,
            escrowId=originalJournal.escrowId,
            originalDigest=originalJournal.originalDigest,
            targetDigest=originalJournal.targetDigest,
            selectedChoice=B04OwnerRecoveryChoice.RESUME_AUTHENTICATED_TARGET,
            acknowledgedDeviceLocalLimitations=true,
            ownerConfirmed=true,
        )
        val recovery=B05VerifiedPhotoForwardResumeCoordinator(app)
        assertThrows(Exception::class.java) {
            runBlocking {
                recovery.resumeOwnerApproved(
                    ByteArrayInputStream(bytes),
                    "invalid-import-password".toCharArray(),
                    password,consent,
                )
            }
        }
        assertEquals(originalJournal,
            StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart())
        val result=recovery.resumeOwnerApproved(
            ByteArrayInputStream(bytes),password,password,consent,
        )
        assertEquals(StabilizationRestoreWriteStep.VERIFYING,result.stage)
        assertEquals(target.targetDigest,result.actualDigest)
        val verifiedDb=V289SpacesDatabase.open(app)
        try {assertEquals(photo,verifiedDb.spaces().getPhoto(photoId))}
        finally {verifiedDb.close()}
        assertArrayEquals(jpeg,file.readBytes())
        assertEquals(listOf(owner,incoming),PersonalReminderStore(app).load())
        val current=checkNotNull(StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart())
        val approval=consent.copy(
            selectedChoice=B04OwnerRecoveryChoice.FINALIZE_VERIFIED_TARGET,
        )
        B04OwnerVerifiedTargetEvidenceCoordinator(app)
            .recordOwnerVerifiedTarget(escrow.id,password,approval)
        val archived=B04OwnerFinalizedRecoveryCoordinator(app)
            .finalizeOwnerApprovedTarget(escrow.id,password,approval)
        assertFalse(root.exists())
        assertEquals(current,
            StabilizationRestoreWriteAheadJournal(archived).inspectAfterRestart())
        assertArrayEquals(jpeg,file.readBytes())
        assertEquals(
            "Original still editable",
            PersonalReminderStore(app).update(owner.id) {
                it.copy(title="Original still editable")
            }.first().title,
        )
    }
}
