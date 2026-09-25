package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.widget.CreatorWidgetContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V145ProjectPulseDeepLinkTest {

    @Test
    fun projectPromptIntent_routesToExactStudioProject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val taskId = "project-v145-deeplink-test"

        val intent = ProjectPulseNextStagePrompt.projectLaunchIntent(context, taskId)

        assertEquals(CreatorWidgetContract.ACTION_OPEN_STUDIO, intent.action)
        assertEquals(taskId, intent.getStringExtra(CreatorWidgetContract.EXTRA_TASK_ID))
        assertEquals(taskId, intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID))
        assertTrue(intent.dataString.orEmpty().contains(taskId))
    }
}
