package com.framebynavin.app.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * UX12 D7 — read-only editorial signature for the three RX01 MAIN surfaces.
 *
 * It reads only the already selected, domain-owned visual palette. No
 * occurrence identity, delivery state, time source, privacy decision,
 * reminder action or persistence is owned by this component.
 */
@Composable
internal fun UX12D7ReminderEditorialHeader(
    domain: RX01ReminderDomain,
    label: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
) {
    val palette = RX01FluidTheme.palette(domain, RX01SurfacePhase.MAIN)
    Row(
        modifier = modifier.fillMaxWidth()
            .heightIn(min = 40.dp)
            .testTag("ux12-d7-header-" + domain.name.lowercase()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(3.dp).height(26.dp)
                .background(palette.iconHalo, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp,
            color = palette.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!trailingText.isNullOrBlank()) {
            Spacer(Modifier.width(7.dp))
            Text(
                trailingText,
                color = palette.iconTint,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
