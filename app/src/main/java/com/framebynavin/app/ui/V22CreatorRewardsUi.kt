package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.CreatorAchievementSnapshot
import com.framebynavin.app.data.CreatorRewardLedgerEntry
import com.framebynavin.app.data.CreatorRewardProgressSnapshot
import com.framebynavin.app.ui.theme.*

@Composable
internal fun V22CreatorProgressCard(
    snapshot: CreatorRewardProgressSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .25f)),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CREATOR PROGRESS", color = MutedGold, fontSize = 8.4.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("Level ${snapshot.level}", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Text("${snapshot.totalXp} XP", color = MutedGold, fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { snapshot.levelProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MutedGold,
                trackColor = CinemaLine,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bolt, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("${snapshot.weeklyMomentum} Momentum this week", color = ProjectorIvory, fontSize = 9.3.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${snapshot.unlockedAchievementCount}/${snapshot.achievements.size} achievements", color = MutedText, fontSize = 8.1.sp)
            }
            snapshot.nextAchievement?.let { next ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "Next · ${next.title} · ${next.current}/${next.target}",
                    color = MutedText,
                    fontSize = 8.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun V22WeeklyMomentumStrip(snapshot: CreatorRewardProgressSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bolt, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("WEEKLY MOMENTUM", color = MutedText, fontSize = 7.8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                Text("${snapshot.weeklyEventCount} rewarded action${if (snapshot.weeklyEventCount == 1) "" else "s"} this week", color = MutedText, fontSize = 8.4.sp)
            }
            Text(snapshot.weeklyMomentum.toString(), color = SuccessGreen, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun V22RewardToast(entry: CreatorRewardLedgerEntry, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 330.dp),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurfaceRaised,
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .38f)),
        shadowElevation = 10.dp,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MutedGold.copy(alpha = .13f), modifier = Modifier.size(35.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Star, null, tint = MutedGold, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("+${entry.xp} XP · +${entry.momentum} Momentum", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(entry.label, color = MutedText, fontSize = 8.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun V22CreatorProgressDialog(
    snapshot: CreatorRewardProgressSnapshot,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = CinemaSurfaceRaised,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Column(Modifier.heightIn(max = 700.dp).verticalScroll(rememberScrollState()).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("CREATOR PROGRESS", color = MutedGold, fontSize = 8.7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text("Level ${snapshot.level}", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory) }
                }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V22Metric("LIFETIME XP", snapshot.totalXp.toString(), MutedGold, Modifier.weight(1f))
                    V22Metric("THIS WEEK", snapshot.weeklyMomentum.toString(), SuccessGreen, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { snapshot.levelProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MutedGold,
                    trackColor = CinemaLine,
                )
                Spacer(Modifier.height(5.dp))
                Text("${snapshot.xpToNextLevel} XP to Level ${snapshot.level + 1}", color = MutedText, fontSize = 8.4.sp)

                Spacer(Modifier.height(20.dp))
                Text("ACHIEVEMENTS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                snapshot.achievements.forEach { achievement ->
                    V22AchievementRow(achievement)
                    Spacer(Modifier.height(7.dp))
                }

                Spacer(Modifier.height(12.dp))
                Text("CREATOR TRAITS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("Derived only from actions recorded in this app.", color = MutedText, fontSize = 8.3.sp)
                Spacer(Modifier.height(8.dp))
                if (snapshot.traits.isEmpty()) {
                    Text("Traits appear as you capture ideas, build projects, publish and review your work.", color = MutedText, fontSize = 9.2.sp, lineHeight = 13.sp)
                } else {
                    snapshot.traits.forEach { trait ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                            shape = RoundedCornerShape(15.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(trait.title, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.weight(1f))
                                    Text(trait.level.uppercase(), color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(trait.body, color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(13.dp))
                Text("RECENT REWARDS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                if (snapshot.recentRewards.isEmpty()) {
                    Text("No reward activity yet.", color = MutedText, fontSize = 9.sp)
                } else {
                    snapshot.recentRewards.forEach { reward ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bolt, null, tint = SuccessGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(reward.label, color = ProjectorIvory, fontSize = 9.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("+${reward.xp} XP", color = MutedGold, fontSize = 8.7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V22AchievementRow(achievement: CreatorAchievementSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, if (achievement.unlocked) MutedGold.copy(alpha = .30f) else CinemaLine),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (achievement.unlocked) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock,
                null,
                tint = if (achievement.unlocked) MutedGold else MutedText,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(achievement.title, color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(achievement.description, color = MutedText, fontSize = 8.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (achievement.unlocked) {
                Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(17.dp))
            } else {
                Text("${achievement.current}/${achievement.target}", color = MutedText, fontSize = 8.3.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V22Metric(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = MutedText, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun V23CreatorProgressScreen(
    snapshot: CreatorRewardProgressSnapshot,
    onClose: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory) }
                Column(Modifier.weight(1f)) {
                    Text("PROGRESS", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Text("Creator Progress", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = CinemaSurface,
                border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f)),
            ) {
                Column(Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("CREATOR LEVEL", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                            Text("Level ${snapshot.level}", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
                        }
                        Text("${snapshot.totalXp} XP", color = MutedGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(9.dp))
                    LinearProgressIndicator(
                        progress = { snapshot.levelProgress },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = MutedGold,
                        trackColor = CinemaLine,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("${snapshot.xpToNextLevel} XP to Level ${snapshot.level + 1}", color = MutedText, fontSize = 8.6.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V22Metric("THIS WEEK", snapshot.weeklyMomentum.toString(), SuccessGreen, Modifier.weight(1f))
                V22Metric("REWARDED ACTIONS", snapshot.weeklyEventCount.toString(), MutedGold, Modifier.weight(1f))
            }

            snapshot.nextAchievement?.let { next ->
                Spacer(Modifier.height(18.dp))
                Text("NEXT MILESTONE", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.EmojiEvents, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(next.title, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            Text("${next.current}/${next.target}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(next.description, color = MutedText, fontSize = 8.6.sp, lineHeight = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("ACHIEVEMENTS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("${snapshot.unlockedAchievementCount}/${snapshot.achievements.size} unlocked", color = MutedText, fontSize = 8.4.sp)
            Spacer(Modifier.height(8.dp))
            snapshot.achievements.forEach { achievement ->
                V22AchievementRow(achievement)
                Spacer(Modifier.height(7.dp))
            }

            Spacer(Modifier.height(13.dp))
            Text("CREATOR TRAITS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("Earned from recorded creator behavior, never from opening the app.", color = MutedText, fontSize = 8.3.sp)
            Spacer(Modifier.height(8.dp))
            if (snapshot.traits.isEmpty()) {
                Text("Traits appear as you capture ideas, build projects, publish and review your work.", color = MutedText, fontSize = 9.2.sp, lineHeight = 13.sp)
            } else {
                snapshot.traits.forEach { trait ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(trait.title, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.weight(1f))
                                Text(trait.level.uppercase(), color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(trait.body, color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(13.dp))
            Text("RECENT REWARDS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("Every XP change stays explainable.", color = MutedText, fontSize = 8.3.sp)
            Spacer(Modifier.height(8.dp))
            if (snapshot.recentRewards.isEmpty()) {
                Text("No reward activity yet.", color = MutedText, fontSize = 9.sp)
            } else {
                snapshot.recentRewards.forEach { reward ->
                    Surface(Modifier.fillMaxWidth().padding(bottom = 6.dp), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bolt, null, tint = SuccessGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(reward.label, color = ProjectorIvory, fontSize = 9.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("+${reward.xp} XP", color = MutedGold, fontSize = 8.7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
