package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Fake filesDir only; never reads owner media, performs a restore or schedules alarms. */
@RunWith(AndroidJUnit4::class)
class V367UnifiedCreatorOriginalVoiceSafetyAndroidTest {
    private val device get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isolated(root: File): Context = object : ContextWrapper(device) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    @Test fun originalLocalVoiceMustExactlyMatchEncryptedOriginalArchive() {
        val root = File(device.filesDir, "v367-original-voice-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            val folder = File(root, "voice_ideas")
            assertTrue(folder.mkdir())
            val recording = File(folder, "voice_idea_" + UUID.randomUUID() + ".m4a")
            val bytes = "only-synthetic-original-voice".toByteArray()
            recording.writeBytes(bytes)

            val expected = mapOf("idea-one" to V366VoiceBytes(
                bytes.size.toLong(), sha(bytes),
            ))
            val json = JSONArray().put(JSONObject()
                .put("id", "idea-one")
                .put("audioLocalPath", recording.absolutePath))
                .put(JSONObject().put("id", "idea-two")
                    .put("audioLocalPath", "")).toString()
            val reader = V367UnifiedCreatorOriginalVoiceSafety(context)
            val verified = reader.inspectOriginalIdeaReferencesReadOnly(
                json, expected, folder,
            )
            assertEquals(2, verified.ideaCount)
            assertEquals(1, verified.verifiedOriginalVoiceCount)
            assertFalse(verified.mayRollbackCreator)
            assertFalse(verified.mayDeleteMedia)
            assertTrue(recording.isFile)

            recording.writeText("same-original-path-but-different-audio")
            assertTrue(runCatching {
                reader.inspectOriginalIdeaReferencesReadOnly(json, expected, folder)
            }.isFailure)
            assertTrue(recording.isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun missingExtraOrSwappedVoiceReferencesAreRejected() {
        val root = File(device.filesDir, "v367-original-voice-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            val folder = File(root, "voice_ideas")
            assertTrue(folder.mkdir())
            val recording = File(folder, "voice_idea_" + UUID.randomUUID() + ".m4a")
            recording.writeText("synthetic")
            val expected = mapOf("a" to V366VoiceBytes(
                recording.length(), sha(recording.readBytes()),
            ))
            val reader = V367UnifiedCreatorOriginalVoiceSafety(context)
            val swapped = JSONArray().put(JSONObject()
                .put("id", "b").put("audioLocalPath", recording.absolutePath))
                .toString()
            assertTrue(runCatching {
                reader.inspectOriginalIdeaReferencesReadOnly(swapped, expected, folder)
            }.isFailure)

            val missing = JSONArray().put(JSONObject()
                .put("id", "a").put("audioLocalPath", "")).toString()
            assertTrue(runCatching {
                reader.inspectOriginalIdeaReferencesReadOnly(missing, expected, folder)
            }.isFailure)

            val repeated = JSONArray()
                .put(JSONObject().put("id", "a")
                    .put("audioLocalPath", recording.absolutePath))
                .put(JSONObject().put("id", "a")
                    .put("audioLocalPath", recording.absolutePath))
                .toString()
            assertTrue(runCatching {
                reader.inspectOriginalIdeaReferencesReadOnly(repeated, expected, folder)
            }.isFailure)
        } finally {
            root.deleteRecursively()
        }
    }
}
