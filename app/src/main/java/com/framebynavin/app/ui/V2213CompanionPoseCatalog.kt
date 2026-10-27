package com.framebynavin.app.ui

import com.framebynavin.app.R

/**
 * V221.3 independent VECTOR ART CONCEPT catalog.
 *
 * These 27 individually authored resources are staged artwork pending owner
 * signoff against the existing V217 transparent WebPs. The real
 * V144GuideCharacter call site continues to use the approved V217 WebPs.
 * Do NOT replace the original Spark/FUNNY renderer or its WebP assets.
 */
internal enum class V2213Pose(val label: String) {
    WELCOME("Welcome"),
    LISTEN("Listen"),
    THINK("Think"),
    POINT("Point"),
    ENCOURAGE("Encourage"),
    PRESENT("Present"),
    CELEBRATE("Celebrate"),
    REST("Rest"),
    MOVE("Move"),
}

internal object V2213PoseCatalog {
    val reviewIdentities = listOf(
        V144GuideIdentity.FRAME, V144GuideIdentity.NAVI, V144GuideIdentity.CUTE,
    )

    /** All 12 legacy CinePulse semantics are normalized to 9 real art states. */
    fun pose(state: CinePulseState): V2213Pose = when (state) {
        CinePulseState.WAVE -> V2213Pose.WELCOME
        CinePulseState.LISTEN -> V2213Pose.LISTEN
        CinePulseState.THINK, CinePulseState.LOOK -> V2213Pose.THINK
        CinePulseState.POINT -> V2213Pose.POINT
        CinePulseState.NOD -> V2213Pose.ENCOURAGE
        CinePulseState.PRESENT -> V2213Pose.PRESENT
        CinePulseState.SUCCESS, CinePulseState.CELEBRATE -> V2213Pose.CELEBRATE
        CinePulseState.IDLE, CinePulseState.REST -> V2213Pose.REST
        CinePulseState.WALK -> V2213Pose.MOVE
    }

    /**
     * Only review identities are accepted: Spark always uses original raster
     * guide assets and never participates in an art-replacement experiment.
     */
    fun conceptDrawable(identity: V144GuideIdentity, pose: V2213Pose): Int =
        when (identity) {
            V144GuideIdentity.FRAME -> when (pose) {
                V2213Pose.WELCOME -> R.drawable.guide_v2213_frame_welcome
                V2213Pose.LISTEN -> R.drawable.guide_v2213_frame_listen
                V2213Pose.THINK -> R.drawable.guide_v2213_frame_think
                V2213Pose.POINT -> R.drawable.guide_v2213_frame_point
                V2213Pose.ENCOURAGE -> R.drawable.guide_v2213_frame_encourage
                V2213Pose.PRESENT -> R.drawable.guide_v2213_frame_present
                V2213Pose.CELEBRATE -> R.drawable.guide_v2213_frame_celebrate
                V2213Pose.REST -> R.drawable.guide_v2213_frame_rest
                V2213Pose.MOVE -> R.drawable.guide_v2213_frame_move
            }
            V144GuideIdentity.NAVI -> when (pose) {
                V2213Pose.WELCOME -> R.drawable.guide_v2213_navi_welcome
                V2213Pose.LISTEN -> R.drawable.guide_v2213_navi_listen
                V2213Pose.THINK -> R.drawable.guide_v2213_navi_think
                V2213Pose.POINT -> R.drawable.guide_v2213_navi_point
                V2213Pose.ENCOURAGE -> R.drawable.guide_v2213_navi_encourage
                V2213Pose.PRESENT -> R.drawable.guide_v2213_navi_present
                V2213Pose.CELEBRATE -> R.drawable.guide_v2213_navi_celebrate
                V2213Pose.REST -> R.drawable.guide_v2213_navi_rest
                V2213Pose.MOVE -> R.drawable.guide_v2213_navi_move
            }
            V144GuideIdentity.CUTE -> when (pose) {
                V2213Pose.WELCOME -> R.drawable.guide_v2213_cute_welcome
                V2213Pose.LISTEN -> R.drawable.guide_v2213_cute_listen
                V2213Pose.THINK -> R.drawable.guide_v2213_cute_think
                V2213Pose.POINT -> R.drawable.guide_v2213_cute_point
                V2213Pose.ENCOURAGE -> R.drawable.guide_v2213_cute_encourage
                V2213Pose.PRESENT -> R.drawable.guide_v2213_cute_present
                V2213Pose.CELEBRATE -> R.drawable.guide_v2213_cute_celebrate
                V2213Pose.REST -> R.drawable.guide_v2213_cute_rest
                V2213Pose.MOVE -> R.drawable.guide_v2213_cute_move
            }
            V144GuideIdentity.FUNNY ->
                error("Spark's approved original artwork is intentionally excluded")
        }
}
