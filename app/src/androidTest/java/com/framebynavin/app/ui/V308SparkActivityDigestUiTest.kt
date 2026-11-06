package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V308SparkActivityDigestUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun item(id: String, domain: V306ActivityDomain) = V306ActivityItem(
        id = id, domain = domain, bucket = V306ActivityBucket.NEEDS_ACTION,
        title = if (domain == V306ActivityDomain.PERSONAL) "My private skincare diary"
            else "Film review project",
        detail = "Verified current reminder evidence",
        atMillis = 10L, targetId = id,
    )

    @Test fun twoIndependentActivitiesOpenTheirCorrectDestinationsWithoutExposingPersonalTitle() {
        HardeningTestEnvironment.requireCiEmulator()
        var project: String? = null
        var personal = 0
        var all = 0
        rule.setContent {
            FrameByNavinTheme {
                V308SparkActivityDigest(
                    feed = listOf(
                        item("creator-1", V306ActivityDomain.CREATOR),
                        item("personal-1", V306ActivityDomain.PERSONAL),
                    ),
                    onOpenActivity = { all++ },
                    onOpenCreator = { project = it },
                    onOpenPersonal = { personal++ },
                )
            }
        }
        rule.onNodeWithTag("v308-spark-activity").assertExists()
        rule.onNodeWithText("Film review project").assertExists()
        rule.onNodeWithText("Personal reminder needs attention").assertExists()
        rule.onNodeWithText("My private skincare diary").assertDoesNotExist()

        val dir = File(requireNotNull(app.getExternalFilesDir(null)), "v308-visual-audit")
        check(dir.isDirectory || dir.mkdirs())
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        try {
            check(bitmap.width > 200 && bitmap.height > 200)
            File(dir, "01-spark-multi-activity.png").outputStream().use { out ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
            }
            check(File(dir, "01-spark-multi-activity.png").length() > 10_000L)
        } finally { bitmap.recycle() }

        rule.onNodeWithTag("v308-spark-activity-row-creator").performClick()
        rule.onNodeWithTag("v308-spark-activity-row-personal").performClick()
        rule.onNodeWithTag("v308-spark-see-all").performClick()
        rule.runOnIdle {
            assertEquals("creator-1", project)
            assertEquals(1, personal)
            assertEquals(1, all)
        }
    }
}
