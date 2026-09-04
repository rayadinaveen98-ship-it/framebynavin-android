package com.framebynavin.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class CreatorCopilotTool(val label: String, val shortLabel: String) {
    IDEA_TO_PLAN("Idea → Project Plan", "IDEA PLAN"),
    OUTLINE("Script Outline", "OUTLINE"),
    HOOKS("Hooks & Openings", "HOOKS"),
    REWRITE("Rewrite / Tighten", "REWRITE"),
    TITLE_PROMO("Titles · Thumbnail · Promo", "PACKAGING"),
}

data class CreatorCopilotConfig(
    val model: String = CreatorCopilotConfigStore.DEFAULT_MODEL,
)

class CreatorCopilotConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("creator_copilot_v18", Context.MODE_PRIVATE)

    fun snapshot(): CreatorCopilotConfig = CreatorCopilotConfig(
        model = prefs.getString(KEY_MODEL, DEFAULT_MODEL)?.trim().orEmpty().ifBlank { DEFAULT_MODEL },
    )

    fun setModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model.trim().ifBlank { DEFAULT_MODEL }).apply()
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-5.6-luna"
        private const val KEY_MODEL = "model"
    }
}

/**
 * Personal BYOK secret storage. The API key never enters TaskStore/backup JSON and is encrypted
 * with an AES/GCM key owned by Android Keystore before it is written to SharedPreferences.
 */
class CreatorCopilotSecretStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("creator_copilot_secret_v18", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = loadKey().isNotBlank()

    fun saveKey(raw: String) {
        val clean = raw.trim()
        require(clean.isNotBlank()) { "API key cannot be empty." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(clean.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteArray(cipher.iv.size + encrypted.size)
        System.arraycopy(cipher.iv, 0, packed, 0, cipher.iv.size)
        System.arraycopy(encrypted, 0, packed, cipher.iv.size, encrypted.size)
        prefs.edit().putString(KEY_SECRET, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun loadKey(): String {
        val encoded = prefs.getString(KEY_SECRET, null) ?: return ""
        return runCatching {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            require(packed.size > IV_BYTES) { "Stored key is invalid." }
            val iv = packed.copyOfRange(0, IV_BYTES)
            val encrypted = packed.copyOfRange(IV_BYTES, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }.getOrElse {
            prefs.edit().remove(KEY_SECRET).apply()
            ""
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_SECRET).apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "framebynavin_creator_copilot_v18"
        private const val KEY_SECRET = "encrypted_api_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
    }
}

object CreatorCopilotPromptEngine {
    private const val CREATOR_CONTEXT = """
You are the optional Creator Copilot inside FrameByNavin, a cinema-analysis creator workflow app.
The creator makes Telugu/English YouTube videos, Shorts, Instagram Reels and X posts about movie reviews,
cinematic moments, scene/frame breakdowns, filmmaking craft, release reactions and related cinema topics.
Be practical and production-ready. Prefer specific creator actions over generic advice. Do not invent facts about
movies or people that are not present in the user's input. When factual research is missing, explicitly mark what
must be verified. Never claim that content has been published or changed; you only draft suggestions.
"""

    fun instructions(): String = CREATOR_CONTEXT.trimIndent()

    fun prompt(tool: CreatorCopilotTool, input: String, task: CreatorTask?): String {
        val project = task?.let {
            "\nCurrent project context:\nTitle: ${it.title}\nPlatform: ${it.platform}\nFormat: ${it.contentType}\nExisting notes:\n${it.notes.take(5000)}\n"
        }.orEmpty()
        val request = when (tool) {
            CreatorCopilotTool.IDEA_TO_PLAN -> """
Turn the creator's rough idea into a compact production plan. Return: Working title, strongest angle,
recommended platform/format, audience promise, 5-8 production steps, research/verification needs, and one
clear next action. Do not pretend uncertain facts are verified.
"""
            CreatorCopilotTool.OUTLINE -> """
Create a strong creator-ready script outline. Return: hook, setup, ordered sections with the purpose of each,
key visual/B-roll or frame suggestions, conclusion, and CTA only if it naturally fits. Keep the structure usable
for cinema analysis rather than generic influencer content.
"""
            CreatorCopilotTool.HOOKS -> """
Write 8 substantially different opening hooks for this content. Mix direct, curiosity, cinematic-observation,
contrarian and emotional approaches. Keep each concise enough to narrate. Then identify the best default hook
and briefly explain why it fits the topic.
"""
            CreatorCopilotTool.REWRITE -> """
Rewrite the supplied draft so it sounds natural when spoken by a Telugu/English cinema creator. Preserve the
meaning, remove repetition, tighten pacing and avoid fake hype. If the draft mixes Telugu and English, keep the
mix natural rather than translating everything. Return only the polished draft plus a short note listing any
claims that still need verification.
"""
            CreatorCopilotTool.TITLE_PROMO -> """
Create a packaging set for the content: 10 YouTube/title options with different angles, 8 minimal thumbnail-text
options (prefer 2-5 words), one concise YouTube description, one natural X post, one Instagram caption, and a
short list of relevant tags/hashtags. Avoid clickbait that the content cannot support.
"""
        }
        return "$request$project\nCreator input:\n${input.trim()}".trim()
    }
}

class CreatorCopilotClient(private val context: Context) {
    fun generate(tool: CreatorCopilotTool, input: String, task: CreatorTask?): String {
        require(input.isNotBlank()) { "Add something for Copilot to work with first." }
        val secret = CreatorCopilotSecretStore(context).loadKey()
        require(secret.isNotBlank()) { "Connect an API key in Copilot setup first." }
        val config = CreatorCopilotConfigStore(context).snapshot()
        val body = JSONObject()
            .put("model", config.model)
            .put("instructions", CreatorCopilotPromptEngine.instructions())
            .put("input", CreatorCopilotPromptEngine.prompt(tool, input, task))
            .toString()

        val connection = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $secret")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val status = connection.responseCode
            val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                error(message.ifBlank { "Copilot request failed (HTTP $status)." })
            }
            return extractText(raw).ifBlank { error("Copilot returned no text.") }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractText(raw: String): String {
        val root = JSONObject(raw)
        val direct = root.optString("output_text").trim()
        if (direct.isNotBlank()) return direct
        val output = root.optJSONArray("output") ?: return ""
        val text = StringBuilder()
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                val value = part.optString("text").trim()
                if (value.isNotBlank()) {
                    if (text.isNotEmpty()) text.append("\n\n")
                    text.append(value)
                }
            }
        }
        return text.toString()
    }

    companion object {
        private const val RESPONSES_URL = "https://api.openai.com/v1/responses"
    }
}
