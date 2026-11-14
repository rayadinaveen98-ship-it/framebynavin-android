package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*

/**
 * Lightweight first-run route; no CreatorProfile defaults, projects, reminders,
 * or permission requests are created by this preference. Existing complete
 * creators never enter this flow on app upgrade.
 */
internal enum class V393Focus(val label: String, val explanation: String) {
    CREATOR("Creator", "Ideas, projects and creative progress"),
    PERSONAL("Personal", "Reminders, Spaces and 21-day challenges"),
    BOTH("Both", "Your work and everyday life together");
}
internal enum class V393FirstAction { CREATE_PROJECT, CAPTURE_IDEA, OPEN_PERSONAL, EXPLORE }

internal object V393FirstRunPrefs {
    private const val FILE = "backlot_first_run_focus_v1"
    private const val FOCUS = "selected_focus"
    private const val ACTION_COMPLETE = "first_action_complete"
    private var initialized = false
    var focus by mutableStateOf<V393Focus?>(null)
        private set
    var firstActionComplete by mutableStateOf(false)
        private set

    @Synchronized fun initialize(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        focus = V393Focus.entries.firstOrNull { it.name == prefs.getString(FOCUS, null) }
        firstActionComplete = prefs.getBoolean(ACTION_COMPLETE, false)
        initialized = true
    }

    fun choose(context: Context, selected: V393Focus) {
        initialize(context)
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(FOCUS, selected.name).putBoolean(ACTION_COMPLETE, false).apply()
        focus = selected
        firstActionComplete = false
    }

    fun finishIntro(context: Context) {
        initialize(context)
        check(focus != null) { "Choose a Backlot focus before finishing the first run" }
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(ACTION_COMPLETE, true).apply()
        firstActionComplete = true
    }

    /** The earlier five-page Creator setup is deferred, not completed with invented profile data. */
    fun shouldDeferCreatorSetup(): Boolean = focus != null && firstActionComplete
}

@Composable
internal fun V393FocusChoiceScreen(onSelected: (V393Focus) -> Unit) {
    Surface(Modifier.fillMaxSize().testTag("v393-focus-screen"), color=CinemaBlack) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(horizontal=22.dp, vertical=24.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("BACKLOT  /  WELCOME", color=RecRed, fontSize=11.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("What should Backlot help you with?",color=ProjectorIvory,
                fontSize=29.sp,lineHeight=36.sp,fontWeight=FontWeight.Black)
            Text("Choose how you'll begin. Every feature remains available, and your choice can always change later.",
                color=MutedText,fontSize=14.sp,lineHeight=21.sp)
            Spacer(Modifier.height(9.dp))
            for (focus in V393Focus.entries) {
                val icon: ImageVector = when(focus) {
                    V393Focus.CREATOR -> Icons.Outlined.Movie
                    V393Focus.PERSONAL -> Icons.Outlined.FavoriteBorder
                    V393Focus.BOTH -> Icons.Outlined.AutoAwesome
                }
                Surface(
                    modifier=Modifier.fillMaxWidth().clickable { onSelected(focus) }
                        .testTag("v393-focus-"+focus.name.lowercase())
                        .semantics { contentDescription = "Start Backlot with " + focus.label },
                    shape=RoundedCornerShape(20.dp), color=CinemaSurface,
                    border=BorderStroke(1.dp,CinemaLine),
                ) {
                    Row(Modifier.fillMaxWidth().padding(19.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(15.dp)) {
                        Icon(icon,contentDescription=null,tint=RecRed,modifier=Modifier.size(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text(focus.label,color=ProjectorIvory,fontSize=18.sp,fontWeight=FontWeight.Bold)
                            Text(focus.explanation,color=MutedText,fontSize=12.sp,lineHeight=17.sp)
                        }
                        Text("›",color=MutedGold,fontSize=24.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Next: optional account → your companion → your first action.",
                color=MutedText,fontSize=12.sp)
        }
    }
}

@Composable
internal fun V393FirstActionScreen(
    focus: V393Focus,
    onAction: (V393FirstAction) -> Unit,
) {
    val options = when(focus) {
        V393Focus.CREATOR -> listOf(
            Triple(V393FirstAction.CREATE_PROJECT,"Create a project","Start your first creative project"),
            Triple(V393FirstAction.CAPTURE_IDEA,"Capture an idea","Keep something worth making"),
            Triple(V393FirstAction.EXPLORE,"Explore Backlot","See Home before adding anything"))
        V393Focus.PERSONAL -> listOf(
            Triple(V393FirstAction.OPEN_PERSONAL,"Open Personal reminders","Create reminders, Spaces or challenges"),
            Triple(V393FirstAction.EXPLORE,"Explore Backlot","See the app before adding anything"))
        V393Focus.BOTH -> listOf(
            Triple(V393FirstAction.CREATE_PROJECT,"Create a project","Start a creative project"),
            Triple(V393FirstAction.OPEN_PERSONAL,"Open Personal reminders","Start your daily routine"),
            Triple(V393FirstAction.EXPLORE,"Explore Backlot","See everything together"))
    }
    Surface(Modifier.fillMaxSize().testTag("v393-first-action-screen"),color=CinemaBlack) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=24.dp),
            verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Text("YOUR FIRST MOMENT",color=RecRed,fontSize=11.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Text("You're ready to begin.",color=ProjectorIvory,fontSize=29.sp,
                lineHeight=35.sp,fontWeight=FontWeight.Black)
            Text("You don't need to finish a long setup to make Backlot useful.",
                color=MutedText,fontSize=14.sp,lineHeight=21.sp)
            Spacer(Modifier.height(12.dp))
            for ((action,title,description) in options) {
                val icon: ImageVector=when(action) {
                    V393FirstAction.CREATE_PROJECT -> Icons.Outlined.Movie
                    V393FirstAction.CAPTURE_IDEA -> Icons.Outlined.Lightbulb
                    V393FirstAction.OPEN_PERSONAL -> Icons.Outlined.FavoriteBorder
                    V393FirstAction.EXPLORE -> Icons.Outlined.Explore
                }
                Surface(
                    modifier=Modifier.fillMaxWidth().clickable { onAction(action) }
                        .testTag("v393-first-"+action.name.lowercase()),
                    color=CinemaSurface,shape=RoundedCornerShape(18.dp),
                    border=BorderStroke(1.dp,CinemaLine)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(15.dp)) {
                        Icon(icon,null,tint=RecRed,modifier=Modifier.size(30.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title,color=ProjectorIvory,fontSize=17.sp,fontWeight=FontWeight.Bold)
                            Text(description,color=MutedText,fontSize=12.sp,lineHeight=18.sp)
                        }
                        Text("›",color=MutedGold,fontSize=24.sp)
                    }
                }
            }
            Text("Creator goals and advanced permissions can be configured later when you need them.",
                color=MutedText,fontSize=12.sp,lineHeight=17.sp)
        }
    }
}
