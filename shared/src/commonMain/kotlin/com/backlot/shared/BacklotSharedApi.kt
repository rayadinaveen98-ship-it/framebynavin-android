package com.backlot.shared

import com.backlot.shared.cloud.BacklotIdentityProvider
import com.backlot.shared.cloud.BacklotIdentityRef
import com.backlot.shared.cloud.BacklotSessionReducer
import com.backlot.shared.cloud.BacklotSessionSnapshot
import com.backlot.shared.workflow.BacklotContentDna
import com.backlot.shared.workflow.BacklotProjectDescriptor
import com.backlot.shared.workflow.BacklotWorkflowResolver
import com.backlot.shared.workflow.BacklotWorkflowTemplate

/**
 * Stable, Swift-friendly entry point into Backlot shared product logic.
 *
 * Keep platform UIs behind this facade instead of teaching Swift/Android screens the internal
 * resolver graph. More shared capabilities can be added here as the migration proceeds.
 */
object BacklotSharedApi {
    const val CONTRACT_VERSION: Int = 2

    fun resolveWorkflow(
        creatorModeId: String,
        archetypeId: String,
        archetypeLabel: String,
        platform: String,
        deliveryFormat: String,
        legacyContentType: String,
    ): BacklotWorkflowTemplate? = BacklotWorkflowResolver.templateFor(
        BacklotProjectDescriptor(
            platform = platform,
            contentType = legacyContentType,
            contentDna = BacklotContentDna(
                creatorModeId = creatorModeId,
                archetypeId = archetypeId,
                archetypeLabel = archetypeLabel,
                platform = platform,
                deliveryFormat = deliveryFormat,
            ),
        )
    )

    /** Shared signed-out state. Native auth/session credentials remain outside this API. */
    fun signedOutSession(): BacklotSessionSnapshot = BacklotSessionReducer.signedOut()

    /**
     * Clears any previously visible identity while a native provider resolves sign-in or an
     * account switch.
     */
    fun resolvingSession(): BacklotSessionSnapshot = BacklotSessionReducer.beginResolution()

    /**
     * Converts a completed native identity result into provider-neutral shared product state.
     * Provider access/refresh tokens must never be passed through this facade.
     *
     * This compatibility path intentionally has no cloud account id. Existing Android/local-only
     * callers can keep using it while native cloud auth migrates to [signedInCloudSession].
     */
    fun signedInSession(
        provider: BacklotIdentityProvider,
        providerSubject: String,
        email: String,
        displayName: String,
        avatarUrl: String,
    ): BacklotSessionSnapshot = BacklotSessionReducer.authenticated(
        BacklotIdentityRef(
            provider = provider,
            providerSubject = providerSubject,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
        )
    )

    /**
     * Cloud-backed identity path. [cloudAccountId] is the canonical Supabase user id and is the
     * only account key that future cross-device sync should use for cloud ownership.
     */
    fun signedInCloudSession(
        provider: BacklotIdentityProvider,
        providerSubject: String,
        cloudAccountId: String,
        email: String,
        displayName: String,
        avatarUrl: String,
    ): BacklotSessionSnapshot = BacklotSessionReducer.authenticated(
        BacklotIdentityRef(
            provider = provider,
            providerSubject = providerSubject,
            cloudAccountId = cloudAccountId,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
        )
    )

    fun signOutSession(): BacklotSessionSnapshot = BacklotSessionReducer.signOut()
}
