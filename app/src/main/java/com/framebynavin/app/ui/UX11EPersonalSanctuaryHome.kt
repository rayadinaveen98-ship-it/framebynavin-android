package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UX12 D4: compact, emotionally warm Personal Today presentation.
 * The existing read-only V288 snapshot and PersonalDeliveryCoordinator caller
 * remain authoritative; this composable adds no scheduler, storage or side effect.
 * The original V288/UX11E test tags and exact Done/Snooze action codes are retained.
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
    val ivory = Color(0xFFFFF4EB)
    val secondary = Color(0xFFCFC0C2)
    val accent = Color(0xFFE7BAA3)
    val next = snapshot.next
    val needsAttention = V288PersonalTodayPolicy.isNeedsAttention(next, nowMillis)
    val compactWidth = LocalConfiguration.current.screenWidthDp <= 360 ||
        LocalDensity.current.fontScale > 1.3f
    val day = snapshot.date.format(
        DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
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
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF20151F),
        border = BorderStroke(1.dp, Color(0xFF7B5864).copy(alpha = .26f)),
    ) {
        Box {
            // One static, inexpensive ambient surface. No perpetual GPU animation.
            Canvas(Modifier.matchParentSize()) {
                if (size.minDimension <= 0f) return@Canvas
                drawRect(Brush.verticalGradient(
                    listOf(Color(0xFF251821), Color(0xFF1B161F))
                ))
                val center = Offset(size.width * .94f, size.height * .05f)
                val radius = size.width * .88f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFB66472).copy(alpha = .15f), Color.Transparent),
                        center = center, radius = radius,
                    ),
                    center = center, radius = radius,
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("PERSONAL", color = accent, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.15.sp)
                    Text(day, color = secondary, fontSize = 11.sp, maxLines = 1)
                }
                Text(
                    "$greeting.",
                    color = ivory, fontSize = 22.sp, lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2,
                    modifier = Modifier.testTag("ux11e-personal-greeting"),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${snapshot.activeReminderCount} active reminders",
                        color = secondary, fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                    )
                    Text(
                        if (historyReady) "${snapshot.confirmedDoneToday} done today"
                        else "Progress unavailable",
                        color = secondary, fontSize = 11.sp,
                        modifier = Modifier.testTag("v288-today-confirmed-done"),
                        maxLines = 2,
                    )
                }

                HorizontalDivider(color = accent.copy(alpha = .15f))
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("v288-next-action"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (needsAttention) "NEEDS ATTENTION" else "UP NEXT",
                        color = accent, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = .8.sp,
                    )
                    if (next == null) {
                        // A small editorial cameo only in the empty summary.
                        // The toolbar retains its separate face-only avatar.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            V144GuideCharacter(
                                guide = V144GuidePrefs.currentGuide,
                                state = CinePulseState.THINK,
                                modifier = Modifier.size(62.dp),
                            )
                            Text(
                                if (snapshot.activeReminderCount == 0)
                                    "Start with one small reminder."
                                else "Nothing due next.",
                                color = ivory, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            V288PersonalReminderIcon(next)
                            Column(Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    next.title, color = ivory,
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("v288-next-title"),
                                )
                                Text(
                                    snapshot.nextAtMillis?.let {
                                        Instant.ofEpochMilli(it).atZone(next.zoneId)
                                            .format(DateTimeFormatter.ofPattern(
                                                "EEE, d MMM · h:mm a", Locale.getDefault()
                                            ))
                                    } ?: "Check schedule in your reminders",
                                    color = secondary, fontSize = 11.sp,
                                    maxLines = 2,
                                )
                            }
                        }
                        if (V288PersonalTodayPolicy.canRespond(next)) {
                            // Never offer Done/Snooze for a future/unpresented lease.
                            UX12D4EligibleResponses(
                                reminder = next,
                                busy = busy,
                                stacked = compactWidth,
                                onRespond = onRespond,
                            )
                        } else if (needsAttention) {
                            Text(
                                "Open the reminder below to review its response status.",
                                color = secondary, fontSize = 11.sp, lineHeight = 16.sp,
                            )
                        }
                    }
                }

                if (snapshot.attentionCount > 1) {
                    Text(
                        "${snapshot.attentionCount} reminders need attention.",
                        color = accent, fontSize = 11.sp,
                        modifier = Modifier.testTag("ux11e-attention-summary"),
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Button(
                        onClick = onCreateReminder,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .testTag("ux11e-create-personal-reminder"),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF18B82),
                            contentColor = Color(0xFF261417),
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2)
                    }
                    OutlinedButton(
                        onClick = onOpenSpaces,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .testTag("v289-open-spaces"),
                        border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.FolderOpen, null, tint = accent,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("My Spaces", color = ivory, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 2)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun UX12D4EligibleResponses(
    reminder: PersonalReminder,
    busy: Boolean,
    stacked: Boolean,
    onRespond: (PersonalReminder, String) -> Unit,
) {
    // Both buttons retain the exact old contract and test tags.
    if (stacked) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            UX12D4DoneButton(reminder, busy, onRespond, Modifier.fillMaxWidth())
            UX12D4SnoozeButton(reminder, busy, onRespond, Modifier.fillMaxWidth())
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UX12D4DoneButton(reminder, busy, onRespond, Modifier.weight(1f))
            UX12D4SnoozeButton(reminder, busy, onRespond, Modifier.weight(1f))
        }
    }
}

@Composable
private fun UX12D4DoneButton(
    reminder: PersonalReminder,
    busy: Boolean,
    onRespond: (PersonalReminder, String) -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = { onRespond(reminder, PersonalDeliveryContract.ACTION_DONE) },
        enabled = !busy,
        modifier = modifier.heightIn(min = 48.dp).testTag("v288-next-done"),
        border = BorderStroke(1.dp, Color(0xFFF1A6A0).copy(alpha = .6f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFFF1B7AA),
            modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text("Done", color = Color(0xFFFFF4EB), fontSize = 12.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UX12D4SnoozeButton(
    reminder: PersonalReminder,
    busy: Boolean,
    onRespond: (PersonalReminder, String) -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = { onRespond(reminder, PersonalDeliveryContract.ACTION_SNOOZE) },
        enabled = !busy,
        modifier = modifier.heightIn(min = 48.dp).testTag("v288-next-snooze"),
        border = BorderStroke(1.dp, Color(0xFFE7BAA3).copy(alpha = .3f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.Outlined.Snooze, null, tint = Color(0xFFE7BAA3),
            modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text("Snooze ${reminder.snoozeMinutes}m",
            color = Color(0xFFFFF4EB), fontSize = 12.sp, maxLines = 2)
    }
}

/** A compact section label. The optional detail is shown only when useful. */
@Composable
internal fun UX11ESanctuarySection(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 9.dp, bottom = 1.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title.uppercase(Locale.getDefault()),
            color = Color(0xFFE9B5A8), fontSize = 11.sp,
            letterSpacing = .9.sp, fontWeight = FontWeight.Bold)
        if (detail.isNotBlank()) {
            Text(detail, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
