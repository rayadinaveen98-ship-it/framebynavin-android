package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorFocusSessionCodec
import com.framebynavin.app.data.CreatorFocusSessionStore
import com.framebynavin.app.data.CreatorHeroArchive
import com.framebynavin.app.data.IdeaCaptureType
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.PortableBestFrameMedia
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalBackupTransfer
import com.framebynavin.app.personal.PersonalCompletionBackupCodec
import com.framebynavin.app.personal.Phase1HabitCodec
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V297ChallengeCodec
import com.framebynavin.app.personal.V298ReminderGroupCodec
import com.framebynavin.app.personal.V301LocalRecoveryReader
import com.framebynavin.app.personal.V303SpaceReceiptArchiveCodec
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * T1C2 internal PREVIEW source capture. Copies real user-owned bytes while
 * BOTH Backlot locks are held in the documented order:
 *
 *      CreatorDataGate.transaction -> Personal.withStableSnapshot
 *
 * Unlike an inventory, this returns app-private copies of actual content,
 * including private photo JPEG bytes and local Voice Idea originals.
 *
 * This is nevertheless NOT a complete app backup: independent settings and
 * live cloud/account data are not frozen here, and writers bypassing either
 * gate cannot be proven globally atomic. The result MUST NOT be uploaded,
 * presented as a completed backup or used for restore. The caller must use {}
 * and dispose all plaintext temporary files immediately after encryption.
 */
internal class TrustT1C2OwnerSourceCapture(context: Context) {
    private val app = context.applicationContext

