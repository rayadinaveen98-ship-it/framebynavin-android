package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*

/**
 * REVIEW-ONLY gallery, never invoked by Home/onboarding/Settings or V144.
 * Compare the approved original 3D concept with nine distinct hand-drawn
 * vector studies before any possible visual replacement.
 */
@Composable
internal fun V2213ExpressionArtReview(guide: V144GuideIdentity) {
    require(guide != V144GuideIdentity.FUNNY) { "Original Spark art stays untouched" }
    Surface(
        modifier = Modifier.fillMaxSize().testTag("v2213-review-${guide.name}"),
        color = CinemaBlack,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "BACKLOT · V221.3 ART REVIEW",
                fontSize = 11.sp, letterSpacing = 1.4.sp,
                color = MutedGold, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${guide.displayName} — nine expression studies",
                fontSize = 18.sp, color = ProjectorIvory,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Vector artwork is not live. Original production character remains unchanged.",
                fontSize = 10.sp, color = MutedText,
                lineHeight = 14.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                color = CinemaSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CinemaLine),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(v217ConceptDrawable(guide)),
                        contentDescription = null,
                        modifier = Modifier.size(92.dp).testTag("v2213-original"),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("APPROVED BASE REFERENCE",fontSize=10.sp,
                            color=MutedGold,fontWeight=FontWeight.Bold)
                        Text("3D-style concept preserved in real Backlot.",
                            fontSize=10.sp,color=ProjectorIvory,lineHeight=14.sp)
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            V2213Pose.entries.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    row.forEach { pose ->
                        val drawable = requireNotNull(V2213PoseCatalog.drawable(guide, pose))
                        Surface(
                            modifier = Modifier.weight(1f)
                                .testTag("v2213-pose-${guide.name}-${pose.name}"),
                            color = CinemaSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(
                                modifier = Modifier.padding(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Image(
                                    painter = painterResource(drawable),
                                    contentDescription = pose.name.lowercase(),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth().height(95.dp)
                                        .testTag("v2213-pose-art-${guide.name}-${pose.name}"),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    pose.name.lowercase().replaceFirstChar { it.titlecase() },
                                    fontSize = 9.sp, color = ProjectorIvory, maxLines = 1,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }
            Text(
                "OWNER REVIEW GATE · NOT AN APPROVED REPLACEMENT",
                fontSize = 9.sp, color = MutedText, letterSpacing = .8.sp,
            )
        }
    }
}
