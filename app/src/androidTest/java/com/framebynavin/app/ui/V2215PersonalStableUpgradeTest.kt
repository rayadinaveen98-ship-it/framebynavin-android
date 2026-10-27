package com.framebynavin.app.ui

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Two deliberately separate CI-only emulator invocations.
 *
 * Seed the original V221.5 APK (version 239), then adb install -r the
 * personalStable V221.5 release candidate (version 240) WITHOUT clearing app
 * data. Verify both SharedPreferences and private files survived, even after
 * launching Backlot. This does not touch production user data or cloud.
 */
@RunWith(AndroidJUnit4::class)
class V2215PersonalStableUpgradeTest {
    private fun app(): Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val probeFileName = "v2215-upgrade-sentinel.txt"
    private val probePreferences = "backlot_v2215_upgrade_probe_ci_only"
    private val sentinel = "v2215-version239-to-240-no-data-clear"

    @Suppress("DEPRECATION")
    private fun installedVersion(): Long {
        val ctx = app()
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }

    @Test fun seedPreview239WithoutChangingCreatorData() {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals(239L, installedVersion())
        val ctx = app()
        assertTrue(
            ctx.getSharedPreferences(probePreferences, Context.MODE_PRIVATE).edit()
                .putString("sentinel", sentinel).commit()
        )
        val file = File(ctx.filesDir, probeFileName)
        file.writeText(sentinel)
        assertEquals(sentinel, file.readText())
    }

    @Test fun verifyPersonalStable240RetainsFilesAndPreferences() {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals(240L, installedVersion())
        val ctx = app()
        assertEquals(
            "The in-place update lost device-owned preferences",
            sentinel,
            ctx.getSharedPreferences(probePreferences, Context.MODE_PRIVATE)
                .getString("sentinel", null),
        )
        val file = File(ctx.filesDir, probeFileName)
        assertTrue("The in-place update removed device-owned private files", file.isFile)
        assertEquals(sentinel, file.readText())
    }
}
