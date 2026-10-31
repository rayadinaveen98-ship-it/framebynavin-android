package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalPurpose
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * Optional personal planning shortcuts. All choices remain local to the
 * Personal reminder. No diagnosis, medical schedule or automatic behavior.
 * Two-column cards preserve readable text on compact Android devices.
 */
@Composable
internal fun V274PersonalPurposePicker(
    selected: PersonalPurpose,
    onSelected: (PersonalPurpose) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("v274-personal-purpose-picker"),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("01  WHAT MATTERS?", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            "Start with something personal, or write your own.",
            color = MutedText, fontSize = 12.sp,
        )
        PersonalPurpose.entries.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                pair.forEach { purpose ->
                    val active = purpose == selected
                    Surface(
                        onClick = { onSelected(purpose) },
                        modifier = Modifier.weight(1f)
                            .testTag("v274-purpose-${purpose.name.lowercase()}"),
                        shape = RoundedCornerShape(15.dp),
                        color = if (active) RecRed.copy(alpha = .11f) else CinemaSurface,
                        border = BorderStroke(1.dp, if (active) RecRed else CinemaLine),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                            Text(
                                purpose.label,
                                color = ProjectorIvory,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                purpose.supportingText,
                                color = MutedText,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                minLines = 2,
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
