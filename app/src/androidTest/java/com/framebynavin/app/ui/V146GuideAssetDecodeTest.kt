package com.framebynavin.app.ui

import android.graphics.ImageDecoder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V146GuideAssetDecodeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectableRoster_isExactlyTheFourLockedGuides() {
        assertEquals(
            listOf(
                V144GuideIdentity.FUNNY,
                V144GuideIdentity.CUTE,
                V144GuideIdentity.KITTY,
                V144GuideIdentity.CUTE_GIRL,
            ),
            V144SelectableGuides,
        )
        assertEquals(V144GuideIdentity.CUTE, v144SelectableGuideOrDefault(V144GuideIdentity.FRAME))
        assertEquals(V144GuideIdentity.CUTE, v144SelectableGuideOrDefault(V144GuideIdentity.NAVI))
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun everySelectableGuidePose_decodesOnAndroid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.resources

        V144SelectableGuides.forEach { guide ->
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

    @Test
    fun everySelectableGuidePose_rendersThroughComposeWithoutCrashing() {
        composeRule.setContent {
            Column {
                V144SelectableGuides.forEach { guide ->
                    Row {
                        CinePulseState.entries.forEach { state ->
                            V144GuideCharacter(
                                guide = guide,
                                state = state,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun settingsPicker_showsOnlyTheLockedFourGuides() {
        composeRule.setContent { V144GuidePicker() }
        composeRule.onNodeWithText("Funny").assertExists()
        composeRule.onNodeWithText("Cute").assertExists()
        composeRule.onNodeWithText("Kitty").assertExists()
        composeRule.onNodeWithText("Cute Girl").assertExists()
        composeRule.onNodeWithText("Frame").assertDoesNotExist()
        composeRule.onNodeWithText("Navi").assertDoesNotExist()
    }
}
