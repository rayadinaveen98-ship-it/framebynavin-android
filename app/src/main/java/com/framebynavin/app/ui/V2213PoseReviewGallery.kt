package com.framebynavin.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory

/**
 * Standalone review-only art gallery. NOT mounted in Backlot navigation,
 * onboarding, Settings, or production companion renderers.
 */
@Composable
internal fun V2213PoseReviewGallery(
    identity: V144GuideIdentity,
    modifier: Modifier = Modifier,
) {
    require(identity != V144GuideIdentity.FUNNY) {
        "Spark's five original WebP assets are not part of the V221.3 review"
    }
    Column(
        modifier = modifier.fillMaxWidth()
            .testTag("v2213-gallery-${identity.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "${identity.displayName.uppercase()} · NINE POSE CONCEPTS",
            color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        )
        Text(
            text = "REVIEW ONLY — approved V217 art remains in the live app",
            color = ProjectorIvory, fontSize = 9.sp,
        )
        V2213Pose.entries.toList().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { pose ->
                    Surface(
                        modifier = Modifier.weight(1f)
                            .testTag("v2213-pose-${identity.name.lowercase()}-${pose.name.lowercase()}"),
                        color = CinemaSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Column(
                            Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                color = CinemaSurfaceRaised,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(68.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(
                                            V2213PoseCatalog.conceptDrawable(identity, pose)),
                                        contentDescription = "${identity.displayName} ${pose.label} concept pose",
                                        modifier = Modifier.size(66.dp),
                                    )
                                }
                            }
                            Text(
                                text = pose.label,
                                color = ProjectorIvory,
                                textAlign = TextAlign.Center,
                                fontSize = 9.sp,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
