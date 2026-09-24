package com.framebynavin.app.data

/** Languages currently surfaced by Backlot's media discovery setup. */
object CreatorMediaLanguageRegistry {
    val supported: List<String> = listOf(
        "Telugu",
        "Tamil",
        "Malayalam",
        "Kannada",
        "Hindi",
        "English",
        "Bengali",
        "Marathi",
    )

    fun canonicalLabel(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        return supported.firstOrNull { it.equals(trimmed, ignoreCase = true) }
            ?: trimmed.take(40).replaceFirstChar { char -> char.uppercase() }
    }
}
