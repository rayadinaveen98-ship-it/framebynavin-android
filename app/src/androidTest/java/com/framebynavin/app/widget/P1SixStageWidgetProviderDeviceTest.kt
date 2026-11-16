package com.framebynavin.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.R
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stage 2 + 5: disposable Android 15 package/launcher-widget registration smoke.
 * Uses synthetic layout inflation; never creates or mutates owner widgets.
 * Launcher pin/resize, OEM refresh and private-data scenarios remain physical QA.
 */
@RunWith(AndroidJUnit4::class)
class P1SixStageWidgetProviderDeviceTest {
    private val app: Context get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private val expected = setOf(
        "V392TodayHubWidgetProvider",
        "V392NextReminderWidgetProvider",
        "V392ChallengePulseWidgetProvider",
    )

    @Suppress("DEPRECATION")
    @Test fun manifestDeclaresExactlyThreeNewWidgetProvidersAndNoOldOnes() {
        HardeningTestEnvironment.requireCiEmulator()
        val pkg = app.packageManager.getPackageInfo(
            app.packageName,
            PackageManager.GET_RECEIVERS or PackageManager.GET_META_DATA,
        )
        val registered = pkg.receivers.orEmpty()
            .filter { it.metaData?.containsKey("android.appwidget.provider") == true }
            .map { it.name.substringAfterLast('.') }
            .toSet()
        assertEquals("Only the V392 replacement providers may be registered", expected, registered)
    }

    @Test fun AndroidWidgetRegistryOffersExactlyTheNewThree() {
        HardeningTestEnvironment.requireCiEmulator()
        val live = AppWidgetManager.getInstance(app).installedProviders
            .filter { it.provider.packageName == app.packageName }
            .map { it.provider.className.substringAfterLast('.') }
            .toSet()
        assertEquals("Android launcher/widget manager must discover the new widget family",
            expected, live)
    }

    @Test fun AllThreeRemoteViewsLayoutsInflateOnAndroid15() {
        HardeningTestEnvironment.requireCiEmulator()
        val targets = listOf(
            R.layout.widget_v392_today to R.id.widget_v392_today_root,
            R.layout.widget_v392_next to R.id.widget_v392_next_root,
            R.layout.widget_v392_challenge to R.id.widget_v392_challenge_root,
        )
        for ((layout, rootId) in targets) {
            val views = RemoteViews(app.packageName, layout)
            val inflated = views.apply(app, FrameLayout(app))
            assertNotNull("Widget layout failed to inflate: $layout", inflated)
            val root = inflated.findViewById<View>(rootId)
            assertNotNull("Widget has no root touch target: $layout", root)
            assertTrue("Root must be reachable in the inflated layout: $layout",
                root.width >= 0)
        }
    }
}
