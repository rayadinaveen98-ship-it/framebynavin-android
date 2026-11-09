package com.framebynavin.app.cloud

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Restore the few visual/companion preferences NOT owned by native Creator
 * settings, Creator Smart escalation or the native Personal import.
 *
 * Each editor is restricted to known portable keys. Missing optional keys are
 * removed ONLY from that allowlist; unrelated Android IDs, OAuth tokens,
 * permissions, alarms and account data are never enumerated or modified.
 *
 * This writer is called ONLY after the native Creator + Personal backup
 * import under the owner's explicitly accepted unified recovery transaction.
 */
internal object TrustPortablePreferenceRestore {
    internal data class Receipt(
        val writtenFields: Int,
        val removedAbsentFields: Int,
    )

    private val visual = linkedMapOf(
        "framebynavin_visual_experience" to
            setOf("theme", "launch_sound"),
        "best_frames_original_prefs" to
            setOf("home_slideshow_paused"),
        "backlot_character_identity_v151" to
            setOf("selected_character"),
        "backlot_companion_suggestions_v2212" to
            setOf("suggestions_enabled", "dismissed"),
    )

    fun restoreOwnerApproved(
        context: Context,
        portableJson: String,
        ownerApproved: Boolean,
    ): Receipt {
        require(ownerApproved) {
            "Portable theme and Spark settings require explicit owner approval"
        }
        val verified = TrustPortablePreferenceCapture.validateRaw(portableJson)
        check(verified.sourceCoverageComplete) {
            "A portable source omitted saved settings; refusing partial restoration"
        }
        val json = JSONObject(verified.json)
        val entries = json.getJSONArray("sources")
        var updated = 0
        var removed = 0
        for (index in 0 until entries.length()) {
            val section = entries.getJSONObject(index)
            val name = section.getString("source")
            val permitted = visual[name] ?: continue
            val source = section.getJSONArray("entries")
            val actual = linkedMapOf<String, Pair<String, Any>>()
            repeat(source.length()) { k ->
                val item = source.getJSONObject(k)
                val key = item.getString("key")
                require(key in permitted && !actual.containsKey(key)) {
                    "Unsupported or repeated visual/companion preference"
                }
                val type = item.getString("type")
                val value = item.get("value")
                if (name == "backlot_character_identity_v151" &&
                    key == "selected_character") {
                    require(type == "string" &&
                        (value as String) == "FUNNY") {
                        "The original approved Spark is the only selectable companion"
                    }
                }
                actual[key] = type to value
            }
            val prefs = context.applicationContext.getSharedPreferences(
                name, Context.MODE_PRIVATE,
            )
            val editor = prefs.edit()
            permitted.forEach { key ->
                val field = actual[key]
                if (field == null) {
                    editor.remove(key)
                    removed++
                } else {
                    val (type, value) = field
                    when (type) {
                        "string" -> editor.putString(key, value as String)
                        "boolean" -> editor.putBoolean(key, value as Boolean)
                        "int" -> editor.putInt(key, (value as Number).toInt())
                        "long" -> editor.putLong(key, (value as Number).toLong())
                        "float" -> editor.putFloat(key, (value as Number).toFloat())
                        "string_set" -> {
                            val array = value as JSONArray
                            val items = (0 until array.length()).map(array::getString)
                            editor.putStringSet(key, items.toSet())
                        }
                        else -> error("Portable preference has unsupported value type")
                    }
                    updated++
                }
            }
            check(editor.commit()) {
                "Could not commit reviewed companion or theme settings"
            }
            val saved = prefs.all
            for (key in permitted) {
                val field = actual[key]
                if (field == null) {
                    check(key !in saved) {
                        "An absent owner preference was not cleared"
                    }
                } else {
                    val (type, value) = field
                    val now = saved[key]
                    val matched = when (type) {
                        "float" -> now is Float &&
                            now == (value as Number).toFloat()
                        "long" -> now is Long &&
                            now == (value as Number).toLong()
                        "int" -> now is Int &&
                            now == (value as Number).toInt()
                        "string_set" -> now is Set<*> &&
                            now == (0 until (value as JSONArray).length())
                                .map(value::getString).toSet()
                        else -> now == value
                    }
                    check(matched) {
                        "Restored companion/theme value failed immediate readback"
                    }
                }
            }
        }
        return Receipt(updated, removed)
    }
}
