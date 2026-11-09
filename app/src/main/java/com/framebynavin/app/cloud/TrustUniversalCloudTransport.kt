package com.framebynavin.app.cloud

import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FC13 transport for the separately staged Trust owner-only Supabase protocol.
 *
 * Calls are NEVER invoked by legacy Sync Now or by any UI until a qualified
 * complete owner archive exists and the review-only server schema is deployed.
 * No service_role key; each request uses the fresh Supabase Auth JWT supplied
 * by CloudSyncManager.withFreshSession. No public URLs, upserts or in-place
 * replacement. No owner data is modified by this class.
 */
internal class TrustUniversalCloudTransport {
    private val contract = TrustT1C4CloudRevisionPolicy
    private val bucket = contract.REVIEW_ONLY_BUCKET

    private fun owner(session: CloudSession): String {
        contract.validateOwner(session.userId)
        require(session.accessToken.isNotBlank()) { "Fresh authenticated session required" }
        return session.userId
    }

    /** Fetch actual remote head from RLS-protected owner row. */
    suspend fun head(session: CloudSession): TrustCloudRevisionHead? {
        val id = owner(session)
        val raw = call(session, "GET", "/rest/v1/trust_backup_heads_v1" +
            "?select=revision,object_path,ciphertext_sha256,ciphertext_bytes" +
            "&user_id=eq.$id&limit=1")
        val rows = JSONArray(raw)
        require(rows.length() <= 1) { "Ambiguous cloud revision owner" }
        if (rows.length() == 0) return null
        val row = rows.getJSONObject(0)
        return TrustCloudRevisionHead(
            revision = row.getLong("revision"),
            objectPath = row.getString("object_path"),
            encryptedSha256 = row.getString("ciphertext_sha256"),
            encryptedBytes = row.getLong("ciphertext_bytes"),
        ).also { contract.validateRemoteHead(it, id) }
    }

    /** The owner state is an independent DB fence. Null is not ACTIVE. */
    suspend fun phase(session: CloudSession): String? {
        val id = owner(session)
        val rows = JSONArray(call(session, "GET",
            "/rest/v1/trust_backup_owner_state_v1?select=phase" +
                "&user_id=eq.$id&limit=1"))
        require(rows.length() <= 1)
        return if (rows.length() == 0) null else rows.getJSONObject(0).getString("phase")
            .also { require(it in setOf("active", "deleting_backup")) }
    }

    suspend fun beginUpload(session: CloudSession) {
        owner(session)
        val result = JSONObject(call(session, "POST",
            "/rest/v1/rpc/trust_begin_upload_v1", "{}"))
        check(result.getString("status") == "ready") {
            "The authenticated backup owner is not ready for a new revision"
        }
    }

    /**
     * Storage API POST is immutable; x-upsert:false. Stream the actual
     * on-disk ciphertext rather than buffering potentially large owner files.
     * Never send an unencrypted source file or unrelated local filesystem path.
     */
    suspend fun uploadNewImmutable(
        session: CloudSession,
        descriptor: TrustCloudCiphertextDescriptor,
        encryptedFile: File,
    ) {
        contract.validate(descriptor, owner(session))
        check(encryptedFile.isFile && encryptedFile.canRead() &&
            encryptedFile.length() == descriptor.encryptedBytes &&
            encryptedFile.length() in 1..MAX_SINGLE_OBJECT) {
            "Encrypted local file must be a verified and size-bounded archive"
        }
        val actualHash = digest(encryptedFile.inputStream())
        check(actualHash == descriptor.encryptedSha256) {
            "Local encrypted object no longer matches the frozen owner archive"
        }
        call(session, "POST", "/storage/v1/object/$bucket/" + descriptor.objectPath,
            bodyFile = encryptedFile, contentType = "application/octet-stream",
            extraHeaders = mapOf("x-upsert" to "false"))
    }

