package com.framebynavin.app.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.framebynavin.app.R
import com.framebynavin.app.widget.V2273WidgetKind

/** The same layout resources declared by Backlot's three widget providers. */
internal object V275WidgetPreviewSpec {
    fun layout(kind: V2273WidgetKind): Int = when (kind) {
        V2273WidgetKind.QUICK_CAPTURE -> R.layout.widget_v224_capture
        V2273WidgetKind.TODAY_FOCUS -> R.layout.widget_v224_today
        V2273WidgetKind.MY_DAY -> R.layout.widget_v224_myday
    }

    fun sampleContent(kind: V2273WidgetKind, packageName: String): RemoteViews =
        RemoteViews(packageName, layout(kind)).apply {
            when (kind) {
                V2273WidgetKind.QUICK_CAPTURE -> Unit
                V2273WidgetKind.TODAY_FOCUS -> {
                    setTextViewText(R.id.v224_today_due, "TODAY")
                    setTextViewText(R.id.v224_today_title, "Your next project")
                    setTextViewText(R.id.v224_today_detail, "Edit · sound · publish")
                    setTextViewText(R.id.v224_today_reminder, "Reminder at 7:00 PM")
                }
                V2273WidgetKind.MY_DAY -> {
                    setTextViewText(R.id.v224_day_creator, "CREATOR\n2 tasks")
                    setTextViewText(R.id.v224_day_personal, "PERSONAL\n3 today")
                }
            }
        }
}

/**
 * Read-only RemoteViews preview of the actual launcher widget layout.
 * Synthetic content only. Does not read private reminder titles or modify
 * any pinned widget, project, personal record, or shortcut.
 */
@Composable
internal fun V275WidgetPreview(kind: V2273WidgetKind) {
    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                contentDescription = "Example ${kind.label} widget layout"
                val preview = runCatching {
                    V275WidgetPreviewSpec.sampleContent(kind, ctx.packageName).apply(ctx, this)
                }.getOrElse {
                    TextView(ctx).apply {
                        text = "Widget visual preview unavailable on this launcher."
                        setPadding(16, 18, 16, 18)
                    }
                }
                addView(preview, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ))
            }
        },
        modifier = Modifier.fillMaxWidth().height(174.dp)
            .testTag("v275-widget-layout-${kind.name.lowercase()}"),
    )
    Text(
        "Actual Backlot widget layout · Example content, not live account data",
        fontSize = 10.sp,
    )
}
