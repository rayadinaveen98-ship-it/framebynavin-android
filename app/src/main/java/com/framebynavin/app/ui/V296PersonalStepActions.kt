package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Guided creation shares the validated legacy footer's compact-width/font
 * breakpoints so navigation remains reachable on small phones and large text.
 */
@Composable
internal fun V296PersonalStepActions(
    step: Int,
    saving: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    require(step in 0..2)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 10.dp)
        .testTag("v296-composer-footer")) {
        val stack = V2271PersonalFooterPolicy.vertical(
            maxWidth.value, LocalDensity.current.fontScale,
        )
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                V296ContinueAction(saving, onContinue, Modifier.fillMaxWidth())
                V296BackAction(step, onBack, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V296BackAction(step, onBack, Modifier.weight(1f))
                V296ContinueAction(saving, onContinue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V296BackAction(step: Int, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).testTag("v296-composer-back"),
    ) { Text(if (step == 0) "CANCEL" else "BACK") }
}

@Composable
private fun V296ContinueAction(
    saving: Boolean, onClick: () -> Unit, modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !saving,
        modifier = modifier.heightIn(min = 52.dp).testTag("v296-composer-next"),
    ) { Text("CONTINUE") }
}
