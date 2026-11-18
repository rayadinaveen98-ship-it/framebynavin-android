package com.framebynavin.app.cloud.ux09rpc

import android.content.Context
import com.framebynavin.app.cloud.CloudApiClient
import com.framebynavin.app.cloud.CloudSyncManager
import com.framebynavin.app.cloud.ux09.Ux09Intent

/**
 * Lab-only, account-generation-fenced Project/Idea RPC facade.
 *
 * NO existing app entrypoints, editors, WorkManager jobs or backup paths call
 * this class. Feature activation requires separate integration certification.
 *
 * CloudSyncManager.withFreshSession checks account epoch before + after I/O;
 * CloudApiClient hard-rejects any non-Lab backend before sending a request.
 */
internal class Ux09LabSignedRpcClient(context: Context) {
    private val account = CloudSyncManager(context.applicationContext)
    private val api = CloudApiClient()

    suspend fun apply(intent: Ux09Intent): Ux09LabRpcContract.Apply =
        account.withFreshSession { session ->
            Ux09LabRpcContract.parseApply(
                api.backlotUx09LabApplyRaw(session, intent),
                intent,
            )
        }

    /**
     * Manual signed LAB page preview only; no writable operation is called.
     * The account epoch is checked before and after the signed request.
     * Authentication refresh may update only the stored sign-in session.
     */
    suspend fun previewFirstPage(): Ux09LabRemotePreview = account.withFreshSession { session ->
        val raw = api.backlotUx09LabReadOnlyPreviewRaw(
            session,
            afterSequence = 0L,
            limit = Ux09LabRemotePreviewPolicy.PAGE_LIMIT,
        )
        val page = Ux09LabRpcContract.parsePull(
            raw, afterSequence = 0L,
            limit = Ux09LabRemotePreviewPolicy.PAGE_LIMIT,
        )
        Ux09LabRemotePreviewPolicy.summarize(page)
    }

    suspend fun pull(afterSequence: Long, limit: Int): Ux09LabRpcContract.Page =
        account.withFreshSession { session ->
            Ux09LabRpcContract.parsePull(
                api.backlotUx09LabPullRaw(session, afterSequence, limit),
                afterSequence,
                limit,
            )
        }
}
