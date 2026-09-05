package com.framebynavin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.ProjectorIvory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * v1.7.4 cinematic cold-launch ident.
 * Fresh film-gate / projector-aperture language: no reuse of the prior layered-card mark.
 */
@Composable
internal fun V174CinematicWelcome() {
    val aperture = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val beamTravel = remember { Animatable(0f) }
    val flare = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { beamTravel.animateTo(1f, tween(2_650, easing = LinearEasing)) }
        delay(120)
        aperture.animateTo(1f, tween(820, easing = FastOutSlowInEasing))
        delay(90)
        launch { title.animateTo(1f, tween(620, easing = LinearOutSlowInEasing)) }
        delay(590)
        flare.animateTo(1f, tween(180, easing = LinearOutSlowInEasing))
        flare.animateTo(.18f, tween(520, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier.fillMaxSize().background(
  Brush.verticalGradient(
      listOf(Color(0xFF030304), Color(0xFF08080B), CinemaBlack)
  )
        )
    ) {
        Canvas(Modifier.fillMaxSize().alpha(.46f)) {
  repeat(70) { i ->
      val x = (((i * 47) % 101) / 101f) * size.width
      val y = (((i * 73 + 19) % 103) / 103f) * size.height
      val a = .018f + ((i % 5) * .006f)
      drawCircle(
          color = Color.White.copy(alpha = a),
          radius = if (i % 4 == 0) 1.15f else .62f,
          center = Offset(x, y),
      )
  }
        }

        Box(
  Modifier.align(Alignment.Center)
      .fillMaxWidth()
      .height((2f + 116f * aperture.value).dp)
      .background(
          Brush.horizontalGradient(
              listOf(
                  Color(0xFF180306),
                  Color(0xFF5B090D),
                  Color(0xFFD01916),
                  Color(0xFFFF7B30),
                  Color(0xFFB30F14),
                  Color(0xFF260307),
              )
          )
      )
        )

        Box(
  Modifier.align(Alignment.Center)
      .offset(x = (-178f + 356f * beamTravel.value).dp)
      .size(width = 58.dp, height = (18f + 126f * aperture.value).dp)
      .alpha(.12f + .26f * aperture.value)
      .background(
          Brush.horizontalGradient(
              listOf(Color.Transparent, Color(0xFFFFE1A6), Color.Transparent)
          )
      )
        )

        Box(
  Modifier.align(Alignment.Center)
      .size((120f + 230f * flare.value).dp)
      .alpha(.18f * flare.value)
      .background(
          Brush.radialGradient(
              listOf(Color(0xFFFFC06B), Color(0xFFE62A21), Color.Transparent)
          ),
          CircleShape,
      )
        )

        Text(
  "FRAME BY NAVIN",
  modifier = Modifier.align(Alignment.Center)
      .alpha(title.value)
      .graphicsLayer {
          scaleX = .94f + (.06f * title.value)
          scaleY = .94f + (.06f * title.value)
          translationY = 10f * (1f - title.value)
      },
  color = ProjectorIvory,
  fontSize = 26.sp,
  lineHeight = 31.sp,
  fontWeight = FontWeight.Black,
  letterSpacing = 3.7.sp,
  textAlign = TextAlign.Center,
        )

        Box(
  Modifier.align(Alignment.Center)
      .fillMaxWidth(.54f)
      .height(1.dp)
      .offset(y = 48.dp)
      .alpha(.44f * title.value)
      .background(
          Brush.horizontalGradient(
              listOf(Color.Transparent, Color(0xFFFF6A38), Color.Transparent)
          )
      )
        )
    }
}
