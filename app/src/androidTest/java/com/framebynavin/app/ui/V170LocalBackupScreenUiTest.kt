package com.framebynavin.app.ui

import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Legacy backup screens are no longer Android entry points. */
@RunWith(AndroidJUnit4::class)
class V170LocalBackupScreenUiTest {
    @Test fun legacyBackupRestoreActivitiesCannotBeLaunched() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pm = context.packageManager
        val removed = listOf(
            "com.framebynavin.app.ui.BackupActivity",
            "com.framebynavin.app.ui.PersonalBackupActivity",
            "com.framebynavin.app.ui.RecoveryCopiesActivity",
            "com.framebynavin.app.cloud.CloudSyncActivity",
        )
        removed.forEach { name ->
            val exists = runCatching {
                @Suppress("DEPRECATION")
                pm.getActivityInfo(ComponentName(context.packageName, name), 0)
            }.isSuccess
            assertFalse("Withdrawn backup activity is still manifest-registered: $name", exists)
        }
        assertTrue("Owner-approved local erase must remain available",
            runCatching {
                @Suppress("DEPRECATION")
                pm.getActivityInfo(
                    ComponentName(context, FreshStartActivity::class.java), 0)
            }.isSuccess)
    }
}
