package com.framebynavin.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import com.framebynavin.app.R
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.widget.*

/** New widget family only. No legacy widget pinning, import or migration. */
internal enum class V392WidgetKind(
    val label: String,
    val description: String,
    val layout: Int,
) {
    TODAY("Today Hub", "Creator work and Personal reminders at a glance", R.layout.widget_v392_today_hub),
    NEXT("Next Reminder", "Your next reminder without exposing Personal titles", R.layout.widget_v392_next_reminder),
    CHALLENGE("Challenge Pulse", "See real 21-day progress, never auto-complete", R.layout.widget_v392_challenge_pulse);
}

internal fun V392WidgetKind.providerClass(): Class<out android.appwidget.AppWidgetProvider> =
    when(this) {
        V392WidgetKind.TODAY -> V392TodayHubWidgetProvider::class.java
        V392WidgetKind.NEXT -> V392NextReminderWidgetProvider::class.java
        V392WidgetKind.CHALLENGE -> V392ChallengePulseWidgetProvider::class.java
    }

class V392WidgetGalleryActivity : ComponentActivity() {
    private var pinned by mutableStateOf<Map<V392WidgetKind, Int>>(emptyMap())
    private var message by mutableStateOf<String?>(null)
    private val manager by lazy { AppWidgetManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refresh()
        setContent {
            FrameByNavinTheme {
                V392WidgetGalleryScreen(
                    pinned = pinned,
                    canPin = manager.isRequestPinAppWidgetSupported,
                    message = message,
                    onBack = { finish() },
                    onPin = ::requestPin,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        pinned = V392WidgetKind.entries.associateWith { kind ->
            runCatching { manager.getAppWidgetIds(ComponentName(this,kind.providerClass())).size }.getOrDefault(0)
        }
    }

    private fun requestPin(kind: V392WidgetKind) {
        if (!manager.isRequestPinAppWidgetSupported) {
            message = "Long-press your phone's Home screen, choose Widgets, then Backlot."
            return
        }
        val accepted = runCatching {
            manager.requestPinAppWidget(ComponentName(this,kind.providerClass()), null, null)
        }.getOrDefault(false)
        message = if (accepted)
            "Your launcher received the request. Confirm the placement on your Home screen."
        else "The launcher did not accept the request. Add Backlot from your phone's widget picker."
    }
}

@Composable
private fun V392WidgetGalleryScreen(
    pinned: Map<V392WidgetKind, Int>,
    canPin: Boolean,
    message: String?,
    onBack: () -> Unit,
    onPin: (V392WidgetKind) -> Unit,
) {
    Surface(color = CinemaBlack, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(horizontal=18.dp, vertical=15.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            TextButton(onClick=onBack) { Text("‹ BACK TO SETTINGS", color=ProjectorIvory) }
            Text("HOME SCREEN", color=MutedGold, fontWeight=FontWeight.Bold, fontSize=11.sp)
            Text("Backlot Widgets", color=ProjectorIvory, fontSize=27.sp, fontWeight=FontWeight.Bold)
            Text("Three focused widgets. Private Personal titles stay inside Backlot, and no widget can mark an activity Done for you.",
                color=MutedText, fontSize=13.sp, lineHeight=18.sp)
            for(kind in V392WidgetKind.entries) {
                Surface(color=CinemaSurface, shape=RoundedCornerShape(20.dp),
                    border=BorderStroke(1.dp,CinemaLine)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp),
                        verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text(kind.label,color=ProjectorIvory,fontSize=18.sp,fontWeight=FontWeight.Bold,
                                modifier=Modifier.weight(1f))
                            Text((pinned[kind] ?: 0).toString() + " PINNED",
                                color=MutedGold,fontSize=10.sp)
                        }
                        Text(kind.description,color=MutedText,fontSize=12.sp)
                        AndroidView(factory={ctx ->
                            FrameLayout(ctx).apply {
                                contentDescription = "Example " + kind.label + " widget, preview content only"
                                val views = RemoteViews(ctx.packageName,kind.layout)
                                when(kind) {
                                    V392WidgetKind.TODAY -> {
                                        views.setTextViewText(R.id.v392_today_date,"TODAY")
                                        views.setTextViewText(R.id.v392_today_headline,"Your day, in focus")
                                        views.setTextViewText(R.id.v392_today_creator,"CREATOR\n2 active · 1 due")
                                        views.setTextViewText(R.id.v392_today_personal,"PERSONAL\n3 reminders")
                                    }
                                    V392WidgetKind.NEXT -> {
                                        views.setTextViewText(R.id.v392_next_time,"4:30 PM")
                                        views.setTextViewText(R.id.v392_next_source,"Personal reminder")
                                    }
                                    V392WidgetKind.CHALLENGE -> {
                                        views.setTextViewText(R.id.v392_challenge_day,"Day 8 / 21")
                                        views.setTextViewText(R.id.v392_challenge_progress,"7 of 21 confirmed")
                                    }
                                }
                                val rendered = runCatching { views.apply(ctx,this) }.getOrElse {
                                    TextView(ctx).apply { text="Launcher widget preview unavailable." }
                                }
                                addView(rendered,FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))
                            }
                        },modifier=Modifier.fillMaxWidth().height(if(kind==V392WidgetKind.TODAY)165.dp else 155.dp)
                            .testTag("v392-widget-preview-"+kind.name.lowercase()))
                        Text("Example content · not your live data",color=MutedText,fontSize=10.sp)
                        Button(onClick={onPin(kind)},enabled=canPin,modifier=Modifier.fillMaxWidth().height(48.dp)
                            .testTag("v392-pin-"+kind.name.lowercase())) {
                            Text("ADD TO HOME",fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
            if(!canPin) Text("To add a widget, long-press your Android Home screen, open Widgets, find Backlot, and choose one.",
                color=MutedText,fontSize=12.sp)
            message?.let { Text(it,color=MutedGold,fontSize=12.sp) }
            Spacer(Modifier.height(12.dp))
        }
    }
}
