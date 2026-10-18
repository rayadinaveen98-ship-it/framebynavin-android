package com.framebynavin.app.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Backlot V173: shared minimum text and target sizes for the New Project wizard,
 * Quick Create and existing-project editor. Larger system font settings may grow the
 * text beyond these sizes; layouts must wrap and expand rather than impose hard heights.
 */
internal object V173ProjectUiMetrics {
    val supportingText: Sp = 12.sp
    val choiceText: Sp = 12.sp
    val bodyText: Sp = 13.sp
    val reviewValueText: Sp = 13.sp
    val actionText: Sp = 13.sp
    val sectionHeadingText: Sp = 18.sp
    val touchTarget: Dp = 48.dp
    val actionMinHeight: Dp = 52.dp
    val reviewRowMinHeight: Dp = 62.dp
}
