package com.pianostudio.alpha

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P0NavigationSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private val packageName = "com.pianostudio.alpha"

    private fun launch(clazz: Class<out Activity>, orientation: Int) {
        val context = instrumentation.targetContext
        val intent = Intent(context, clazz).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<Activity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(orientation, activity.requestedOrientation)
                assertTrue(activity.window.decorView.isAttachedToWindow)
            }
            // Allow Compose DisposableEffects to start and call the real native bridge.
            instrumentation.waitForIdleSync()
            Thread.sleep(1500)
            assertTrue("App process died while opening ${clazz.simpleName}", device.executeShellCommand("pidof $packageName").trim().isNotEmpty())
        }
    }

    @Test fun everyProductionActivityStartsWithoutNativeCrash() {
        launch(StudioHomeActivity::class.java, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        launch(LearningHubActivity::class.java, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        launch(R2FreePianoActivity::class.java, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        launch(R2PracticeActivity::class.java, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        launch(R2LessonActivity::class.java, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    private fun clickText(text: String) {
        val target = device.wait(Until.findObject(By.text(text)), 10000)
        assertTrue("Missing navigation target: $text", target != null)
        target.click()
        assertTrue("App did not remain visible after $text", device.wait(Until.hasObject(By.pkg(packageName)), 10000))
        instrumentation.waitForIdleSync()
        Thread.sleep(1000)
        assertTrue("Process died after $text", device.executeShellCommand("pidof $packageName").trim().isNotEmpty())
    }

    @Test fun homeNavigationOpensRealPlayers() {
        ActivityScenario.launch(StudioHomeActivity::class.java).use { scenario ->
            clickText("Free Piano")
            assertTrue(device.wait(Until.hasObject(By.text("Free Piano")), 5000))
            device.pressBack()
            instrumentation.waitForIdleSync()
            clickText("Smart Practice")
            device.pressBack()
            instrumentation.waitForIdleSync()
            clickText("Piano Foundations")
            clickText("Find Middle C")
            assertTrue(device.executeShellCommand("pidof $packageName").trim().isNotEmpty())
            device.pressBack()
        }
    }
}
