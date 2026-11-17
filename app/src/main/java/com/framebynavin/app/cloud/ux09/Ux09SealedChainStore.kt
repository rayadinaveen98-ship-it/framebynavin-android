package com.framebynavin.app.cloud.ux09

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * EXPERIMENTAL, DISCONNECTED UX09-02A-S storage. No production callers.
 *
 * A single account/domain/entity chain is sealed with AES-GCM using a key
 * supplied by the caller. Associated data binds the account, epochs, domain,
 * entity, and storage revision, so ciphertext cannot be moved to another owner.
 *
 * This does not make a DataStore transaction atomic with this journal, and it
 * does not claim Android power-failure safety. A real app-private directory,
 * backup exclusion, key recovery and cross-process locking need device tests.
 */
internal class Ux09SealedChainStore(
    private val root: File,
    private val keyForAccount: (String) -> SecretKey,
    private val random: SecureRandom = SecureRandom(),
) {
    internal data class Snapshot(val state: Ux09EntityChain, val storageRevision: Long)

    private val lock = Any()
    private val magic = "UX09GCM1".toByteArray(Charsets.US_ASCII)
    private val version: Byte = 1
    private val nonceBytes = 12
    private val maxBytes = 1024 * 1024
    private val maxPlaintext = 512 * 1024

    fun write(
        state: Ux09EntityChain,
        activeAccountId: String,
        activeAccountGeneration: Long,
        activeRestoreGeneration: Long,
        expectedStorageRevision: Long?,
    ): Snapshot = synchronized(lock) {
        assertIdentity(state, activeAccountId, activeAccountGeneration, activeRestoreGeneration)
        // Cross-instance/process protection is essential: two independent store
        // objects must not both observe the same expected storage revision.
        // The OS lock is held across the read/CAS decision AND atomic rename.
        withEntityFileLock(state.accountId, state.domain, state.entityId) {
        val plaintext = Ux09SealedChainJson.encode(state)
        require(plaintext.size <= maxPlaintext) {
            "Protected queue exceeds local prototype limit; nothing was discarded"
        }
        val target = file(state.accountId, state.domain, state.entityId)
        val old = if (target.exists()) read(
            state.accountId, activeAccountGeneration, activeRestoreGeneration,
            state.domain, state.entityId
        ) else null
        require(old?.storageRevision == expectedStorageRevision) {
            "Stale local queue write: original protected revision was preserved"
        }
        require(expectedStorageRevision == null ||
            expectedStorageRevision in 1..(Long.MAX_VALUE - 2)) {
            "Invalid local storage revision"
        }
        val nextRevision = (expectedStorageRevision ?: 0L) + 1L
        val bytes = encrypt(
            plaintext,
            keyForAccount(state.accountId),
            binding(state.accountId, state.domain, state.entityId,
                state.accountGeneration, state.restoreGeneration, nextRevision),
            nextRevision,
        )
        persistAtomically(target, bytes)
        Snapshot(state, nextRevision)
        }
    }

    fun read(
        activeAccountId: String,
        activeAccountGeneration: Long,
        activeRestoreGeneration: Long,
        domain: Ux09Domain,
        entityId: String,
    ): Snapshot? = synchronized(lock) {
        val target = file(activeAccountId, domain, entityId)
        if (!target.exists()) return@synchronized null
        ensureRegularFile(target)
        require(target.length() in (magic.size + 1 + 8 + nonceBytes + 4 + 16).toLong()..maxBytes.toLong()) {
            "Invalid protected queue envelope; original bytes must be retained"
        }
        val blob = target.readBytes()
        val buffer = ByteBuffer.wrap(blob).order(ByteOrder.BIG_ENDIAN)
        val marker = ByteArray(magic.size)
        buffer.get(marker)
        require(marker.contentEquals(magic) && buffer.get() == version) {
            "Unknown protected queue format; do not overwrite"
        }
        val revision = buffer.long
        require(revision in 1..(Long.MAX_VALUE - 1)) {
            "Invalid protected storage revision"
        }
        val nonce = ByteArray(nonceBytes)
        buffer.get(nonce)
        val length = buffer.int
        require(length in 16..maxBytes && length == buffer.remaining()) {
            "Invalid protected queue ciphertext length"
        }
        val ciphertext = ByteArray(length)
        buffer.get(ciphertext)
        val aad = binding(activeAccountId, domain, entityId,
            activeAccountGeneration, activeRestoreGeneration, revision)
        val plaintext = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keyForAccount(activeAccountId),
                GCMParameterSpec(128, nonce))
            cipher.updateAAD(aad)
            cipher.doFinal(ciphertext)
        } catch (error: java.security.GeneralSecurityException) {
            throw IllegalStateException(
                "Protected queue authentication failed; preserve the original for owner review",
                error,
            )
        }
        require(plaintext.size <= maxPlaintext) {
            "Oversized protected queue plaintext; retained original bytes"
        }
        val state = Ux09SealedChainJson.decode(plaintext)
        assertIdentity(state, activeAccountId, activeAccountGeneration, activeRestoreGeneration)
        require(state.domain == domain && state.entityId == entityId) {
            "Protected queue belongs to another entity; retain original"
        }
        Snapshot(state, revision)
    }

    private fun assertIdentity(
        state: Ux09EntityChain,
        accountId: String,
        accountGeneration: Long,
        restoreGeneration: Long,
    ) {
        Ux09Contract.requireUuid(accountId)
        Ux09Contract.requireUuid(state.accountId)
        require(state.accountId == accountId &&
            state.accountGeneration == accountGeneration &&
            state.restoreGeneration == restoreGeneration &&
            accountGeneration >= 0 && restoreGeneration >= 0) {
            "Account/restore generation changed; refuse protected queue access"
        }
    }

    private fun file(accountId: String, domain: Ux09Domain, entityId: String): File {
        Ux09Contract.requireUuid(accountId)
        Ux09Contract.requireUuid(entityId)
        require(!Files.isSymbolicLink(root.toPath())) {
            "Protected queue root must not be a symbolic link"
        }
        val directory = File(root, accountId)
        require(!Files.isSymbolicLink(directory.toPath())) {
            "Protected queue account directory must not be a symbolic link"
        }
        // Domain is an enum constant, not a caller-provided path fragment.
        return File(directory, domain.wire + "-" + entityId + ".sealed")
    }

    /**
     * Holds a durable file lock for the entire local optimistic-CAS update.
     * The .guard inode is deliberately never deleted (deleting would allow two
     * processes to lock different inodes under the same pathname).
     *
     * Do not silently treat lock contention as success. The caller may safely
     * retry after re-reading the committed storage revision.
     */
    private fun <T> withEntityFileLock(
        accountId: String,
        domain: Ux09Domain,
        entityId: String,
        action: () -> T,
    ): T {
        val target = file(accountId, domain, entityId)
        val dir = target.parentFile ?: error("Missing account-scoped protected directory")
        require(dir.isDirectory || dir.mkdirs()) {
            "Cannot establish account-scoped protected queue locking"
        }
        require(!Files.isSymbolicLink(dir.toPath()) &&
            Files.isDirectory(dir.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Protected account lock directory must not be a symlink"
        }
        val guard = File(dir, target.name + ".guard")
        if (Files.exists(guard.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            ensureRegularFile(guard)
        }
        FileChannel.open(guard.toPath(),
            StandardOpenOption.CREATE, StandardOpenOption.WRITE,
            LinkOption.NOFOLLOW_LINKS).use { channel ->
            val lease = try {
                channel.tryLock()
            } catch (_: java.nio.channels.OverlappingFileLockException) {
                throw IllegalStateException("Another offline writer holds the entity lock")
            }
            check(lease != null) {
                "Another offline writer holds the entity lock; original queue preserved"
            }
            lease.use {
                // The optimistic storage revision is re-read only AFTER the
                // cross-process lease is acquired. Never trust a pre-lock read.
                return action()
            }
        }
    }

    private fun ensureRegularFile(target: File) {
        require(Files.isRegularFile(target.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(target.toPath())) {
            "Protected queue file must be a regular file, without symlinks"
        }
    }

    private fun binding(
        accountId: String,
        domain: Ux09Domain,
        entityId: String,
        accountGeneration: Long,
        restoreGeneration: Long,
        storageRevision: Long,
    ): ByteArray = (
        "Backlot.UX09.SealedChain.v1|" + accountId + "|" + domain.wire + "|" +
            entityId + "|" + accountGeneration + "|" + restoreGeneration + "|" +
            storageRevision
    ).toByteArray(Charsets.UTF_8)

    private fun encrypt(
        plaintext: ByteArray,
        secret: SecretKey,
        aad: ByteArray,
        storageRevision: Long,
    ): ByteArray {
        val nonce = ByteArray(nonceBytes).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)
        require(ciphertext.size + magic.size + 1 + 8 + nonceBytes + 4 < maxBytes)
        return ByteBuffer.allocate(magic.size + 1 + 8 + nonceBytes + 4 + ciphertext.size)
            .order(ByteOrder.BIG_ENDIAN)
            .put(magic).put(version).putLong(storageRevision).put(nonce)
            .putInt(ciphertext.size).put(ciphertext).array()
    }

    private fun persistAtomically(target: File, content: ByteArray) {
        val parent = target.parentFile ?: error("Missing protected queue directory")
        require(parent.isDirectory || parent.mkdirs()) {
            "Cannot create protected queue directory; no queue was written"
        }
        require(!Files.isSymbolicLink(parent.toPath())) {
            "Account queue directory must not be a symlink"
        }
        if (target.exists()) ensureRegularFile(target)
        val tmp = File(parent, target.name + ".tmp")
        require(tmp.createNewFile()) {
            "Interrupted protected write present; preserve it for recovery"
        }
        try {
            FileOutputStream(tmp).use {
                it.write(content)
                it.fd.sync()
            }
            Files.move(tmp.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            FileChannel.open(parent.toPath(), StandardOpenOption.READ).use {
                it.force(true)
            }
        } catch (error: Exception) {
            // If a rename committed before directory-fsync failed, keep the
            // new target for read-back/revision inspection. Never delete it.
            if (tmp.exists()) tmp.delete()
            throw error
        }
    }
}

/** Exact-key, versioned serializer for synthetic offline state only. */
internal object Ux09SealedChainJson {
    private val digest = Regex("[a-f0-9]{64}")
    private const val MAX_PENDING = 100

    fun encode(state: Ux09EntityChain): ByteArray {
        validate(state)
        val json = JSONObject()
            .put("version", 1)
            .put("accountId", state.accountId)
            .put("accountGeneration", state.accountGeneration)
            .put("restoreGeneration", state.restoreGeneration)
            .put("domain", state.domain.wire)
            .put("entityId", state.entityId)
            .put("remoteRevision", state.remoteRevision)
            .put("lastAcknowledgedSequence", state.lastAcknowledgedSequence)
            .put("lastCommittedLocalSha256", state.lastCommittedLocalSha256)
            .put("serverConflict", state.serverConflict)
            .put("remoteDeleted", state.remoteDeleted)
        val pending = JSONArray()
        state.pendingEdits.forEach { edit ->
            pending.put(JSONObject()
                .put("localEditId", edit.localEditId)
                .put("localBeforeSha256", edit.localBeforeSha256)
                .put("localAfterSha256", edit.localAfterSha256)
                .put("canonicalPayloadJson", edit.canonicalPayloadJson)
                .put("tombstone", edit.tombstone))
        }
        json.put("pendingEdits", pending)
        json.put("preparedRemoteOperation", state.preparedRemoteOperation?.let { op ->
            JSONObject()
                .put("accountId", op.accountId)
                .put("accountGeneration", op.accountGeneration)
                .put("restoreGeneration", op.restoreGeneration)
                .put("domain", op.domain.wire)
                .put("entityId", op.entityId)
                .put("operationId", op.operationId)
                .put("expectedRevision", op.expectedRevision)
                .put("localBeforeSha256", op.localBeforeSha256)
                .put("localAfterSha256", op.localAfterSha256)
                .put("payloadJson", op.payloadJson)
                .put("tombstone", op.tombstone)
        } ?: JSONObject.NULL)
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): Ux09EntityChain {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        exactKeys(json, setOf(
            "version", "accountId", "accountGeneration", "restoreGeneration",
            "domain", "entityId", "remoteRevision", "lastAcknowledgedSequence",
            "lastCommittedLocalSha256", "serverConflict", "remoteDeleted",
            "pendingEdits", "preparedRemoteOperation"
        ))
        require(json.getInt("version") == 1) { "Unsupported protected queue schema" }
        fun domainOf(wire: String) =
            Ux09Domain.entries.singleOrNull { it.wire == wire }
                ?: throw IllegalArgumentException("Unknown protected entity domain")
        val edits = json.getJSONArray("pendingEdits")
        require(edits.length() <= MAX_PENDING) { "Too many pending local edits" }
        val items = (0 until edits.length()).map { index ->
            val item = edits.getJSONObject(index)
            exactKeys(item, setOf("localEditId", "localBeforeSha256",
                "localAfterSha256", "canonicalPayloadJson", "tombstone"))
            Ux09LocalEdit(item.getString("localEditId"),
                item.getString("localBeforeSha256"), item.getString("localAfterSha256"),
                item.getString("canonicalPayloadJson"), item.getBoolean("tombstone"))
        }
        val prepared = if (json.isNull("preparedRemoteOperation")) null else
            json.getJSONObject("preparedRemoteOperation").let { op ->
                exactKeys(op, setOf("accountId", "accountGeneration", "restoreGeneration",
                    "domain", "entityId", "operationId", "expectedRevision",
                    "localBeforeSha256", "localAfterSha256", "payloadJson", "tombstone"))
                Ux09Intent(op.getString("accountId"), op.getLong("accountGeneration"),
                    op.getLong("restoreGeneration"), domainOf(op.getString("domain")),
                    op.getString("entityId"), op.getString("operationId"),
                    op.getLong("expectedRevision"), op.getString("localBeforeSha256"),
                    op.getString("localAfterSha256"), op.getString("payloadJson"),
                    op.getBoolean("tombstone"))
            }
        val state = Ux09EntityChain(
            json.getString("accountId"), json.getLong("accountGeneration"),
            json.getLong("restoreGeneration"), domainOf(json.getString("domain")),
            json.getString("entityId"), json.getLong("remoteRevision"),
            json.getLong("lastAcknowledgedSequence"),
            json.getString("lastCommittedLocalSha256"), items, prepared,
            json.getBoolean("serverConflict"), json.getBoolean("remoteDeleted"),
        )
        validate(state)
        return state
    }

    private fun exactKeys(json: JSONObject, expected: Set<String>) {
        require(json.keys().asSequence().toSet() == expected) {
            "Unexpected/omitted protected chain field; original bytes retained"
        }
    }

    private fun validate(state: Ux09EntityChain) {
        Ux09Contract.requireUuid(state.accountId)
        Ux09Contract.requireUuid(state.entityId)
        require(state.accountGeneration >= 0 && state.restoreGeneration >= 0)
        require(state.remoteRevision in 0..(Long.MAX_VALUE - 2))
        require(state.lastAcknowledgedSequence >= 0)
        require(digest.matches(state.lastCommittedLocalSha256))
        require(state.pendingEdits.size <= MAX_PENDING)
        var previous = state.lastCommittedLocalSha256
        val localIds = mutableSetOf<String>()
        state.pendingEdits.forEachIndexed { index, edit ->
            Ux09Contract.requireUuid(edit.localEditId)
            require(localIds.add(edit.localEditId) &&
                digest.matches(edit.localBeforeSha256) &&
                digest.matches(edit.localAfterSha256) &&
                edit.localBeforeSha256 == previous &&
                edit.localAfterSha256 != previous) {
                "Broken protected queue hash/edit sequence"
            }
            require(edit.canonicalPayloadJson ==
                Ux09Contract.canonicalPayload(edit.canonicalPayloadJson)) {
                "Noncanonical protected payload"
            }
            if (edit.tombstone) require(index == state.pendingEdits.lastIndex &&
                edit.canonicalPayloadJson == "{}") {
                "Permanent tombstone must be the final queued edit"
            }
            previous = edit.localAfterSha256
        }
        val active = state.preparedRemoteOperation
        if (active != null) {
            Ux09Contract.validate(active)
            val first = state.pendingEdits.firstOrNull()
            requireNotNull(first) { "Prepared operation lost its edit" }
            require(active.accountId == state.accountId &&
                active.accountGeneration == state.accountGeneration &&
                active.restoreGeneration == state.restoreGeneration &&
                active.domain == state.domain && active.entityId == state.entityId &&
                active.expectedRevision == state.remoteRevision &&
                active.localBeforeSha256 == first.localBeforeSha256 &&
                active.localAfterSha256 == first.localAfterSha256 &&
                active.payloadJson == first.canonicalPayloadJson &&
                active.tombstone == first.tombstone) {
                "Prepared operation was mutated or changed owner"
            }
        }
        if (state.remoteDeleted) require(state.pendingEdits.isEmpty() &&
            state.preparedRemoteOperation == null) {
            "Tombstoned entity retains an invalid new write"
        }
    }
}
