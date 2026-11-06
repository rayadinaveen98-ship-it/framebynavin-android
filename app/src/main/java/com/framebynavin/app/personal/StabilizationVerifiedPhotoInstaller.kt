package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/**
 * Isolated prerequisite for the pending owner-approved full Personal restore.
 * No screen or cloud service calls this. A live caller MUST first authenticate
 * owner escrow, freeze all writers, and durably journal BEFORE_PHOTOS.
 */
internal enum class StabilizationPhotoInstallResult { INSTALLED, ALREADY_VERIFIED }

internal class StabilizationVerifiedPhotoInstaller(private val root: File) {
    private val maximumBytes = 12L * 1024L * 1024L

    @Synchronized
    fun installVerified(
        photo: V290SpacePhotoEntity, verifiedSource: File, expectedBytes: Long,
    ): StabilizationPhotoInstallResult {
        V290PhotoTimelinePolicy.validate(photo)
        require(expectedBytes in 1..maximumBytes) { "Invalid verified photo length" }
        require(Files.isRegularFile(
            verifiedSource.toPath(), LinkOption.NOFOLLOW_LINKS,
        )) { "Verified staged photo is missing" }
        require(verifiedSource.length() == expectedBytes &&
            hashFile(verifiedSource, expectedBytes) == photo.sha256Hex) {
            "Authenticated photo bytes changed before installation"
        }
        check(!root.exists() ||
            (root.isDirectory && !Files.isSymbolicLink(root.toPath()))) {
            "Private photo root is unsafe"
        }
        require(!verifiedSource.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())) {
            "Source cannot be inside permanent private photo storage"
        }
        val spaceFolder = File(root, photo.spaceId)
        check(!spaceFolder.exists() ||
            (spaceFolder.isDirectory && !Files.isSymbolicLink(spaceFolder.toPath()))) {
            "Private Space folder is unsafe"
        }
        val destination = File(spaceFolder, photo.fileName)
        val pending = File(spaceFolder, "." + photo.id + ".restore.pending")
        require(!Files.exists(pending.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Interrupted photo copy needs owner recovery review"
        }

        if (Files.exists(destination.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            check(Files.isRegularFile(destination.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                destination.length() == expectedBytes &&
                hashFile(destination, expectedBytes) == photo.sha256Hex) {
                "Existing private JPEG differs; refusing to overwrite it"
            }
            return StabilizationPhotoInstallResult.ALREADY_VERIFIED
        }
        check(spaceFolder.isDirectory || spaceFolder.mkdirs()) {
            "Unable to create private photo folder"
        }
        check(!Files.isSymbolicLink(spaceFolder.toPath()))
        check(pending.createNewFile()) { "Private recovery file already exists" }
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L
        verifiedSource.inputStream().use { input ->
            FileOutputStream(pending).use { output ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (n == 0) continue
                    copied += n
                    check(copied <= expectedBytes) { "Photo stage grew during copy" }
                    digest.update(buffer, 0, n)
                    output.write(buffer, 0, n)
                }
                output.fd.sync()
            }
        }
        check(copied == expectedBytes &&
            digest.digest().joinToString("") { "%02x".format(it) } == photo.sha256Hex) {
            "Private photo changed during write; owner review required"
        }
        // Android app-private filesystems can deny hard-link creation.
        // CREATE_NEW is an atomic O_EXCL-style reservation: unlike rename or
        // FileOutputStream(destination), it can NEVER replace an existing JPEG.
        // Copy verified .pending bytes into that new destination and fsync it.
        // A crash can leave an incomplete destination, but .pending remains
        // and forces explicit recovery review rather than claiming success.
        FileChannel.open(
            destination.toPath(),
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
            LinkOption.NOFOLLOW_LINKS,
        ).use { channel ->
            pending.inputStream().use { input ->
                val chunk = ByteArray(16 * 1024)
                while (true) {
                    val count = input.read(chunk)
                    if (count < 0) break
                    if (count == 0) continue
                    val buffer = ByteBuffer.wrap(chunk, 0, count)
                    while (buffer.hasRemaining()) {
                        check(channel.write(buffer) > 0) {
                            "Failed to complete private photo copy"
                        }
                    }
                }
            }
            channel.force(true)
        }
        syncFolder(spaceFolder)
        check(Files.isRegularFile(destination.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            destination.length() == expectedBytes &&
            hashFile(destination, expectedBytes) == photo.sha256Hex) {
            "Installed photo failed final verification; keep pending for review"
        }
        Files.delete(pending.toPath())
        syncFolder(spaceFolder)
        return StabilizationPhotoInstallResult.INSTALLED
    }

    private fun syncFolder(dir: File) {
        FileChannel.open(dir.toPath(), StandardOpenOption.READ).use { it.force(true) }
    }

    private fun hashFile(file: File, sizeLimit: Long): String {
        val sha = MessageDigest.getInstance("SHA-256")
        var read = 0L
        file.inputStream().use { stream ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                if (n == 0) continue
                read += n
                require(read <= sizeLimit) { "Photo exceeds verified byte limit" }
                sha.update(buffer, 0, n)
            }
        }
        return sha.digest().joinToString("") { "%02x".format(it) }
    }
}
