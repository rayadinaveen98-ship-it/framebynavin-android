package com.framebynavin.app.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*

@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        RecRed.copy(alpha = 0.07f),
                        CinemaBlack
                    ),
                    radius = 900f
                )
            )
            .background(CinemaBlack.copy(alpha = 0.90f))
    ) {
        FilmRail(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 88.dp)
        ) {
            TopBar()
            Spacer(Modifier.height(14.dp))
            EditorialHero()
            Spacer(Modifier.height(18.dp))
            PublishCard()
            Spacer(Modifier.height(14.dp))
            CurrentTaskCard()
            Spacer(Modifier.height(14.dp))
            NextActionRow()
            Spacer(Modifier.height(14.dp))
            FocusButton()
            Spacer(Modifier.height(14.dp))
            WeeklyStrip()
        }

        BottomNav(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(RecRed, CircleShape)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "FRAMEBYNAVIN",
                color = ProjectorIvory,
                fontSize = 13.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            onClick = { },
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = CinemaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "Quick add",
                tint = ProjectorIvory,
                modifier = Modifier.padding(9.dp)
            )
        }
    }
}

@Composable
private fun EditorialHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = RecRed,
                    topLeft = Offset(0f, 17f),
                    size = Size(3.dp.toPx(), 103.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
            }
            .padding(start = 16.dp)
    ) {
        Text(
            "THURSDAY · FRAME BREAKDOWN",
            color = MutedText,
            fontSize = 10.sp,
            letterSpacing = 1.35.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(9.dp))

        Text(
            text = "MAKE\nTHE FRAME\nCOUNT.",
            color = ProjectorIvory,
            fontSize = 40.sp,
            lineHeight = 38.sp,
            letterSpacing = (-1.35).sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "One clear priority. Everything else waits.",
            color = MutedText,
            fontSize = 12.5.sp
        )
    }
}

@Composable
private fun PublishCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)
    ) {
        Box {
            ApertureMotif(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(116.dp)
            )

            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(RecRed, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "PUBLISH · 7:00 PM",
                        color = RecRed,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.25.sp
                    )
                }

                Spacer(Modifier.height(7.dp))

                Text(
                    "Frame Breakdown",
                    color = ProjectorIvory,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Instagram Reel · Important",
                    color = MutedText,
                    fontSize = 11.5.sp
                )

                Spacer(Modifier.height(13.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StatusChip("EDITING", active = true)
                    StatusChip("72% DONE")
                    StatusChip("SMART ESC.")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, active: Boolean = false) {
    val bg = if (active) RecRedDeep else Color(0xFF111111)
    val fg = if (active) Color(0xFFFFD1CE) else Color(0xFFAAA49D)
    val stroke = if (active) Color(0xFF542424) else CinemaLine

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, stroke)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CurrentTaskCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = CinemaSurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242424))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "CURRENT TASK",
                color = Color(0xFF77726C),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "Finish the edit",
                color = ProjectorIvory,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("42 min remaining", color = MutedGold, fontSize = 11.sp)
                Text("72%", color = ProjectorIvory, fontSize = 11.sp)
            }

            Spacer(Modifier.height(9.dp))

            LinearProgressIndicator(
                progress = { 0.72f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = RecRed,
                trackColor = Color(0xFF303030),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun NextActionRow() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xDD0D0D0D),
            border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "NEXT",
                    color = Color(0xFF77726C),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.15.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Export → Final QC",
                    color = ProjectorIvory,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Recommended by 6:35 PM",
                    color = MutedText,
                    fontSize = 10.5.sp
                )
            }
        }

        Surface(
            modifier = Modifier.width(84.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF17140E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3122))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "BUFFER",
                    color = Color(0xFF948978),
                    fontSize = 8.5.sp,
                    letterSpacing = 0.8.sp
                )
                Text(
                    "25m",
                    color = MutedGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FocusButton() {
    Button(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "ENTER FOCUS MODE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun WeeklyStrip() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF817B74),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "WEEK 01 · 3 OF 5 PUBLISHED",
            color = Color(0xFF817B74),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.85.sp
        )
        Spacer(Modifier.weight(1f))
        Text(
            "60%",
            color = ProjectorIvory,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ApertureMotif(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val radii = listOf(size.minDimension * 0.24f, size.minDimension * 0.37f, size.minDimension * 0.49f)
        radii.forEachIndexed { index, radius ->
            drawCircle(
                color = RecRed.copy(alpha = 0.22f - index * 0.045f),
                radius = radius,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        drawArc(
            color = RecRed.copy(alpha = 0.45f),
            startAngle = -35f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(c.x - radii[1], c.y - radii[1]),
            size = Size(radii[1] * 2, radii[1] * 2),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun FilmRail(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 8.dp, height = 410.dp)) {
        val holeW = 3.dp.toPx()
        val holeH = 8.dp.toPx()
        val gap = 9.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(size.width - holeW, y),
                size = Size(holeW, holeH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
            y += holeH + gap
        }
    }
}

@Composable
private fun BottomNav(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1C1C)),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem("TODAY", Icons.Outlined.MovieCreation, active = true)
            NavItem("PLAN", Icons.Outlined.CalendarMonth)
            NavItem("STUDIO", Icons.Outlined.Tune)
            NavItem("INSIGHTS", Icons.Outlined.Insights)
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean = false
) {
    val fg = if (active) ProjectorIvory else Color(0xFF74706A)
    Column(
        modifier = Modifier.width(68.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = fg,
            fontSize = 8.5.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.55.sp
        )
        if (active) {
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .size(4.dp)
                    .background(RecRed, CircleShape)
            )
        }
    }
}

@Preview(
    name = "FrameByNavin A2 Today",
    showBackground = true,
    backgroundColor = 0xFF070707,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun TodayScreenPreview() {
    FrameByNavinTheme {
        TodayScreen()
    }
}
