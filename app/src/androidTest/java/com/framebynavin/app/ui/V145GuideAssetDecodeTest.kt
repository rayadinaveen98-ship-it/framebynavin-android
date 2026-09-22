package com.framebynavin.app.ui

import android.graphics.ImageDecoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V145GuideAssetDecodeTest {

    @Test
    @RequiresApi(Build.VERSION_CODES.P)
    fun everyActiveGuidePose_decodesOnAndroid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources

        V145SelectableGuides.forEach { guide ->
            CinePulseState.entries.forEach { state ->
                val drawableId = v144GuideDrawable(guide, state)
                val bytes = resources.openRawResource(drawableId).use { it.readBytes() }
                val source = ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))
                val drawable = ImageDecoder.decodeDrawable(source)
                assertTrue("$guide / $state has invalid width", drawable.intrinsicWidth > 0)
                assertTrue("$guide / $state has invalid height", drawable.intrinsicHeight > 0)
            }
        }
    }
}
