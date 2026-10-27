package com.framebynavin.app.ui

import androidx.annotation.DrawableRes
import com.framebynavin.app.R

/**
 * V221.3 owner-review art studies. Nine visibly different vector drawings per
 * guide, deliberately NOT plugged into the real V144 character renderer until
 * the artwork is compared against approved original 3D concepts.
 */
internal enum class V2213Pose { IDLE,
    WELCOME,
    LISTEN,
    THINK,
    POINT,
    PRESENT,
    ENCOURAGE,
    CELEBRATE,
    REST }

internal object V2213PoseCatalog {
    const val REQUIRED_COUNT_PER_GUIDE = 9
    val studiedGuides = listOf(
        V144GuideIdentity.FRAME,
        V144GuideIdentity.NAVI,
        V144GuideIdentity.CUTE,
    )
    /** Spark's original five raster WebPs must not be touched or replaced. */
    @DrawableRes fun drawable(guide: V144GuideIdentity, pose: V2213Pose): Int? = when (guide) {
        V144GuideIdentity.FRAME -> when (pose) {
            V2213Pose.IDLE -> R.drawable.guide_v2213_frame_idle
            V2213Pose.WELCOME -> R.drawable.guide_v2213_frame_welcome
            V2213Pose.LISTEN -> R.drawable.guide_v2213_frame_listen
            V2213Pose.THINK -> R.drawable.guide_v2213_frame_think
            V2213Pose.POINT -> R.drawable.guide_v2213_frame_point
            V2213Pose.PRESENT -> R.drawable.guide_v2213_frame_present
            V2213Pose.ENCOURAGE -> R.drawable.guide_v2213_frame_encourage
            V2213Pose.CELEBRATE -> R.drawable.guide_v2213_frame_celebrate
            V2213Pose.REST -> R.drawable.guide_v2213_frame_rest
        }
        V144GuideIdentity.NAVI -> when (pose) {
            V2213Pose.IDLE -> R.drawable.guide_v2213_navi_idle
            V2213Pose.WELCOME -> R.drawable.guide_v2213_navi_welcome
            V2213Pose.LISTEN -> R.drawable.guide_v2213_navi_listen
            V2213Pose.THINK -> R.drawable.guide_v2213_navi_think
            V2213Pose.POINT -> R.drawable.guide_v2213_navi_point
            V2213Pose.PRESENT -> R.drawable.guide_v2213_navi_present
            V2213Pose.ENCOURAGE -> R.drawable.guide_v2213_navi_encourage
            V2213Pose.CELEBRATE -> R.drawable.guide_v2213_navi_celebrate
            V2213Pose.REST -> R.drawable.guide_v2213_navi_rest
        }
        V144GuideIdentity.CUTE -> when (pose) {
            V2213Pose.IDLE -> R.drawable.guide_v2213_cute_idle
            V2213Pose.WELCOME -> R.drawable.guide_v2213_cute_welcome
            V2213Pose.LISTEN -> R.drawable.guide_v2213_cute_listen
            V2213Pose.THINK -> R.drawable.guide_v2213_cute_think
            V2213Pose.POINT -> R.drawable.guide_v2213_cute_point
            V2213Pose.PRESENT -> R.drawable.guide_v2213_cute_present
            V2213Pose.ENCOURAGE -> R.drawable.guide_v2213_cute_encourage
            V2213Pose.CELEBRATE -> R.drawable.guide_v2213_cute_celebrate
            V2213Pose.REST -> R.drawable.guide_v2213_cute_rest
        }
        V144GuideIdentity.FUNNY -> null
    }

    /** Nine expressive intent states, not nine copies of one static image. */
    fun poseFor(state: CinePulseState): V2213Pose = when (state) {
        CinePulseState.IDLE -> V2213Pose.IDLE
        CinePulseState.WAVE, CinePulseState.WALK -> V2213Pose.WELCOME
        CinePulseState.LISTEN, CinePulseState.NOD -> V2213Pose.LISTEN
        CinePulseState.LOOK, CinePulseState.THINK -> V2213Pose.THINK
        CinePulseState.POINT -> V2213Pose.POINT
        CinePulseState.PRESENT -> V2213Pose.PRESENT
        CinePulseState.SUCCESS -> V2213Pose.ENCOURAGE
        CinePulseState.CELEBRATE -> V2213Pose.CELEBRATE
        CinePulseState.REST -> V2213Pose.REST
    }
}
