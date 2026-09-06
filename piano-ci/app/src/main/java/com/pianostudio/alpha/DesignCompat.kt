package com.pianostudio.alpha

import androidx.compose.runtime.Composable
import com.pianostudio.core.designsystem.PianoStudioTheme
import com.pianostudio.core.designsystem.StudioColors

// Temporary compatibility facade while feature screens migrate to the production design system.
// There is one source of truth: core:designsystem.
val StudioBlack = StudioColors.Ink
val StudioCharcoal = StudioColors.InkRaised
val StudioCarbon = StudioColors.Surface
val StudioIvory = StudioColors.Ivory
val StudioWhite = StudioColors.IvoryStrong
val StudioGold = StudioColors.Champagne
val StudioGoldSoft = StudioColors.ChampagneSoft
val StudioMuted = StudioColors.Muted
val StudioSuccess = StudioColors.Success
val StudioError = StudioColors.Error

val R2Black = StudioColors.Ink
val R2Charcoal = StudioColors.InkRaised
val R2Carbon = StudioColors.Surface
val R2Raised = StudioColors.SurfaceRaised
val R2Ivory = StudioColors.Ivory
val R2White = StudioColors.IvoryStrong
val R2Gold = StudioColors.Champagne
val R2Muted = StudioColors.Muted
val R2Subtle = StudioColors.Subtle
val R2Success = StudioColors.Success
val R2Error = StudioColors.Error
val R2Amber = StudioColors.Warning

@Composable
fun QuietConcertStudioTheme(content: @Composable () -> Unit) = PianoStudioTheme(content)

@Composable
fun R2Theme(content: @Composable () -> Unit) = PianoStudioTheme(content)
