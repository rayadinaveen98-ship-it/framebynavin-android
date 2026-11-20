package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalDeliveryContract
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V288PersonalTodayPolicy
import com.framebynavin.app.personal.V288TodaySnapshot
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UX11-E: data-backed, read-only Personal Today projection, not a new store,
 * alarm, completion/streak engine, photo observer or response coordinator.
 * Reuses the exact V288 today snapshot and source-authorized Done/Snooze.
 * Render only in the unlocked local Personal hub, never notification/keyguard.
 */
@Composable
internal fun UX11EPersonalSanctuaryHero(
    snapshot: V288TodaySnapshot,
    nowMillis: Long,
    busy: Boolean,
    historyReady: Boolean,
    onRespond: (PersonalReminder, String) -> Unit,
    onCreateReminder: () -> Unit,
    onOpenSpaces: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = Color(0xFFFFF5EF)
    val rose = Color(0xFFFFB9AE)
    val gold = Color(0xFFE9C69A)
    val soft = Color(0xFFBDB1B9)
    val next = snapshot.next
    val attention = V288PersonalTodayPolicy.isNeedsAttention(next, nowMillis)
    val day = snapshot.date.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    )
    val greeting = when (
        Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).hour
    ) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "A quiet moment"
    }
    Box(modifier.fillMaxWidth().testTag("ux11e-personal-sanctuary-hero")) {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("v288-today-home"),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF1C121D),
            border = BorderStroke(1.dp, Color(0xFF7F5660).copy(alpha = .60f)),
        ) {
            Box {
                // Original warm atmospheric material, deliberately static,
                // with no always-running bitmap/shader/remote-image decode.
                Canvas(Modifier.matchParentSize()) {
                    drawRect(Brush.verticalGradient(listOf(
                        Color(0xFF241825), Color(0xFF18141D), Color(0xFF1C1218)
                    )))
                    val w = size.width
                    val h = size.height
                    val top = Offset(w * .97f, h * .03f)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(
                            Color(0xFFD05F6C).copy(alpha = .25f),
                            Color(0xFF9E4C64).copy(alpha = .07f),
                            Color.Transparent
                        ), center = top, radius = w * .82f),
                        center = top, radius = w * .82f,
                    )
                    val bottom = Offset(w * .02f, h * .96f)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(
                            Color(0xFFE9AD78).copy(alpha = .15f),
                            Color.Transparent
                        ), center = bottom, radius = w * .72f),
                        center = bottom, radius = w * .72f,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = rose,
                            modifier = Modifier.size(17.dp))
                        Text("YOUR PERSONAL SPACE", color = gold,
                            fontSize = 10.sp, fontWeight = FontWeight.Black,
                            letterSpacing = 1.3.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(greeting + ".", color = ink, fontSize = 27.sp,
                            lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            modifier = Modifier.testTag("ux11e-personal-greeting"))
                        Text("A little room for what matters to you.",
                            color = soft, fontSize = 13.sp, lineHeight = 19.sp)
                        Text(day, color = gold.copy(alpha = .92f), fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        UX11ESanctuaryStat(
                            label = "PERSONAL ACTIVE",
                            value = snapshot.activeReminderCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        UX11ESanctuaryStat(
                            label = "DONE TODAY",
                            value = if (historyReady) snapshot.confirmedDoneToday.toString()
                                else "—",
                            detail = if (historyReady) null else "Unavailable",
                            modifier = Modifier.weight(1f)
                                .testTag("v288-today-confirmed-done"),
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("v288-next-action"),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xE626222B),
                        border = BorderStroke(1.dp,
                            if (attention) gold.copy(alpha = .75f)
                            else Color(0xFF73585C).copy(alpha = .53f)),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(15.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Icon(Icons.Outlined.CalendarToday, null, tint = gold,
                                    modifier = Modifier.size(16.dp))
                                Text(
                                    if (attention) "NEEDS ATTENTION" else "UP NEXT",
                                    color = gold, fontWeight = FontWeight.Black,
                                    fontSize = 10.sp, letterSpacing = .9.sp,
                                )
                            }
                            if (next == null) {
                                Text("Nothing waiting right now.", color = ink,
                                    fontSize = 16.sp, lineHeight = 22.sp,
                                    fontWeight = FontWeight.Bold)
                                Text("Your next Personal reminder will appear here when scheduled.",
                                    color = soft, fontSize = 12.sp, lineHeight = 18.sp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF583D4D),
                                        modifier = Modifier.size(43.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            V288PersonalReminderIcon(next, large = true)
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(next.title, color = ink, fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold, maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("v288-next-title"))
                                        val time = snapshot.nextAtMillis?.let {
                                            Instant.ofEpochMilli(it).atZone(next.zoneId)
                                                .format(DateTimeFormatter.ofPattern(
                                                    "EEE, d MMM · h:mm a", Locale.getDefault()
                                                ))
                                        } ?: "Check its schedule in your reminders"
                                        Text(time, color = soft, fontSize = 11.sp,
                                            lineHeight = 16.sp, maxLines = 2)
                                    }
                                }
                                if (V288PersonalTodayPolicy.canRespond(next)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                onRespond(next, PersonalDeliveryContract.ACTION_DONE)
                                            },
                                            enabled = !busy,
                                            modifier = Modifier.weight(1f)
                                                .heightIn(min = 50.dp)
                                                .testTag("v288-next-done"),
                                            border = BorderStroke(1.dp, rose.copy(alpha = .72f)),
                                            shape = RoundedCornerShape(13.dp),
                                        ) {
                                            Icon(Icons.Outlined.CheckCircle, null, tint = rose,
                                                modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text("DONE", color = ink, fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onRespond(next, PersonalDeliveryContract.ACTION_SNOOZE)
                                            },
                                            enabled = !busy,
                                            modifier = Modifier.weight(1f)
                                                .heightIn(min = 50.dp)
                                                .testTag("v288-next-snooze"),
                                            border = BorderStroke(1.dp, gold.copy(alpha = .55f)),
                                            shape = RoundedCornerShape(13.dp),
                                        ) {
                                            Icon(Icons.Outlined.Snooze, null, tint = gold,
                                                modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text("SNOOZE " + next.snoozeMinutes + "m",
                                                color = ink, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                } else if (attention) {
                                    Text("This reminder needs review. Android delivery " +
                                        "and response eligibility are checked separately.",
                                        color = soft, fontSize = 11.sp, lineHeight = 17.sp)
                                }
                            }
                        }
                    }
                    if (snapshot.attentionCount > 1) {
                        Text(snapshot.attentionCount.toString() +
                            " Personal reminders need attention. Review the agenda below.",
                            color = gold, fontSize = 11.sp,
                            modifier = Modifier.testTag("ux11e-attention-summary"))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onCreateReminder,
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                                .heightIn(min = 54.dp)
                                .testTag("ux11e-create-personal-reminder"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFED6769),
                                contentColor = Color(0xFF1B1017)),
                            shape = RoundedCornerShape(15.dp),
                        ) {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("REMINDER", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = onOpenSpaces,
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                                .heightIn(min = 54.dp)
                                .testTag("v289-open-spaces"),
                            border = BorderStroke(1.dp, Color(0xFF9A7D83)),
                            shape = RoundedCornerShape(15.dp),
                        ) {
                            Icon(Icons.Outlined.FolderOpen, null, tint = gold,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("MY SPACES", color = ink, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UX11ESanctuaryStat(
    label: String,
    value: String,
    modifier: Modifier,
    detail: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x802D2530),
        border = BorderStroke(1.dp, Color(0xFF84636A).copy(alpha = .29f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, color = ProjectorIvory, fontSize = 21.sp,
                fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = Color(0xFFE1C0B9), fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = .6.sp,
                maxLines = 1)
            if (detail != null) {
                Text(detail, color = MutedText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
internal fun UX11ESanctuarySection(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(top = 13.dp, bottom = 3.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title.uppercase(Locale.getDefault()),
            color = Color(0xFFE9B5A8), fontSize = 11.sp,
            letterSpacing = 1.sp, fontWeight = FontWeight.Black)
        Text(detail, color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
