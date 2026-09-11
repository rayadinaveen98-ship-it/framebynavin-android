package com.framebynavin.app.youtube

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

/** Encode one URL path segment. URLEncoder uses '+' for spaces, while URL paths require %20. */
internal fun encodePath(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
