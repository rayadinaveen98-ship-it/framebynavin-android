package com.framebynavin.app.personal

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs twice in separate instrumentation invocations on a disposable emulator.
 * First run targets the authentic signed Build 320; second targets signed 321.
 * No adb uninstall, no pm clear after baseline seed, no owner/account data.
 */
@RunWith(AndroidJUnit4::class)
class Phase1Build320InPlaceUpgradeDeviceTest {
    private val name = "backlot-phase1-in-place-upgrade-evidence"
    private val key = "original-build320-sentinel"
    private val original = "frozen-320-owner-local-data-was-preserved"

    private fun target(): Context {
        require(InstrumentationRegistry.getArguments()
            .getString("hardeningTestEnvironment") == "ci-emulator") {
            "Never run upgrade migration probes on a user device"
        }
        require(Build.FINGERPRINT.contains("generic") ||
            Build.FINGERPRINT.contains("sdk_gphone")) {
            "Upgrade retention test requires a disposable Android emulator"
        }
        return InstrumentationRegistry.getInstrumentation().targetContext
            .applicationContext
    }

    @Test fun seedBuild320LocalDataBeforeInstallUpgrade() {
        val app = target()
        val version = app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode
        assertEquals("Install authentic frozen Build 320 before seeding", 320L, version)
        assertTrue("Synthetic baseline preference must persist to disk",
            app.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
                .putString(key, original).commit())
        assertEquals(original,
            app.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key, null))
    }

    @Test fun verifySameLocalDataAfterUpgradeTo321() {
        val app = target()
        val version = app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode
        assertEquals("Signed update must raise version to 321", 321L, version)
        assertEquals("In-place update may not clear locally persisted application data",
            original, app.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key, null))
    }
}
