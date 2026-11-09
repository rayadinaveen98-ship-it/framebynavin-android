package com.framebynavin.app.cloud

import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable, content-free catalog of encrypted owner copies.
 *
 * The only portable artifact is the AES-GCM ciphertext. Catalog rows contain
 * a UUID, owner UUID, SHA-256, byte length, source count and covered-key names;
 * no project titles, journal notes, images, reminders or passphrase.
 * Catalog metadata alone is never sufficient to authorize Restore or Upload.
 */
internal object TrustOwnerCopyCatalog {
    private val uuid = Regex(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
    )
    private val sha = Regex("[0-9a-f]{64}")
    private const val FORMAT = "backlot-trust-encrypted-owner-copy"
    private const val VERSION = 1
    private const val MAX_COPIES = 3

    internal data class Inventory(
        val receipts: List<TrustOwnerEncryptedLocalCopy.Receipt>,
        val orphanedEncryptedFiles: Int,
    )

    fun persist(
        root: File,
        receipt: TrustOwnerEncryptedLocalCopy.Receipt,
    ) {
        validate(receipt)
        val directory = directory(root)
        val target = File(directory, "copy-${receipt.copyId}.json")
        check(!target.exists()) { "Owner copy metadata would overwrite a saved receipt" }
        val data = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("owner", receipt.ownerId)
            .put("id", receipt.copyId)
            .put("size", receipt.encryptedBytes)
            .put("sha256", receipt.encryptedSha256)
            .put("sources", receipt.verifiedSources)
            .put("records", receipt.capturedRecords)
            .put("created", receipt.createdAtMillis)
            .put("uncovered", JSONArray(receipt.uncoveredSavedKeys))
            .toString().toByteArray(Charsets.UTF_8)
        require(data.size <= 512 * 1024) { "Encrypted copy catalog record is oversized" }
        val atomic = AtomicFile(target)
        var file: FileOutputStream? = null
        try {
            val output = atomic.startWrite()
            file = output
            output.write(data)
            atomic.finishWrite(output)
            file = null
        } catch (failure: Throwable) {
            file?.let { runCatching { atomic.failWrite(it) } }
            throw failure
        }
    }

    fun inspect(root: File, ownerId: String): Inventory {
        TrustT1C4CloudRevisionPolicy.validateOwner(ownerId)
        val metadata = directory(root)
        val copies = File(root, "verified")
        require(copies.isDirectory && !Files.isSymbolicLink(copies.toPath())) {
            "Encrypted owner copies are not accessible"
        }
        val listed = metadata.listFiles().orEmpty()
        check(listed.size <= 50) { "Unexpectedly large private backup catalog" }
        val receiptIds = HashSet<String>()
        val receipts = listed.map { file ->
            require(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
                file.name.matches(Regex("copy-$uuid[.]json")) &&
                file.length() in 1..(512L * 1024)) {
                "Unknown or invalid owner copy catalog entry"
            }
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val receipt = decode(json)
            check(receipt.copyId in file.name &&
                receiptIds.add(receipt.copyId)) {
                "Duplicate or mismatched owner encrypted copy identifier"
            }
            // Do not claim that an absent encrypted copy is a recoverable backup.
            val ciphertext = File(copies, "copy-${receipt.copyId}.enc")
            check(ciphertext.isFile &&
                !Files.isSymbolicLink(ciphertext.toPath()) &&
                ciphertext.length() == receipt.encryptedBytes) {
                "An encrypted owner copy is missing or changed"
            }
            receipt
        }.filter { it.ownerId == ownerId }
        val orphaned = copies.listFiles().orEmpty().count { ciphertext ->
            ciphertext.isFile && ciphertext.name.matches(Regex("copy-$uuid[.]enc")) &&
                ciphertext.name.removePrefix("copy-").removeSuffix(".enc") !in receiptIds
        }
        return Inventory(
            receipts.sortedByDescending { it.createdAtMillis },
            orphanedEncryptedFiles = orphaned,
        )
    }

    private fun decode(obj: JSONObject): TrustOwnerEncryptedLocalCopy.Receipt {
        require(obj.keys().asSequence().toSet() == setOf(
            "format", "version", "owner", "id", "size", "sha256",
            "sources", "records", "created", "uncovered",
        ))
        require(obj.getString("format") == FORMAT &&
            obj.getInt("version") == VERSION)
        val skipped = obj.getJSONArray("uncovered")
        require(skipped.length() <= 4096)
        val exclusions = (0 until skipped.length()).map {
            skipped.getString(it).also { label ->
                require(label.length <= 4096 && label.isNotBlank())
            }
        }
        return TrustOwnerEncryptedLocalCopy.Receipt(
            ownerId = obj.getString("owner"),
            copyId = obj.getString("id"),
            encryptedBytes = obj.getLong("size"),
            encryptedSha256 = obj.getString("sha256"),
            verifiedSources = obj.getInt("sources"),
            capturedRecords = obj.getInt("records"),
            createdAtMillis = obj.getLong("created"),
            uncoveredSavedKeys = exclusions,
        ).also(::validate)
    }

    private fun validate(receipt: TrustOwnerEncryptedLocalCopy.Receipt) {
        TrustT1C4CloudRevisionPolicy.validateOwner(receipt.ownerId)
        require(uuid.matches(receipt.copyId) &&
            receipt.encryptedSha256.matches(sha) &&
            receipt.encryptedBytes in 1..(1024L * 1024 * 1024) &&
            receipt.verifiedSources in 1..800 &&
            receipt.capturedRecords >= 0 &&
            receipt.createdAtMillis > 0L &&
            receipt.uncoveredSavedKeys.size <= 4096)
    }

    private fun directory(root: File): File {
        require(root.isDirectory && !Files.isSymbolicLink(root.toPath())) {
            "Private owner copy root unavailable"
        }
        val path = File(root, "receipts")
        check(path.isDirectory || path.mkdirs()) {
            "Private owner copy catalog cannot be prepared"
        }
        check(!Files.isSymbolicLink(path.toPath())) {
            "Owner copy catalog cannot be a symlink"
        }
        return path
    }
}
