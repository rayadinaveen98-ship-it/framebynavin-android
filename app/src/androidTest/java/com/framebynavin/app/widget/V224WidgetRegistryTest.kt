package com.framebynavin.app.widget

import android.content.ComponentName
import android.content.pm.PackageManager
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
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
            val receiver = app.packageManager.getReceiverInfo(
                ComponentName(app, it), PackageManager.GET_META_DATA,
            )
            assertTrue(receiver.enabled)
            assertTrue(
                receiver.metaData?.getInt("android.appwidget.provider", 0) != 0,
            )
        }
        listOf(
            R.layout.widget_v224_capture,
            R.layout.widget_v224_today,
            R.layout.widget_v224_myday,
        ).forEach { id ->
            assertNotNull(app.resources.getLayout(id))
        }
    }

    @Test fun allThreeRemoteViewsInflateOnActualAndroidRuntime() {
        listOf(
            R.layout.widget_v224_capture,
            R.layout.widget_v224_today,
            R.layout.widget_v224_myday,
        ).forEach { layout ->
            val view = RemoteViews(app.packageName, layout).apply(app, FrameLayout(app))
            assertNotNull(view)
        }
    }

    @Test fun compactLayoutsExposeTheExpectedResizeTargets() {
        val capture = RemoteViews(app.packageName, R.layout.widget_v224_capture)
            .apply(app, FrameLayout(app))
        val focus = RemoteViews(app.packageName, R.layout.widget_v224_today)
            .apply(app, FrameLayout(app))
        val day = RemoteViews(app.packageName, R.layout.widget_v224_myday)
            .apply(app, FrameLayout(app))
        assertNotNull(capture.findViewById<android.view.View>(R.id.v224_capture_prompt))
        assertNotNull(focus.findViewById<android.view.View>(R.id.v224_today_detail))
        assertNotNull(focus.findViewById<android.view.View>(R.id.v224_today_reminder))
        assertNotNull(day.findViewById<android.view.View>(R.id.v224_day_headline))
        assertNotNull(day.findViewById<android.view.View>(R.id.v224_day_open_personal))
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
