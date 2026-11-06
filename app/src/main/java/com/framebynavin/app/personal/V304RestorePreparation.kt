package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * V304 creates a VERIFIED PRIVATE PREPARATION ONLY.
 *
 * Never touches active Personal DataStore, Room, live photos, scheduling, or
 * cloud. A future commit must re-open the archive, recheck current revisions,
 * independently secure rollback and obtain explicit owner approval.
 */
internal data class V304RestorePlan(
    val baselineDigest: String,
    val archiveDigest: String,
    val report: V301RestoreDryRunReport,
    /** In-memory authenticated photo manifest; never persisted in cleartext. */
    val expectedPhotoMetadata: List<V290SpacePhotoEntity>,
) {
    val canWriteLiveOwnerData: Boolean get() = false
}

internal object V304RestorePlanPolicy {
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun propose(
        local: V301LocalRecoverySnapshot,
        incoming: V300PersonalComponents,
        incomingPhotos: List<V290SpacePhotoEntity>,
    ): V304RestorePlan {
        val report = V301RestoreDryRun.compare(local, incoming, incomingPhotos)
        require(local.localIntegrityVerified && !report.requiresConflictReview &&
            report.conflicts == 0) {
            "Recovery preparation blocked: local integrity or ID conflicts require review"
        }
        return V304RestorePlan(
            baselineDigest = fingerprint(local.components, local.photos),
            archiveDigest = fingerprint(incoming, incomingPhotos),
            report = report,
            expectedPhotoMetadata = incomingPhotos.sortedBy { it.id },
        )
    }

    fun fingerprint(
        data: V300PersonalComponents,
        photos: List<V290SpacePhotoEntity>,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun next(raw: String) {
            val b = raw.toByteArray(Charsets.UTF_8)
            digest.update(byteArrayOf(
                (b.size ushr 24).toByte(),
                (b.size ushr 16).toByte(),
                (b.size ushr 8).toByte(),
                b.size.toByte(),
            ))
            digest.update(b)
        }
        next("backlot.v304.snapshot.v1")
        next(data.personal)
        next(data.challenges)
        next(data.groups)
        next(data.spaces)
        photos.sortedBy { it.id }.forEach { p ->
            next(p.id); next(p.spaceId); next(p.fileName); next(p.sha256Hex)
            next(p.takenAtMillis.toString()); next(p.addedAtMillis.toString())
            next(p.widthPx.toString()); next(p.heightPx.toString()); next(p.note)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun checkStillSame(
        plan: V304RestorePlan,
        local: V301LocalRecoverySnapshot,
    ) {
        require(local.localIntegrityVerified &&
            SHA256.matches(plan.baselineDigest) &&
            SHA256.matches(plan.archiveDigest) &&
            fingerprint(local.components, local.photos) == plan.baselineDigest) {
            "Phone data changed since preview; start recovery preparation again"
        }
    }
}

internal data class V304PreparationInventory(
    val ready: Int,
    val interrupted: Int,
    val unrecognized: Int,
) {
    init {
        require(ready >= 0 && interrupted >= 0 && unrecognized >= 0)
    }
    val needsReview: Boolean get() = interrupted > 0 || unrecognized > 0
}

internal data class V304PreparedTransfer(
    val transactionId: String,
    val photoCount: Int,
    val baselineDigest: String,
    val archiveDigest: String,
    val state: String,
) {
    val canCommitToLiveStorage: Boolean get() = false
}

/**
 * Isolated photo *copying* with journal checkpoints.
 *
 * Everything lives under the caller-supplied app-private preparation root,
 * never under the existing live `backlot-personal-space-photos` directory.
 * Crashes can leave incomplete private preparation folders; those folders
 * are not applied, must not be silently auto-installed or called a backup.
 */
internal class V304RestorePreparationStore(private val privateRoot: File) {
    private val UUID_CANONICAL = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val HEX64 = Regex("[0-9a-f]{64}")
    private val FORMAT = "backlot.personal.restore-preparation"
    private val MAX_PHOTOS = 500
    private val MAX_PHOTO_BYTES = 12L * 1024L * 1024L
    private val MAX_TOTAL = 500L * 1024L * 1024L

    /**
     * The caller must only pass a full authenticated V300 stage. A false
     * requestedByOwner argument must never allocate or copy private bytes.
     */
    fun prepare(
        plan: V304RestorePlan,
        photos: List<V300VerifiedPhoto>,
        requestedByOwner: Boolean,
    ): V304PreparedTransfer {
        require(requestedByOwner) { "Owner has not requested preparation" }
        require(plan.report.conflicts == 0 &&
            !plan.report.requiresConflictReview &&
            !plan.canWriteLiveOwnerData &&
            HEX64.matches(plan.baselineDigest) &&
            HEX64.matches(plan.archiveDigest)) {
            "Cannot prepare unverified or conflicted archive"
        }
        require(photos.size == plan.report.photos.proposed && photos.size <= MAX_PHOTOS) {
            "Photo count differs from verified archive"
        }
        require(photos.map { it.photo.id }.distinct().size == photos.size) {
            "Repeated private photo identity"
        }
        photos.forEach { V290PhotoTimelinePolicy.validate(it.photo) }
        require(photos.map { it.photo }.sortedBy { it.id } == plan.expectedPhotoMetadata) {
            "Prepared JPEG metadata does not match the authenticated archive plan"
        }
        var combined = 0L
        photos.forEach {
            require(it.sizeBytes in 1L..MAX_PHOTO_BYTES) {
                "Private photo exceeds preparation limit"
            }
            combined += it.sizeBytes
            require(combined <= MAX_TOTAL) { "Private photo preparation exceeds limit" }
        }
        ensurePrivateRoot()
        val id = UUID.randomUUID().toString()
        val dir = File(privateRoot, id)
        check(dir.mkdir()) { "Unable to reserve private preparation" }
        try {
            writeJournal(dir, id, plan, "PREPARING", emptyList())
            val ordered = photos.sortedBy { it.photo.id }
            val records = ordered.mapIndexed { index, photo ->
                val name = "part-${index.toString().padStart(5, '0')}.jpg"
                val target = File(dir, name)
                val pending = File(dir, "$name.partial")
                val digest = MessageDigest.getInstance("SHA-256")
                check(pending.createNewFile()) { "Preparation file collision" }
                var total = 0L
                try {
                    photo.stagedFile.inputStream().use { input ->
                        FileOutputStream(pending).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                if (n == 0) continue
                                total += n
                                require(total <= MAX_PHOTO_BYTES) {
                                    "Photo changed or grew while preparing"
                                }
                                digest.update(buffer, 0, n)
                                output.write(buffer, 0, n)
                            }
                            output.fd.sync()
                        }
                    }
                    val checksum = digest.digest().joinToString("") {
                        "%02x".format(it)
                    }
                    require(total == photo.sizeBytes &&
                        checksum == photo.photo.sha256Hex) {
                        "Photo changed after archive authentication"
                    }
                    check(pending.renameTo(target)) {
                        "Could not finalize private preparation copy"
                    }
                    Part(name, photo.sizeBytes, checksum)
                } finally {
                    pending.delete()
                }
            }
            writeJournal(dir, id, plan, "READY", records)
            return V304PreparedTransfer(
                id, records.size, plan.baselineDigest, plan.archiveDigest, "READY",
            )
        } catch (failure: Throwable) {
            deleteOwnDirectory(dir)
            throw failure
        }
    }

