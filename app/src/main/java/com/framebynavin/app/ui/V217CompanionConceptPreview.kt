package com.framebynavin.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.framebynavin.app.R

/**
 * V217 OWNER-REVIEW ONLY. These are separate alpha-transparent concept cutouts,
 * NOT an approved 12-state production pose pack.
 *
 * This composable is not called by V144GuideCharacter, onboarding, Settings,
 * Daily Brief or alarm/voice UI. Preview experiments must not silently change
 * existing companion choices or their persistent IDs.
 */
@Composable
internal fun V217CompanionConceptPreview(
    identity: V144GuideIdentity,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(v217ConceptDrawable(identity)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/**
 * FUNNY/Spark intentionally uses its unmodified real resource, never an AI
 * approximation. This code is review-only, not active app routing.
 */
@DrawableRes
internal fun v217ConceptDrawable(identity: V144GuideIdentity): Int = when (identity) {
    V144GuideIdentity.FRAME -> R.drawable.guide_v217_frame_concept
    V144GuideIdentity.NAVI -> R.drawable.guide_v217_navi_concept
    V144GuideIdentity.CUTE -> R.drawable.guide_v217_cute_concept
    V144GuideIdentity.FUNNY -> R.drawable.guide_funny_welcome
}