    suspend fun freezePartialReadOnly(): TrustFrozenSourceFiles =
        withContext(Dispatchers.IO) {
            val freezer = TrustSourceFreezer(app.filesDir, app.cacheDir)
            try {
                CreatorDataGate.transaction {
                    StabilizationPersonalMutationGate.withStableSnapshot(app) {
                        val generation = CreatorDataGate.generation(app)

                        // Use a non-seeding task exporter: the standard
                        // TaskStore.load() opportunistically writes workflow
                        // history when legacy project data is viewed.
                        val tasks = TaskStore(app)
                        val projectsJson = tasks.exportJsonForTrustReadOnly()
                        freezer.addJson("creator/projects", projectsJson,
                            tasks.validateJson(projectsJson))

                        val ideaStore = IdeaVaultStore(app)
                        val ideasJson = ideaStore.exportJson()
                        val ideas = ideaStore.load()
                        check(ideaStore.validateJson(ideasJson) == ideas.size) {
                            "Idea Vault changed while freezing owner originals"
                        }
                        freezer.addJson("creator/ideas", ideasJson, ideas.size)

                        val focusJson = CreatorFocusSessionStore(app).exportPortableJson()
                        val focus = CreatorFocusSessionCodec.decode(focusJson)
                        freezer.addJson("creator/focus", focusJson,
                            focus.history.size + if (focus.active == null) 0 else 1)

                        val personal = V301LocalRecoveryReader(app).readV2()
                        check(personal.localIntegrityVerified) {
                            "Personal owner source integrity is not confirmed"
                        }
                        val components = personal.components
                        val reminders = PersonalCompletionBackupCodec.decode(components.personal)
                        // Device alarm leases, custom music/document URIs and
                        // exact alarm permissions are not portable. Reuse the
                        // existing reminder exporter that removes device-only
                        // fields while keeping Done history unchanged.
                        val portableReminders = PersonalBackupTransfer(app).exportForCloud()
                        val portable = PersonalCompletionBackupCodec.decode(portableReminders)
                        check(
                            portable.reminders.map { it.id } == reminders.reminders.map { it.id } &&
                            portable.completions == reminders.completions
                        ) {
                            "Personal reminders changed while creating a portable owner copy"
                        }
                        val challenges = Phase1HabitCodec.decode(
                            checkNotNull(components.singleActivityChallenges) {
                                "21-day challenge Done receipts cannot be omitted"
                            },
                        )
                        val legacy = V297ChallengeCodec.decode(components.challenges)
                        val groups = V298ReminderGroupCodec.decode(components.groups)
                        val spaces = V303SpaceReceiptArchiveCodec.decode(
                            components.spaces, reminders,
                        )

                        // Each source is independently immutable after copying.
                        // These are five real v2 Personal domains, not an old
                        // Personal-only cloud envelope or fabricated blanks.
                        freezer.addJson("personal/reminders-history", portableReminders,
                            reminders.reminders.size + reminders.completions.size)
                        freezer.addJson("personal/legacy-challenges", components.challenges,
                            legacy.size)
                        freezer.addJson("personal/reminder-groups", components.groups,
                            groups.size)
                        freezer.addJson("personal/spaces-journal-links", components.spaces,
                            spaces.spaces.size + spaces.entries.size + spaces.links.size)
                        freezer.addJson("personal/single-activity-receipts",
                            checkNotNull(components.singleActivityChallenges),
                            challenges.size + challenges.sumOf { it.confirmations.size })

                        // Every DB-indexed JPEG must have verified original
                        // bytes. V301 has already refused untracked/orphan files,
                        // and this copy requires its expected SHA again.
                        val privatePhotos = personal.photos.sortedBy { it.id }
                        privatePhotos.forEachIndexed { index, photo ->
                            val original = File(
                                File(app.filesDir, "backlot-personal-space-photos"),
                                photo.spaceId + "/" + photo.fileName,
                            )
                            freezer.addOwnedFile(
                                "personal/photo-" + index.toString().padStart(4, '0'),
                                original, 1, photo.sha256Hex,
                            )
                        }

                        // Creator active Best Frames are also the saved Home
                        // slideshow originals. Retired/orphan originals remain
                        // unverified and block any universal-backup claim.
                        val preparedFrames = PortableBestFrameMedia.prepareFromFiles(
                            app.filesDir, CreatorHeroArchive.orderedSourceNames(app),
                        )
                        preparedFrames.frames.forEachIndexed { index, frame ->
                            freezer.addOwnedFile(
                                "creator/best-frame-" + index.toString().padStart(4, '0'),
                                frame.file, 1, frame.sha256,
                            )
                        }

                        // Check local-only recordings without treating a remote
                        // storage pointer as evidence that owner bytes exist.
                        // Pending takes/orphan files are NOT automatically removed.
                        val recordingProof = TrustVoiceIdeaInventory.inspect(
                            app.filesDir, ideas,
                        )
                        check(recordingProof != null) {
                            "Voice Idea originals need owner review before capture"
                        }
                        val voices = ideas.filter {
                            it.captureType == IdeaCaptureType.VOICE
                        }.sortedBy { it.id }
                        check(recordingProof.records == voices.size) {
                            "Voice Idea original inventory changed during capture"
                        }
                        voices.forEachIndexed { index, voice ->
                            freezer.addOwnedFile(
                                "creator/voice-" + index.toString().padStart(4, '0'),
                                File(voice.audioLocalPath), 1,
                            )
                        }

                        // Detect unrelated producers that bypass a lock but
                        // change primary Creator or Personal serialization
                        // before all original files have been frozen.
                        check(tasks.exportJsonForTrustReadOnly() == projectsJson &&
                            ideaStore.exportJson() == ideasJson &&
                            PersonalBackupTransfer(app).exportForCloud() == portableReminders
                        ) {
                            "Owner records changed during the immutable source capture"
                        }
                        check(CreatorDataGate.generation(app) == generation) {
                            "Creator restore changed the owner source generation"
                        }
                    }
                }
                // The locks are now released, but every referenced byte was
                // separately frozen and independently hashed before release.
                // Do NOT equate this with full cross-store completeness.
                freezer.sealPartial()
            } catch (failure: Throwable) {
                try { freezer.close() }
                catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
                throw failure
            }
        }
}
