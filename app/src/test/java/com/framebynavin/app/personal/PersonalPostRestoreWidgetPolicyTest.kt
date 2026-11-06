package com.framebynavin.app.personal

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Post-commit widget refresh is not permission to undo committed Personal data. */
class PersonalPostRestoreWidgetPolicyTest {
    @Test fun successfulWidgetRefreshReportsNoRecoveryAttention() {
        var executed = 0
        assertTrue(PersonalPostRestoreWidgetPolicy.refresh { executed++ })
        assertTrue(executed == 1)
    }

    @Test fun widgetSecurityExceptionReportsAttentionNotFailedRestore() {
        val completed = PersonalBackupRestoreReceipt(
            importedCount = 2,
            recoveryAttentionNeeded = !PersonalPostRestoreWidgetPolicy.refresh {
                throw SecurityException("Widget permissions changed")
            },
        )
        assertTrue(completed.recoveryAttentionNeeded)
        assertTrue(completed.importedCount == 2)
    }

    @Test fun arbitraryWidgetFailureNeverInventsSuccess() {
        assertFalse(PersonalPostRestoreWidgetPolicy.refresh {
            throw IllegalStateException("Launcher widget update failed")
        })
    }

    @Test fun cancellationIsNeverHiddenAsRecoverySuccess() {
        val cancellation = CancellationException("scope cancelled")
        val thrown = assertThrows(CancellationException::class.java) {
            PersonalPostRestoreWidgetPolicy.refresh { throw cancellation }
        }
        assertTrue(thrown === cancellation)
    }
}
