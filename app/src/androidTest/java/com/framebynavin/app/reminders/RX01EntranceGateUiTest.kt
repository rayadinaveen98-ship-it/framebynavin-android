package com.framebynavin.app.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic, disposable visual identities ONLY; no real notification,
 * store writes, alarm service, user data, backend or real delivered receipt.
 * Run on the isolated CI emulator, never against live user data.
 */
@RunWith(AndroidJUnit4::class)
class RX01EntranceGateUiTest {
    @get:Rule val rule = createComposeRule()

    private fun scene(valid: () -> Boolean, rejected: () -> Unit = {}) {
        rule.setContent {
            FrameByNavinTheme {
                RX01ReminderExperienceGate(
                    domain = RX01ReminderDomain.CREATOR,
                    mode = RX01ReminderIconKind.VOICE,
                    occurrenceKey = "synthetic:creator:voice:test-only",
                    occurrenceIsActive = true,
                    validateOnOpen = { valid() },
                    motionEnabled = false,
                    lowPower = true,
                    onInvalidOccurrence = rejected,
                ) {
                    Box(Modifier.fillMaxSize().testTag("rx01-test-main-content")) {
                        Text("Synthetic response controls")
                    }
                }
            }
        }
    }

    @Test fun ordinarySingleTapDoesNotUnlockThenDoubleTapUnlocksOnce() {
        HardeningTestEnvironment.requireCiEmulator()
        val verifications = AtomicInteger(0)
        scene(valid = {
            verifications.incrementAndGet()
            true
        })
        rule.onNodeWithTag("rx01-entry-double-tap-surface").assertExists()
        rule.onNodeWithTag("rx01-test-main-content").assertDoesNotExist()
        rule.onNodeWithTag("rx01-entry-double-tap-surface")
            .performTouchInput { click() }
        rule.waitForIdle()
        rule.onNodeWithTag("rx01-test-main-content").assertDoesNotExist()

        rule.onNodeWithTag("rx01-entry-double-tap-surface")
            .performTouchInput { doubleClick() }
        rule.waitUntil(5_000L) {
            rule.onAllNodesWithTag("rx01-test-main-content").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("rx01-test-main-content").assertExists()
        assertEquals("Fresh verification before and after entrance transition",
            2, verifications.get())
    }

    @Test fun invalidCanonicalOwnerCannotEnterMainAfterDoubleTap() {
        HardeningTestEnvironment.requireCiEmulator()
        val rejected = AtomicInteger(0)
        scene(valid = { false }, rejected = { rejected.incrementAndGet() })
        rule.onNodeWithTag("rx01-entry-double-tap-surface")
            .performTouchInput { doubleClick() }
        rule.waitUntil(5_000L) { rejected.get() > 0 }
        rule.onNodeWithTag("rx01-test-main-content").assertDoesNotExist()
        rule.onNodeWithTag("rx01-entry-double-tap-surface").assertExists()
    }

    @Test fun accessibleOpenActionUsesSameOwnerValidation() {
        HardeningTestEnvironment.requireCiEmulator()
        val verifications = AtomicInteger(0)
        scene(valid = {
            verifications.incrementAndGet()
            true
        })
        rule.onNodeWithTag("rx01-entry-double-tap-surface")
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(5_000L) {
            rule.onAllNodesWithTag("rx01-test-main-content").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(2, verifications.get())
    }
}
