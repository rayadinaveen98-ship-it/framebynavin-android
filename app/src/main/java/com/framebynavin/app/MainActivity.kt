package com.framebynavin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.framebynavin.app.ui.FrameByNavinApp
import com.framebynavin.app.ui.theme.FrameByNavinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrameByNavinTheme {
                FrameByNavinApp()
            }
        }
    }
}
