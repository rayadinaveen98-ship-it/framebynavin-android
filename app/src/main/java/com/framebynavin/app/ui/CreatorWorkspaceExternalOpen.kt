package com.framebynavin.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.framebynavin.app.data.CreatorWorkspaceLinkRules

/** Invoked only on explicit creator taps. Returns null on success or a short visible error. */
internal object CreatorWorkspaceExternalOpen {
    fun openSource(context: Context, url: String): String? = open(context, url, allowDocument = false)

    fun openAsset(context: Context, location: String): String? = open(context, location, allowDocument = true)

    private fun open(context: Context, raw: String, allowDocument: Boolean): String? {
        val valid = if (allowDocument) CreatorWorkspaceLinkRules.isAssetLocation(raw)
            else CreatorWorkspaceLinkRules.isWebUrl(raw)
        if (!valid) return if (allowDocument) "This file link isn't supported." else "Enter a valid HTTP or HTTPS link."

        val location = raw.trim()
        val uri = Uri.parse(location)
        val isDocument = uri.scheme.equals("content", ignoreCase = true)
        val intent = Intent(Intent.ACTION_VIEW)
        if (isDocument) {
            // SAF grant was persisted when the creator originally attached the document.
            // Pass a transient read grant to the chosen viewing app; do not grant write access.
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            if (!mime.isNullOrBlank()) intent.setDataAndType(uri, mime)
            else intent.data = uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.data = uri
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return try {
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "No app is available to open this link."
        } catch (_: SecurityException) {
            "Access to this file has expired. Attach it again."
        } catch (_: IllegalArgumentException) {
            "This link can't be opened."
        }
    }
}
