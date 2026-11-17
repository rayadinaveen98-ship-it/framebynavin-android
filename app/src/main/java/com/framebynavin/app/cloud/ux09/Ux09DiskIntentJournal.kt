package com.framebynavin.app.cloud.ux09

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * UX09-02A disconnected write-ahead intent JOURNAL PROTOTYPE.
 *
 * Protocol for later, separately authorized canonical store integration:
 * 1. PREPARED intent is fsynced and atomically renamed BEFORE the DataStore edit.
 * 2. Save local entity; verify its full canonical hash.
 * 3. Mark READY. On crash before step 3, recovery checks stored data hash.
 *
 * There is no production caller. This does not give cross-DataStore atomicity.
 * The at-rest JSON is unencrypted: DO NOT store real user content here until
 * app-private storage/encryption and operational safety receive separate approval.
 * Errors in atomic move/directory fsync fail closed; no fallback.
 */
internal class Ux09DiskIntentJournal(private val root: File) {
    private val lock = Any()
    private val header = "BACKLOT-UX09-02A-JOURNAL-1"

    internal enum class Phase { PREPARED, READY }
    internal enum class Recovery {
        NOT_SAVED, READY_TO_RETRY, NEEDS_MANUAL_RECONCILIATION, WRONG_ACCOUNT_OR_RESTORE,
    }
    internal data class Stored(val intent: Ux09Intent, val phase: Phase)

    fun prepare(intent: Ux09Intent): Stored = synchronized(lock) {
        Ux09Contract.validate(intent)
        require(intent.localBeforeSha256 != intent.localAfterSha256) {
            "No-op mutations do not need an upload journal entry"
        }
        val target = path(intent.accountId, intent.operationId)
        if (target.exists()) {
            val previous = readFile(target)
            require(previous.intent == intent) { "Operation UUID was reused with different intent" }
            return@synchronized previous
        }
        val value = Stored(intent, Phase.PREPARED)
        persist(target, value)
        value
    }

    fun markLocallySaved(intent: Ux09Intent, persistedFullEntityHash: String): Stored =
        synchronized(lock) {
            require(persistedFullEntityHash == intent.localAfterSha256) {
                "Canonical local save is not proven; do NOT enqueue sync"
            }
            val target = path(intent.accountId, intent.operationId)
            val stored = readFile(target)
            require(stored.intent == intent) { "Journal changed during local save" }
            if (stored.phase == Phase.READY) return@synchronized stored
            val ready = stored.copy(phase = Phase.READY)
            persist(target, ready)
            ready
        }

    fun read(accountId: String, operationId: String): Stored = synchronized(lock) {
        readFile(path(accountId, operationId))
    }

    fun recover(
        stored: Stored,
        observedFullEntityHash: String,
        currentAccountId: String,
        currentAccountGeneration: Long,
        currentRestoreGeneration: Long,
    ): Recovery {
        val intent = stored.intent
        if (currentAccountId != intent.accountId ||
            currentAccountGeneration != intent.accountGeneration ||
            currentRestoreGeneration != intent.restoreGeneration
        ) return Recovery.WRONG_ACCOUNT_OR_RESTORE
        if (observedFullEntityHash == intent.localAfterSha256) return Recovery.READY_TO_RETRY
        if (stored.phase == Phase.PREPARED &&
            observedFullEntityHash == intent.localBeforeSha256
        ) return Recovery.NOT_SAVED
        return Recovery.NEEDS_MANUAL_RECONCILIATION
    }

    fun pending(accountId: String): List<Stored> = synchronized(lock) {
        val dir = directory(accountId)
        if (!dir.exists()) return@synchronized emptyList()
        require(dir.isDirectory) { "Sync journal account path is not a directory" }
        val files = dir.listFiles() ?: error("Could not inspect sync journal")
        require(files.none { it.name.endsWith(".tmp") }) {
            "Unfinished journal temp file retained for manual recovery"
        }
        require(files.all { it.isFile && it.name.endsWith(".intent") }) {
            "Unexpected journal file: stop rather than deleting or ignoring"
        }
        files.sortedBy { it.name }.map(::readFile)
    }

    private fun directory(accountId: String): File {
        Ux09Contract.requireUuid(accountId)
        return File(root, accountId)
    }

    private fun path(accountId: String, operationId: String): File {
        Ux09Contract.requireUuid(operationId)
        return File(directory(accountId), "$operationId.intent")
    }

    private fun persist(target: File, stored: Stored) {
        val parent = target.parentFile ?: error("Journal directory required")
        require(parent.isDirectory || parent.mkdirs()) { "Could not create app-private account journal" }
        val temp = File(parent, target.name + ".tmp")
        require(temp.createNewFile()) {
            "Previous unfinished journal temp exists; manual recovery required"
        }
        try {
            val i = stored.intent
            val body = JSONObject()
                .put("version", 1)
                .put("phase", stored.phase.name)
                .put("account_id", i.accountId)
                .put("account_generation", i.accountGeneration)
                .put("restore_generation", i.restoreGeneration)
                .put("domain", i.domain.wire)
                .put("entity_id", i.entityId)
                .put("operation_id", i.operationId)
                .put("expected_revision", i.expectedRevision)
                .put("before_sha256", i.localBeforeSha256)
                .put("after_sha256", i.localAfterSha256)
                .put("payload", i.payloadJson)
                .put("tombstone", i.tombstone).toString()
            val payload = body.toByteArray(Charsets.UTF_8)
            val content = header + "\n" + Ux09Contract.sha256(payload) + "\n" + body
            FileOutputStream(temp).use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.fd.sync()
            }
            Files.move(temp.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            FileChannel.open(parent.toPath(), StandardOpenOption.READ).use { it.force(true) }
        } catch (failure: Exception) {
            // If rename happened before an error, leave committed file untouched.
            if (temp.exists()) temp.delete()
            throw failure
        }
    }

    private fun readFile(file: File): Stored {
        require(file.isFile && file.length() in 1..100_000) {
            "Missing or invalid sync journal; do not alter local data"
        }
        val raw = file.readText(Charsets.UTF_8)
        val a = raw.indexOf('\n')
        val b = raw.indexOf('\n', a + 1)
        require(a >= 0 && b > a) { "Damaged journal header" }
        require(raw.substring(0, a) == header) { "Unknown journal schema" }
        val body = raw.substring(b + 1)
        require(Ux09Contract.sha256(body.toByteArray(Charsets.UTF_8)) ==
            raw.substring(a + 1, b)) { "Journal checksum mismatch; operation retained" }
        val json = JSONObject(body)
        require(json.getInt("version") == 1) { "Unsupported journal version" }
        val domain = Ux09Domain.entries.singleOrNull { it.wire == json.getString("domain") }
            ?: error("Unsupported journal domain")
        val intent = Ux09Intent(
            json.getString("account_id"), json.getLong("account_generation"),
            json.getLong("restore_generation"), domain, json.getString("entity_id"),
            json.getString("operation_id"), json.getLong("expected_revision"),
            json.getString("before_sha256"), json.getString("after_sha256"),
            json.getString("payload"), json.getBoolean("tombstone"),
        )
        Ux09Contract.validate(intent)
        require(file == path(intent.accountId, intent.operationId)) {
            "Journal account/path mismatch"
        }
        return Stored(intent, Phase.valueOf(json.getString("phase")))
    }
}
