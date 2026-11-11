package com.framebynavin.app.reminders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared V331 post-reveal visual language. Based on the actual approved
 * Backlot V1 prototype's `.detail` panel (burgundy radial backdrop,
 * luminous rounded icon plaque, centered title, large time and two primary
 * actions). This is not a new reminder engine or a new interaction.
 *
 * All four combinations use this same layout language while keeping
 * their original domain-specific callback contracts.
 */
internal object V331RevealStyle {
    val ink = Color(0xFF09080D)
    val surface = Color(0xFF241C24)
    val line = Color(0x33FFFFFF)
    val foreground = Color(0xFFF9F3F6)
    val secondary = Color(0xFFBBAEB7)
    val red = Color(0xFFF42B50)

    fun accent(domain: V329ReminderDomain, channel: V329ReminderChannel): Color =
        when (domain to channel) {
            V329ReminderDomain.CREATOR to V329ReminderChannel.ALARM -> Color(0xFFFF6D84)
            V329ReminderDomain.CREATOR to V329ReminderChannel.VOICE -> Color(0xFFFF92B6)
            V329ReminderDomain.PERSONAL to V329ReminderChannel.ALARM -> Color(0xFFFF7B8E)
            else -> Color(0xFFFFA5C8)
        }

    fun timeLabel(millis: Long): String =
        if (millis <= 0L) "" else
            DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    // The Creator due label is presentation text from the existing task.
    // Only show a scheduled time if the label actually contains one.
    private val scheduledTime = Regex(
        "(?i)\\b(1[0-2]|0?[1-9]):([0-5][0-9])\\s*(AM|PM)\\b"
    )

    fun timeFromCreatorDueLabel(dueLabel: String): String =
        scheduledTime.find(dueLabel)?.value.orEmpty()
}

@Composable
internal fun V331RevealBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // CSS source: radial-gradient(ellipse at 50% 15%,
        // #46121f 0%, #150911 36%, #09090d 75%)
        drawRect(Brush.verticalGradient(listOf(
            Color(0xFF210C16),
            Color(0xFF150911),
            Color(0xFF09090D),
        )))
        val center = Offset(size.width * .5f, size.height * .15f)
        val radius = size.height * .69f
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFF46121F).copy(alpha = .92f),
                .34f to Color(0xFF2D0C18).copy(alpha = .65f),
                1f to Color.Transparent,
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        // Static, low-cost reflective hue behind the hero plaque.
        val highlightCenter = Offset(size.width * .50f, size.height * .28f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x2BDF365B), Color.Transparent),
                center = highlightCenter,
                radius = size.width * .46f,
            ),
            center = highlightCenter,
            radius = size.width * .46f,
        )
    }
}

@Composable
internal fun V331PrototypeRevealHero(
    domain: V329ReminderDomain,
    channel: V329ReminderChannel,
    title: String,
    subtitle: String,
    timeLabel: String,
    chips: List<String>,
    compact: Boolean,
    titleTag: String? = null,
    modifier: Modifier = Modifier,
) {
    val accent = V331RevealStyle.accent(domain, channel)
    val kicker = buildString {
        append(if (domain == V329ReminderDomain.CREATOR) "CREATOR" else "PERSONAL")
        append("  /  ")
        append(if (channel == V329ReminderChannel.ALARM) "ALARM REMINDER" else "VOICE REMINDER")
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = kicker,
            color = Color(0xFFE1A7B7),
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 2.0.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(Modifier.height(if (compact) 16.dp else 29.dp))
        Surface(
            modifier = Modifier.size(if (compact) 78.dp else 94.dp),
            shape = RoundedCornerShape(if (compact) 23.dp else 28.dp),
            color = Color(0xFF4D1221),
            border = BorderStroke(1.dp, Color(0x88FF5E7B)),
            shadowElevation = 18.dp,
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // The prototype's rounded icon plaque, not the old Spark
                // illustration or an unrelated solid Android icon card.
                Icon(
                    imageVector = if (channel == V329ReminderChannel.ALARM)
                        Icons.Outlined.Alarm else Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(if (compact) 37.dp else 44.dp),
                )
            }
        }
        Spacer(Modifier.height(if (compact) 18.dp else 23.dp))
        Text(
            text = title,
            modifier = if (titleTag != null) Modifier.testTag(titleTag) else Modifier,
            color = V331RevealStyle.foreground,
            fontSize = if (compact) 25.sp else 29.sp,
            lineHeight = if (compact) 30.sp else 35.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.7).sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            Text(
                text = subtitle,
                color = V331RevealStyle.secondary,
                fontSize = if (compact) 12.sp else 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (timeLabel.isNotBlank()) {
            Spacer(Modifier.height(if (compact) 22.dp else 30.dp))
            val pieces = timeLabel.trim().split(Regex("\\s+"), limit = 2)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = pieces.first(),
                    color = V331RevealStyle.foreground,
                    fontSize = if (compact) 52.sp else 65.sp,
                    lineHeight = if (compact) 56.sp else 69.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-3).sp,
                )
                if (pieces.size > 1) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = pieces[1].uppercase(Locale.getDefault()),
                        color = V331RevealStyle.secondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        val validChips = chips.filter { it.isNotBlank() }.take(2)
        if (validChips.isNotEmpty()) {
            Spacer(Modifier.height(if (compact) 15.dp else 23.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                validChips.forEach { chip ->
                    Surface(
                        color = V331RevealStyle.surface,
                        border = BorderStroke(1.dp, V331RevealStyle.line),
                        shape = RoundedCornerShape(32.dp),
                    ) {
                        Text(
                            text = chip,
                            color = Color(0xFFC9BFC6),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }
    }
}
