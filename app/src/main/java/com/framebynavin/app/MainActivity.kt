package com.framebynavin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.framebynavin.app.reminders.ReminderNotifications
import com.framebynavin.app.ui.FrameByNavinV10App
import com.framebynavin.app.ui.theme.FrameByNavinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNotifications.ensureChannel(this)
        enableEdgeToEdge()
        setContent {
            FrameByNavinTheme {
                FrameByNavinV10App()
            }
        }
    }
}
