package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.V298ReminderGroup
import com.framebynavin.app.personal.V310LinkTargets
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V310ReviewLinksUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun optionalReviewChoicesRevealProgressivelyAndDoNotWriteRecords() {
        HardeningTestEnvironment.requireCiEmulator()
        val spaceId = "11000000-0000-4000-8000-000000000301"
        val groupId = "22000000-0000-4000-8000-000000000302"
        var targets by mutableStateOf(V310LinkTargets())
        rule.setContent {
            FrameByNavinTheme {
                V310ReviewLinks(
                    availableSpaces = listOf(V289SpaceEntity(
                        id = spaceId, title = "Skin Care", type = "SKIN_CARE",
                        iconKey = "sun", goalText = "", createdAtMillis = 100L,
                        archivedAtMillis = null, displayOrder = 0,
                    )),
                    availableGroups = listOf(V298ReminderGroup(
                        id = groupId, name = "Morning Rhythm", createdAtMillis = 100L,
                        updatedAtMillis = 100L,
                    )),
                    spacesReady = true,
                    groupsReady = true,
                    spacesError = null,
                    groupsError = null,
                    value = targets,
                    onChange = { targets = it },
                )
            }
        }
        rule.onNodeWithTag("v310-review-links").assertExists()
        rule.onNodeWithTag("v310-space-$spaceId").assertDoesNotExist()
        rule.onNodeWithTag("v310-group-$groupId").assertDoesNotExist()
        rule.onNodeWithTag("v310-expand-links").performClick()
        rule.onNodeWithTag("v310-space-$spaceId").performClick()
        rule.onNodeWithTag("v310-group-$groupId").performClick()
        rule.runOnIdle {
            assertEquals(spaceId, targets.spaceId)
            assertEquals(groupId, targets.groupId)
        }
        rule.onNodeWithText("Skin Care · Morning Rhythm").assertExists()
        val dir = File(requireNotNull(app.getExternalFilesDir(null)), "v310-visual-audit")
        check(dir.isDirectory || dir.mkdirs())
        rule.waitForIdle()
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        try {
            check(image.width >= 200 && image.height >= 200)
            val file = File(dir, "01-personal-review-optional-space-and-group.png")
            file.outputStream().use { output ->
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            check(file.length() > 10_000L)
        } finally { image.recycle() }
        rule.onNodeWithTag("v310-space-$spaceId").performClick()
        rule.onNodeWithTag("v310-group-$groupId").performClick()
        rule.runOnIdle { assertEquals(V310LinkTargets(), targets) }
    }

    @Test fun openingPersonalComposerDoesNotCreateAnUnusedSpacesDatabase() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = app.getDatabasePath(V289SpacesDatabase.FILE_NAME)
        assertFalse("Requires a disposable blank emulator", db.exists())
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalReminderHub(onClose = {}, onCreator = {})
            }
        }
        rule.onNodeWithTag("v2232-personal-add").performClick()
        rule.onNodeWithTag("v2232-personal-editor").assertExists()
        rule.waitForIdle()
        assertFalse("Opening an optional link picker must not create Room", db.exists())
    }

    @Test fun unavailableCatalogDoesNotInventEmptyOptionsOrFalseSelections() {
        HardeningTestEnvironment.requireCiEmulator()
        var targets by mutableStateOf(V310LinkTargets())
        rule.setContent {
            FrameByNavinTheme {
                V310ReviewLinks(
                    availableSpaces = emptyList(),
                    availableGroups = emptyList(),
                    spacesReady = false,
                    groupsReady = false,
                    spacesError = "Spaces couldn't be read. You can still save this reminder.",
                    groupsError = "Groups couldn't be read. You can still save this reminder.",
                    value = targets,
                    onChange = { targets = it },
                )
            }
        }
        rule.onNodeWithTag("v310-expand-links").performClick()
        rule.onNodeWithTag("v310-space-load-error").assertExists()
        rule.onNodeWithTag("v310-group-load-error").assertExists()
        rule.runOnIdle { assertEquals(V310LinkTargets(), targets) }
    }
}
