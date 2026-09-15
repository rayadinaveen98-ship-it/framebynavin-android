package com.backlot.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(width = 1440.dp, height = 900.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Backlot",
        state = windowState,
    ) {
        BacklotDesktopApp()
    }
}
