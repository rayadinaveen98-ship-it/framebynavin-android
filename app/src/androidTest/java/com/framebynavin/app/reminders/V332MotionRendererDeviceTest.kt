package com.framebynavin.app.reminders

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test exercises a REAL Android WebView, not the separate browser HTML.
 * It captures PixelCopy from the compositor and asserts canvas visibility,
 * correct dynamic size and ring/symbol center alignment.
 * It never creates real alarms, notifications or reminder records.
 */
@RunWith(AndroidJUnit4::class)
class V332MotionRendererDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun creatorAlarmHasVisibleResponsiveAnimation() =
        verifyScene(V329ReminderDomain.CREATOR, V329ReminderChannel.ALARM, 0)

    @Test fun creatorVoiceHasVisibleResponsiveAnimation() =
        verifyScene(V329ReminderDomain.CREATOR, V329ReminderChannel.VOICE, 1)

    @Test fun personalAlarmHasVisibleResponsiveAnimation() =
        verifyScene(V329ReminderDomain.PERSONAL, V329ReminderChannel.ALARM, 2)

    @Test fun personalVoiceHasVisibleResponsiveAnimation() =
        verifyScene(V329ReminderDomain.PERSONAL, V329ReminderChannel.VOICE, 3)

    private fun verifyScene(
        domain: V329ReminderDomain,
        channel: V329ReminderChannel,
        worldIndex: Int,
    ) {
        val world = V329ReminderWorld.entries[worldIndex]
        rule.setContent {
            V330ReminderPrototypeScene(
                domain = domain,
                channel = channel,
                appearance = V329ReminderAppearance(world = world),
                onReveal = {},
                modifier = Modifier.fillMaxSize(),
                interactive = false,
                lowPowerOverride = false,
                motionOverride = true,
            )
        }
        rule.waitForIdle()
        Thread.sleep(3600L)
        val web = rule.runOnUiThread {
            findWeb(rule.activity.window.decorView)
        }
        assertNotNull("Real Android WebView absent: native fallback?", web)
        val viewportAndCenters = evaluate(
            requireNotNull(web),
            "(function(){try{" +
                "var s=window.BacklotMotion.state();" +
                "var f=document.querySelector('.energy-frame').getBoundingClientRect();" +
                "return !!(s.nodes>=180&&s.viewportWidth>=120&&s.viewportHeight>=180" +
                "&&Math.abs(f.y+f.height/2-s.centerY)<4);" +
                "}catch(e){return false}})()",
        )
        assertEquals("Real WebView did not render or align the Canvas scene", "true", viewportAndCenters)

        // The touch field must visibly displace particles and spring back.
        evaluate(requireNotNull(web), "window.BacklotMotion.startNormalized(.50,.47);true")
        Thread.sleep(200L)
        val moved = evaluate(
            requireNotNull(web),
            "window.BacklotMotion.state().averageOffset > .4",
        )
        assertEquals("Touch did not affect the particle field", "true", moved)
        evaluate(requireNotNull(web), "window.BacklotMotion.end();true")

        val activity = rule.activity
        val decor = activity.window.decorView
        val width = decor.width
        val height = decor.height
        assertTrue(width >= 200 && height >= 300)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val finished = CountDownLatch(1)
        var result = -1
        PixelCopy.request(activity.window, bitmap, { code ->
            result = code
            finished.countDown()
        }, Handler(Looper.getMainLooper()))
        assertTrue("Compositor screenshot timed out", finished.await(8, TimeUnit.SECONDS))
        assertEquals("PixelCopy failed", PixelCopy.SUCCESS, result)
        var colored = 0
        for (y in (height / 5)..(height * 3 / 4) step 8) {
            for (x in (width / 8)..(width * 7 / 8) step 8) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                if (red > 105 && red > green * 1.38f && red > blue * 1.08f) colored++
            }
        }
        assertTrue("WebView compositor shows no luminous particles", colored > 12)
        val filename = "v332_${domain.name.lowercase()}_${channel.name.lowercase()}.png"
        InstrumentationRegistry.getInstrumentation().targetContext.openFileOutput(
            filename, 0,
        ).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findWeb(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun evaluate(web: WebView, script: String): String {
        val finished = CountDownLatch(1)
        var result: String? = null
        rule.runOnUiThread {
            web.evaluateJavascript(script) {
                result = it
                finished.countDown()
            }
        }
        assertTrue("JavaScript callback never returned", finished.await(7, TimeUnit.SECONDS))
        return requireNotNull(result)
    }
}
