package com.framebynavin.app.ui

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.R
import com.framebynavin.app.widget.V2273WidgetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Android real-layout preview tests: synthetic labels only, no stored data. */
@RunWith(AndroidJUnit4::class)
class V275WidgetPreviewTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()

    @Test fun allThreeActualLauncherLayoutsInflateAsPreviews() {
        for (kind in V2273WidgetKind.entries) {
            val parent = FrameLayout(app)
            val preview = V275WidgetPreviewSpec.sampleContent(kind, app.packageName).apply(app, parent)
            assertNotNull(preview)
            assertNotNull(preview.background)
        }
    }

    @Test fun myDayPreviewNeverIncludesPersonalTitles() {
        val parent = FrameLayout(app)
        val preview = V275WidgetPreviewSpec.sampleContent(
            V2273WidgetKind.MY_DAY, app.packageName,
        ).apply(app, parent)
        val personal = preview.findViewById<TextView>(R.id.v224_day_personal)
        val creator = preview.findViewById<TextView>(R.id.v224_day_creator)
        assertEquals("PERSONAL\n3 today", personal.text.toString())
        assertEquals("CREATOR\n2 tasks", creator.text.toString())
    }
}
