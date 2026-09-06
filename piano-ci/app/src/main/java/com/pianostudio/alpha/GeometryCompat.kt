package com.pianostudio.alpha

import androidx.compose.ui.geometry.Size

/** Small geometry helper used by the production piano key renderer. */
operator fun Size.minus(other: Size): Size = Size(
    width = (width - other.width).coerceAtLeast(0f),
    height = (height - other.height).coerceAtLeast(0f),
)
