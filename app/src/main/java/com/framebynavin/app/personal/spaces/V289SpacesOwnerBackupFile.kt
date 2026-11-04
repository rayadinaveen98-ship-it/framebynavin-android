package com.framebynavin.app.personal.spaces

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * User-mediated Storage Access Framework only. No automatic cloud upload, cache
 * export, extra storage permissions or access to other apps' media.
 *
 * The selected document provider may itself be cloud-backed; owner consent must
 * always precede choosing that destination. Notes in this format are plaintext.
 */
internal object V289SpacesOwnerBackupFile {
    const val MIME_TYPE = "application/json"
    const val DEFAULT_FILE_NAME = "backlot-personal-spaces-metadata.json"
    const val MAX_FILE_CHARS = 4_000_000

    fun readLimited(stream: InputStream): String {
        stream.reader(StandardCharsets.UTF_8).use { reader ->
            val buffer = CharArray(4096)
            val output = StringBuilder()
            while (true) {
                // Do not deserialize or hold a giant untrusted document in memory.
                val count = reader.read(buffer, 0, buffer.size)
                if (count < 0) break
                if (count == 0) continue
                require(output.length + count <= MAX_FILE_CHARS) {
                    "Spaces metadata backup exceeds the 4 million character limit"
                }
                output.append(buffer, 0, count)
            }
            require(output.isNotEmpty()) { "Selected Spaces backup file was empty" }
            return output.toString()
        }
    }

    suspend fun readFromSelectedFile(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("Unable to read the selected backup document")
            stream.use { readLimited(it) }
        }

    suspend fun writeToSelectedFile(context: Context, uri: Uri, raw: String) {
        // Never write malformed or unverified data into a backup document.
        V289SpacesMetadataCodec.decode(raw)
        require(raw.length <= MAX_FILE_CHARS) { "Spaces export is too large" }
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Unable to create the selected backup document")
            stream.writer(StandardCharsets.UTF_8).use { writer ->
                writer.write(raw)
                writer.flush()
            }
        }
    }
}
