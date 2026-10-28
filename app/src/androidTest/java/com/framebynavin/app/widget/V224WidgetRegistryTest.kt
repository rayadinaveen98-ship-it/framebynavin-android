package com.framebynavin.app.widget

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Device-side manifest and RemoteViews resource contract. No private data required. */
@RunWith(AndroidJUnit4::class)
class V224WidgetRegistryTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress("DEPRECATION")
    @Test fun allThreeNewProvidersAreEnabledAndHaveRealLayouts() {
        val providers = listOf(
            V224QuickCaptureWidgetProvider::class.java,
            V224TodayFocusWidgetProvider::class.java,
            V224MyDayWidgetProvider::class.java,
        )
        providers.forEach {
            val receiver = app.packageManager.getReceiverInfo(ComponentName(app, it), 0)
            assertTrue(receiver.enabled)
        }
        listOf(
            R.layout.widget_v224_capture,
            R.layout.widget_v224_today,
            R.layout.widget_v224_myday,
        ).forEach { id ->
            assertNotNull(app.resources.getLayout(id))
        }
    }

    @Test fun oldWidgetProviderNamesRemainForAlreadyPinnedLauncherInstances() {
        // Migration cannot silently remove a user's launcher widget during an
        // in-place upgrade; final retirement happens only after owner's approval.
        @Suppress("DEPRECATION")
        val old = app.packageManager.getReceiverInfo(
            ComponentName(app, CreatorCompactWidgetProvider::class.java), 0,
        )
        assertTrue(old.enabled)
    }
}
