package com.backlot.shared.cloud

/**
 * Product-level account lifecycle shared by Android and iOS.
 *
 * Provider credentials never belong here. Native adapters authenticate with Google/Apple/Supabase,
 * then hand this layer only the stable [BacklotIdentityRef] that the rest of Backlot may use.
 */
enum class BacklotSessionStatus {
    SIGNED_OUT,
    RESOLVING,
    SIGNED_IN,
}

/**
 * Immutable snapshot of the creator account visible to shared product logic.
 *
 * The invariant is intentionally strict: an identity exists only while the product session is
 * signed in. Starting a new authentication/account-switch resolution therefore drops the previous
 * identity immediately instead of letting it leak into the next account's workspace.
 */
data class BacklotSessionSnapshot(
    val status: BacklotSessionStatus,
    val identity: BacklotIdentityRef? = null,
) {
    init {
        val valid = when (status) {
            BacklotSessionStatus.SIGNED_IN -> identity != null
            BacklotSessionStatus.SIGNED_OUT,
            BacklotSessionStatus.RESOLVING,
            -> identity == null
        }
        require(valid) { "Backlot session status and identity are inconsistent" }
    }

    val isSignedIn: Boolean
        get() = status == BacklotSessionStatus.SIGNED_IN
}

/**
 * Deterministic account-state transitions used by both platforms.
 *
 * Authentication itself remains native. This reducer only converts a completed native identity
 * result into safe shared product state.
 */
object BacklotSessionReducer {
    fun signedOut(): BacklotSessionSnapshot = BacklotSessionSnapshot(
        status = BacklotSessionStatus.SIGNED_OUT,
    )

    fun beginResolution(): BacklotSessionSnapshot = BacklotSessionSnapshot(
        status = BacklotSessionStatus.RESOLVING,
    )

    fun authenticated(identity: BacklotIdentityRef): BacklotSessionSnapshot {
        require(identity.providerSubject.isNotBlank()) {
            "Authenticated Backlot identity requires a non-blank provider subject"
        }
        require(identity.cloudAccountId.isEmpty() || identity.cloudAccountId.isNotBlank()) {
            "Backlot cloud account id cannot be blank when present"
        }
        return BacklotSessionSnapshot(
            status = BacklotSessionStatus.SIGNED_IN,
            identity = identity,
        )
    }

    fun signOut(): BacklotSessionSnapshot = signedOut()
}