    /**
     * The actual, authenticated downloaded ciphertext stream is hashed while
     * reading; neither HTTP 200 nor Storage metadata can forge readback proof.
     */
    suspend fun verifiedDownload(
        session: CloudSession, descriptor: TrustCloudCiphertextDescriptor,
    ): TrustCloudVerifiedReadback = withContext(Dispatchers.IO) {
        contract.validate(descriptor, owner(session))
        val conn = connection(session, "GET",
            "/storage/v1/object/authenticated/$bucket/" + descriptor.objectPath,
            contentType = null)
        try {
            expectSuccess(conn)
            check(conn.contentLengthLong <= 0L ||
                conn.contentLengthLong == descriptor.encryptedBytes) {
                "Remote encrypted length differs before readback"
            }
            TrustCloudVerifiedReadback.verify(conn.inputStream, descriptor)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Download an immutable encrypted revision into a NEW private file.
     * This never decrypts bytes, writes to a user-facing directory, or
     * replaces an existing preview. Only the exact expected owner ciphertext
     * is retained, after independent local SHA-256 readback.
     */
    suspend fun downloadVerifiedToPrivateFile(
        session: CloudSession,
        descriptor: TrustCloudCiphertextDescriptor,
        destination: File,
    ): TrustCloudVerifiedReadback = withContext(Dispatchers.IO) {
        val id = owner(session)
        contract.validate(descriptor, id)
        require(descriptor.encryptedBytes in 1..MAX_SINGLE_OBJECT) {
            "Owner cloud revision exceeds verified free-tier object size"
        }
        require(destination.parentFile?.isDirectory == true &&
            !destination.exists() &&
            !java.nio.file.Files.isSymbolicLink(destination.parentFile!!.toPath())) {
            "Cloud review requires an unused, private destination"
        }
        check(destination.createNewFile()) { "Encrypted owner review target already exists" }
        val conn = connection(
            session, "GET",
            "/storage/v1/object/authenticated/$bucket/" + descriptor.objectPath,
            contentType = null,
        )
        try {
            expectSuccess(conn)
            check(conn.contentLengthLong <= 0 ||
                conn.contentLengthLong == descriptor.encryptedBytes) {
                "Remote object length changed before downloading"
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            conn.inputStream.buffered().use { source ->
                destination.outputStream().buffered().use { sink ->
                    val buffer = ByteArray(32 * 1024)
                    var zeroReads = 0
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        if (count == 0) {
                            require(++zeroReads <= 16) {
                                "Cloud revision download made no progress"
                            }
                            continue
                        }
                        zeroReads = 0
                        check(copied <= descriptor.encryptedBytes - count) {
                            "Cloud revision has extra ciphertext bytes"
                        }
                        sink.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        copied += count
                    }
                }
            }
            check(copied == descriptor.encryptedBytes &&
                destination.length() == copied &&
                digest.digest().joinToString("") { "%02x".format(it) } ==
                    descriptor.encryptedSha256) {
                "Owner ciphertext did not match its authenticated revision"
            }
            // A second independent read proves bytes committed to the local file,
            // not only network bytes observed by the first digest.
            val verified = TrustCloudVerifiedReadback.verify(
                destination.inputStream().buffered(), descriptor,
            )
            verified
        } catch (failure: Throwable) {
            if (!destination.delete() && destination.exists()) {
                failure.addSuppressed(IllegalStateException(
                    "Failed to clean an unverified private cloud download",
                ))
            }
            throw failure
        } finally {
            conn.disconnect()
        }
    }

    /** Atomically claim after verified download, then require HEAD re-fetch. */
    suspend fun claimRevision(
        session: CloudSession,
        previous: TrustCloudRevisionHead?,
        proposed: TrustCloudCiphertextDescriptor,
        proof: TrustCloudVerifiedReadback,
    ): TrustCloudRevisionProgress {
        val id = owner(session)
        contract.validate(proposed, id)
        contract.validateRemoteHead(previous, id)
        check(proof.descriptor == proposed) { "Readback proof belongs to a different object" }
        val body = JSONObject()
            .put("p_expected_revision", previous?.revision ?: JSONObject.NULL)
            .put("p_expected_path", previous?.objectPath ?: JSONObject.NULL)
            .put("p_expected_sha", previous?.encryptedSha256 ?: JSONObject.NULL)
            .put("p_new_path", proposed.objectPath)
            .put("p_new_sha", proposed.encryptedSha256)
            .put("p_new_bytes", proposed.encryptedBytes)
        val result = JSONObject(call(session, "POST",
            "/rest/v1/rpc/trust_claim_backup_head_v1", body.toString()))
        check(result.getString("status") == "accepted") {
            "Cloud revision conflict; keep the prior verified copy and review"
        }
        val reread = checkNotNull(head(session)) {
            "A committed remote revision could not be read back"
        }
        return contract.acknowledgeNewHead(previous, reread, proposed, proof)
    }

    /**
     * Begin a destructive action only after the owner reviewed the exact
     * current head, and typed DELETE CLOUD BACKUP. This is NOT an account delete.
     */
    suspend fun fenceDeleteBackup(
        session: CloudSession,
        reviewedHead: TrustCloudRevisionHead?,
        exactConfirmation: String,
    ) {
        val id = owner(session)
        contract.validateRemoteHead(reviewedHead, id)
        require(exactConfirmation == "DELETE CLOUD BACKUP") {
            "Exact owner deletion confirmation required"
        }
        val payload = JSONObject()
            .put("p_expected_revision", reviewedHead?.revision ?: JSONObject.NULL)
            .put("p_expected_path", reviewedHead?.objectPath ?: JSONObject.NULL)
            .put("p_expected_sha", reviewedHead?.encryptedSha256 ?: JSONObject.NULL)
            .put("p_confirmation", exactConfirmation)
        val response = JSONObject(call(session, "POST",
            "/rest/v1/rpc/trust_begin_delete_backup_v1", payload.toString()))
        check(response.getString("status") == "deletion_fenced") {
            "Cloud backup changed; owner must review again"
        }
    }

    /**
     * List only the known immutable backup paths in this owner's folder.
     * An unknown filename or a folder sentinel blocks deletion, never widens
     * the target to a bucket or another owner.
     */
    suspend fun listOwnerRevisions(session: CloudSession): List<String> {
        val id = owner(session)
        val prefix = "$id/revisions/"
        val paths = mutableListOf<String>()
        var offset = 0
        do {
            val payload = JSONObject()
                .put("prefix", prefix)
                .put("limit", PAGE_SIZE)
                .put("offset", offset)
                .put("sortBy", JSONObject().put("column", "name").put("order", "asc"))
            val batch = JSONArray(call(session, "POST",
                "/storage/v1/object/list/$bucket", payload.toString()))
            for (i in 0 until batch.length()) {
                val row = batch.getJSONObject(i)
                val name = row.getString("name")
                // Storage list returns file basenames for a requested prefix.
                // Accept a full path only if it is exactly inside this prefix.
                val fullPath = if (name.startsWith(prefix)) name else prefix + name
                val descriptor = TrustCloudCiphertextDescriptor(
                    id, fullPath, "0".repeat(64), 1L)
                contract.validate(descriptor, id)
                check(fullPath !in paths) { "Repeated Storage object in owner listing" }
                paths.add(fullPath)
                check(paths.size <= MAX_OWNER_OBJECTS) {
                    "Too many owner revisions; manual support needed before deletion"
                }
            }
            offset += batch.length()
        } while (batch.length() == PAGE_SIZE)
        return paths
    }

    /** Delete exact file paths via Storage API, never SQL storage.objects. */
    suspend fun removeOwnerRevisionBatch(session: CloudSession, paths: List<String>) {
        val id = owner(session)
        require(paths.isNotEmpty() && paths.size <= PAGE_SIZE && paths.distinct().size == paths.size)
        check(phase(session) == "deleting_backup") {
            "Cloud backup write fence must be active before Storage deletion"
        }
        paths.forEach { path ->
            contract.validate(TrustCloudCiphertextDescriptor(
                id, path, "0".repeat(64), 1L), id)
        }
        val response = call(session, "DELETE", "/storage/v1/object/$bucket",
            JSONObject().put("prefixes", JSONArray(paths)).toString())
        // Do not assume a success response is proof that every remote byte is gone.
        check(response.isNotBlank()) { "Storage deletion response was unconfirmed" }
    }

    /** Storage empty, then server clears the head and history inside one txn. */
    suspend fun finishDeleteBackup(session: CloudSession) {
        check(phase(session) == "deleting_backup") {
            "Cloud backup deletion has no owner fence"
        }
        check(listOwnerRevisions(session).isEmpty()) {
            "Some encrypted owner backups remain; deletion is not complete"
        }
        val result = JSONObject(call(session, "POST",
            "/rest/v1/rpc/trust_finish_delete_backup_v1", "{}"))
        check(result.getString("status") == "deleted") {
            "Cloud backup deletion has not been confirmed"
        }
        check(head(session) == null && phase(session) == "active" &&
            listOwnerRevisions(session).isEmpty()) {
            "Deleted cloud backup failed final server readback"
        }
    }

    /**
     * Server calls are IO-bound. The ONLY routes allowed belong to the new
     * FC13 Trust protocol; legacy Creator/Personal endpoints cannot be reached.
     */
    private suspend fun call(
        session: CloudSession,
        method: String,
        route: String,
        body: String? = null,
        bodyFile: File? = null,
        contentType: String = "application/json",
        extraHeaders: Map<String,String> = emptyMap(),
    ): String = withContext(Dispatchers.IO) {
        check(body == null || bodyFile == null)
        val conn = connection(session, method, route, contentType, extraHeaders)
        try {
            if (body != null || bodyFile != null) {
                conn.doOutput = true
                if (bodyFile != null) {
                    conn.setFixedLengthStreamingMode(bodyFile.length())
                    bodyFile.inputStream().use { input ->
                        conn.outputStream.use { output -> input.copyTo(output, 32 * 1024) }
                    }
                } else {
                    val bytes = checkNotNull(body).toByteArray(Charsets.UTF_8)
                    conn.setFixedLengthStreamingMode(bytes.size)
                    conn.outputStream.use { it.write(bytes) }
                }
            }
            expectSuccess(conn)
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val text = reader.readText()
                require(text.length < 512_000) {
                    "Unexpectedly large cloud control response"
                }
                text
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun connection(
        session: CloudSession,
        method: String,
        route: String,
        contentType: String?,
        extraHeaders: Map<String,String> = emptyMap(),
    ): HttpURLConnection {
        require(route.startsWith("/rest/v1/trust_backup_heads_v1?") ||
            route.startsWith("/rest/v1/trust_backup_owner_state_v1?") ||
            route.startsWith("/rest/v1/rpc/trust_") ||
            route.startsWith("/storage/v1/object/$bucket") ||
            route.startsWith("/storage/v1/object/authenticated/$bucket")) {
            "Unapproved universal backup transport route"
        }
        return (URL(CloudConfig.SUPABASE_URL + route)
            .openConnection() as HttpURLConnection).apply {
            requestMethod = method
            useCaches = false
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Cache-Control", "no-store")
            setRequestProperty("apikey", CloudConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer " + session.accessToken)
            setRequestProperty("Accept", "*/*")
            if (contentType != null) setRequestProperty("Content-Type", contentType)
            extraHeaders.forEach { (key,value) -> setRequestProperty(key,value) }
        }
    }

    private fun expectSuccess(conn: HttpURLConnection) {
        if (conn.responseCode !in 200..299) {
            throw CloudHttpException(conn.responseCode,
                "Authenticated universal backup transport failed")
        }
    }

    private fun digest(input: InputStream): String {
        val hash = MessageDigest.getInstance("SHA-256")
        input.use { src ->
            val buf = ByteArray(32 * 1024)
            while (true) {
                val count = src.read(buf)
                if (count < 0) break
                if (count > 0) hash.update(buf, 0, count)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        // The staged bucket enforces a lower cap than the generic Trust codec.
        const val MAX_SINGLE_OBJECT = 50L * 1024 * 1024
        const val PAGE_SIZE = 100
        const val MAX_OWNER_OBJECTS = 1000
    }
}
