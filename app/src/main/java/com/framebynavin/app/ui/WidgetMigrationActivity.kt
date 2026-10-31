package com.framebynavin.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.widget.*

/**
 * Launcher migration is opt-in. Android has no platform transaction that
 * exchanges an old pinned widget for a new one on every OEM launcher.
 */
class WidgetMigrationActivity : ComponentActivity() {
    private var inventory by mutableStateOf<V2273WidgetInventory?>(null)
    private var feedback by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refresh()
        setContent {
            FrameByNavinTheme {
                V2273WidgetMigrationScreen(
                    inventory = inventory,
                    feedback = feedback,
                    canRequestPin = AppWidgetManager.getInstance(this).isRequestPinAppWidgetSupported,
                    onClose = { finish() },
                    onRefresh = ::refresh,
                    onRequestPin = ::requestWidgetPin,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        inventory = runCatching { V2273WidgetInventoryReader.snapshot(this) }.getOrNull()
        if (inventory == null) {
            feedback = "This launcher did not provide widget counts. Existing widgets were not changed."
        }
    }

    private fun requestWidgetPin(kind: V2273WidgetKind) {
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) {
            feedback = V2273WidgetMigrationPolicy.pinMessage(false)
            return
        }
        val accepted = runCatching {
            manager.requestPinAppWidget(
                ComponentName(this, V2273WidgetInventoryReader.provider(kind)), null, null,
            )
        }.getOrDefault(false)
        feedback = V2273WidgetMigrationPolicy.pinMessage(accepted)
    }
}

@Composable
internal fun V2273WidgetMigrationScreen(
    inventory: V2273WidgetInventory?,
    feedback: String?,
    canRequestPin: Boolean,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPin: (V2273WidgetKind) -> Unit,
) {
    Surface(color = CinemaBlack, modifier = Modifier.fillMaxSize().testTag("v2273-widget-migration")) {
        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onClose) { Text("‹  BACK TO SETTINGS", color = ProjectorIvory) }
            Text("HOME SCREEN", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Your Backlot widgets", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(
                "Preview each Backlot widget before adding it. Existing pinned widgets and private data stay unchanged.",
                color = MutedText, fontSize = 13.sp,
            )
            val newCount = inventory?.newTotal
            val oldCount = inventory?.oldTotal
            Text(
                "NEW: ${newCount ?: "—"} PINNED     OLDER: ${oldCount ?: "—"} PINNED",
                color = ProjectorIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.testTag("v2273-widget-counts"),
            )
            Text(
                if (inventory == null) "Widget inventory unavailable; nothing was modified."
                else V2273WidgetMigrationPolicy.migrationMessage(inventory.newTotal, inventory.oldTotal),
                color = MutedText, fontSize = 13.sp,
            )
            for (kind in V2273WidgetKind.entries) {
                Surface(
                    color = CinemaSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(kind.label, color = ProjectorIvory,
                            fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(kind.summary, color = MutedText, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        V275WidgetPreview(kind)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${inventory?.newPinned?.get(kind) ?: "—"} currently pinned",
                            color = MutedGold, fontSize = 12.sp,
                        )
                        Button(
                            onClick = { onRequestPin(kind) },
                            enabled = canRequestPin,
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("v2273-pin-${kind.name.lowercase()}"),
                        ) {
                            Text("ADD ${kind.label.uppercase()}", maxLines = 2)
                        }
                    }
                }
            }
            if (!canRequestPin) {
                Text(
                    "Your launcher doesn't support direct pin requests. Long-press an empty area on Home, tap Widgets, find Backlot, then add your preferred widget.",
                    color = MutedText, fontSize = 13.sp,
                )
            }
            TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()
                .testTag("v2273-refresh")) {
                Text("REFRESH PINNED COUNTS", color = ProjectorIvory)
            }
            feedback?.let { Text(it, color = MutedGold, fontSize = 12.sp) }
            Divider(color = CinemaLine)
            Text("REMOVING OLDER WIDGETS", color = ProjectorIvory, fontWeight = FontWeight.Bold)
            Text(
                "1. Add and test your new widgets. 2. Return to your phone's Home screen. 3. Long-press an older Backlot widget and choose Remove. That removes only its Home-screen shortcut, not Backlot projects, ideas or reminders.",
                color = MutedText, fontSize = 13.sp,
            )
            inventory?.legacyPinned?.filterValues { it > 0 }?.forEach { (name, count) ->
                Text("• $name — $count pinned", color = ProjectorIvory, fontSize = 12.sp)
            }
            Text(
                "Some older widgets have specialized actions not present in the new trio. Check your workflow before removing one. Backlot won't automatically remove, disable or replace pinned widgets.",
                color = MutedText, fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