    /**
     * Inspect all files and hash every photo AGAIN. A PREPARING transaction or
     * an altered/extra file cannot be reported READY, even after process death.
     */
    fun inspect(transactionId: String): V304PreparedTransfer {
        val dir = ownDirectory(transactionId)
        check(dir.isDirectory) { "Unknown private restore preparation" }
        val envelopeFile = File(dir, "journal.json")
        require(envelopeFile.isFile && envelopeFile.length() in 1L..(256 * 1024L)) {
            "Preparation journal is missing or oversized"
        }
        val envelope = JSONObject(envelopeFile.readText(Charsets.UTF_8))
        val payload = envelope.getString("payload")
        require(envelope.getString("sha256") == sha256(payload)) {
            "Preparation journal integrity check failed"
        }
        val json = JSONObject(payload)
        require(json.getString("format") == FORMAT &&
            json.getInt("version") == 1 &&
            json.getString("transactionId") == transactionId) {
            "Unsupported or mismatched preparation"
        }
        val state = json.getString("state")
        require(state == "READY") {
            "Preparation was interrupted; it has not been committed"
        }
        val baseline = json.getString("baselineDigest")
        val archive = json.getString("archiveDigest")
        require(HEX64.matches(baseline) && HEX64.matches(archive)) {
            "Invalid preparation fingerprints"
        }
        val array = json.getJSONArray("parts")
        require(array.length() <= MAX_PHOTOS) { "Too many prepared photos" }
        val expected = HashSet<String>()
        var total = 0L
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val name = "part-${i.toString().padStart(5, '0')}.jpg"
            require(item.getString("name") == name && expected.add(name)) {
                "Unsafe preparation file reference"
            }
            val bytes = item.getLong("size")
            val sha = item.getString("sha256")
            require(bytes in 1L..MAX_PHOTO_BYTES && HEX64.matches(sha)) {
                "Invalid prepared file size or integrity"
            }
            total += bytes
            require(total <= MAX_TOTAL) { "Prepared photos exceed limit" }
            val file = File(dir, name)
            require(file.isFile && file.length() == bytes && streamedSha256(file) == sha) {
                "Prepared private photo failed integrity verification"
            }
        }
        val actual = dir.listFiles().orEmpty().map { it.name }.toSet()
        require(actual == expected + "journal.json") {
            "Untracked or incomplete private preparation files"
        }
        return V304PreparedTransfer(transactionId, array.length(), baseline, archive, "READY")
    }

    /**
     * Read-only owner recovery inventory. Interrupted/corrupt folders are
     * counted but NEVER deleted or installed automatically after a crash.
     * Names, photo notes and sensitive file paths are never returned.
     */
    fun inspectInventory(): V304PreparationInventory {
        if (!privateRoot.exists()) return V304PreparationInventory(0, 0, 0)
        require(privateRoot.isDirectory &&
            !Files.isSymbolicLink(privateRoot.toPath())) {
            "Private recovery preparation root is unsafe"
        }
        var ready = 0
        var interrupted = 0
        var unknown = 0
        val folders = checkNotNull(privateRoot.listFiles()) {
            "Could not inspect private preparation folder"
        }
        folders.forEach { child ->
            if (child.isDirectory &&
                !Files.isSymbolicLink(child.toPath()) &&
                UUID_CANONICAL.matches(child.name)) {
                try {
                    inspect(child.name)
                    ready++
                } catch (_: Exception) {
                    interrupted++
                }
            } else {
                unknown++
            }
        }
        return V304PreparationInventory(ready, interrupted, unknown)
    }

    /** Owner-invoked disposal, never touches any live photos or DataStore. */
    fun discard(transactionId: String) {
        val dir = ownDirectory(transactionId)
        if (dir.exists()) deleteOwnDirectory(dir)
    }

    private data class Part(val name: String, val size: Long, val sha256: String)

    private fun writeJournal(
        dir: File, id: String, plan: V304RestorePlan,
        state: String, photos: List<Part>,
    ) {
        val payload = JSONObject()
            .put("format", FORMAT).put("version", 1)
            .put("transactionId", id)
            .put("state", state)
            .put("baselineDigest", plan.baselineDigest)
            .put("archiveDigest", plan.archiveDigest)
            .put("parts", JSONArray().apply {
                photos.forEach { p ->
                    put(JSONObject().put("name", p.name)
                        .put("size", p.size).put("sha256", p.sha256))
                }
            }).toString()
        val bytes = JSONObject()
            .put("payload", payload)
            .put("sha256", sha256(payload))
            .toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 256 * 1024) { "Preparation journal too large" }
        val pending = File(dir, "journal.pending")
        FileOutputStream(pending).use {
            it.write(bytes)
            it.fd.sync()
        }
        val saved = File(dir, "journal.json")
        check(pending.renameTo(saved)) { "Could not record preparation checkpoint" }
    }

    private fun ensurePrivateRoot() {
        check(privateRoot.isDirectory || privateRoot.mkdirs()) {
            "Private preparation folder unavailable"
        }
        require(!Files.isSymbolicLink(privateRoot.toPath())) {
            "Preparation root cannot be a symbolic link"
        }
    }

    private fun ownDirectory(id: String): File {
        require(UUID_CANONICAL.matches(id) &&
            UUID.fromString(id).toString() == id) {
            "Invalid private preparation ID"
        }
        ensurePrivateRoot()
        val directory = File(privateRoot, id)
        require(directory.parentFile.canonicalFile == privateRoot.canonicalFile &&
            !Files.isSymbolicLink(directory.toPath())) {
            "Unsafe private preparation path"
        }
        return directory
    }

    private fun deleteOwnDirectory(dir: File) {
        require(dir.parentFile.canonicalFile == privateRoot.canonicalFile) {
            "Refusing to remove files outside the private preparation root"
        }
        if (!dir.exists()) return
        Files.walkFileTree(dir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(
                file: Path, attrs: BasicFileAttributes,
            ): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }
            override fun postVisitDirectory(dir: Path, error: java.io.IOException?): FileVisitResult {
                if (error != null) throw error
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun streamedSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
