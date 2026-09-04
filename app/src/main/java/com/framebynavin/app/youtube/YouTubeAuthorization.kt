package com.framebynavin.app.youtube

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope
import java.security.MessageDigest

object YouTubeAuthorization {
    const val YOUTUBE_READONLY = "https://www.googleapis.com/auth/youtube.readonly"
    const val ANALYTICS_READONLY = "https://www.googleapis.com/auth/yt-analytics.readonly"

    val scopes: List<Scope> = listOf(
        Scope(YOUTUBE_READONLY),
        Scope(ANALYTICS_READONLY),
    )

    fun request(selectAccount: Boolean = false): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(scopes)
        if (selectAccount) {
            builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
        }
        return builder.build()
    }

    fun signingSha1(context: Context): String {
        return runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val bytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            } ?: return@runCatching "Unavailable"
            MessageDigest.getInstance("SHA-1")
                .digest(bytes)
                .joinToString(":") { "%02X".format(it) }
        }.getOrDefault("Unavailable")
    }
}
