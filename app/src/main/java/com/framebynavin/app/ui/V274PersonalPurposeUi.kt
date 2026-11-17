package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalPurpose
import com.framebynavin.app.ui.theme.BacklotUX10Tokens
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
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
        Text("CHOOSE A PURPOSE", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            "Start with something personal, or write your own.",
            color = MutedText, fontSize = 12.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PersonalPurpose.entries.forEach { purpose ->
                val active = purpose == selected
                Surface(
                    onClick = { onSelected(purpose) },
                    modifier = Modifier.width(
                        UX10DPersonalPresentationPolicy.purposeCardWidthDp(
                            LocalDensity.current.fontScale
                        ).dp
                    ).heightIn(min = 104.dp)
                        .testTag("v274-purpose-${purpose.name.lowercase()}"),
                    shape = RoundedCornerShape(BacklotUX10Tokens.surfaceRadius),
                    color = if (active) RecRed.copy(alpha = .11f) else CinemaSurface,
                    border = if (active) BacklotUX10Tokens.activeBorder(RecRed)
                    else BacklotUX10Tokens.quietBorder(CinemaLine),
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
        }
    }
}
