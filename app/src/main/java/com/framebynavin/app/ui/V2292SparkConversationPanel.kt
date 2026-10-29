package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.*

private data class V2292ChatTurn(
    val message: String,
    val isSpark: Boolean,
    val reply: V2292SparkReply? = null,
)

/**
 * Session-only and entirely local. Nothing is sent to a service or stored
 * in a project, preferences, logs, databases or backups.
 */
@Composable
internal fun V2292SparkConversationPanel(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenIdeas: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val turns = remember { mutableStateListOf<V2292ChatTurn>() }
    fun send(text: String) {
        val input = text.trim().take(V2292SparkConversationPolicy.INPUT_MAX)
        if (input.isEmpty()) return
        val reply = V2292SparkConversationPolicy.respond(input, tasks, ideas)
        turns.add(V2292ChatTurn(input, false))
        turns.add(V2292ChatTurn(reply.message, true, reply))
        while (turns.size > 12) turns.removeAt(0)
        draft = ""
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .testTag("v2292-spark-conversation"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("v2292-spark-back")) {
                Text("BACK", color = MutedGold, fontSize = 11.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("SPARK · LOCAL GUIDE", color = ProjectorIvory,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Offline, limited answers · this chat is not saved",
                    color = MutedText, fontSize = 10.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "Next step" to "What should I do next?",
                "My ideas" to "My ideas",
                "Reminders" to "Reminders",
            ).forEachIndexed { i, chip ->
                FilterChip(
                    selected = false,
                    onClick = { send(chip.second) },
                    label = { Text(chip.first, fontSize = 11.sp) },
                    modifier = Modifier.testTag("v2292-spark-chip-$i"),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .testTag("v2292-spark-turns"),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (turns.isEmpty()) {
                Text("Ask about your next project step, ideas, or reminders.",
                    color = MutedText, fontSize = 12.sp)
            }
            turns.forEachIndexed { i, turn ->
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .testTag(if (turn.isSpark) "v2292-spark-reply" else "v2292-user-turn"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (turn.isSpark) CinemaSurface else CinemaSurfaceRaised,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (turn.isSpark) "SPARK" else "YOU", color = MutedGold,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(turn.message, color = ProjectorIvory,
                            fontSize = 12.sp, lineHeight = 17.sp)
                        val action = turn.reply?.takeIf {
                            it.destination != V2292SparkDestination.NONE &&
                                it.actionLabel.isNotBlank()
                        }
                        if (action != null) {
                            TextButton(
                                onClick = {
                                    when (action.destination) {
                                        V2292SparkDestination.PROJECT ->
                                            action.targetProjectId.takeIf { it.isNotBlank() }
                                                ?.let(onOpenProject)
                                        V2292SparkDestination.IDEAS -> onOpenIdeas()
                                        V2292SparkDestination.REMINDERS -> onOpenReminders()
                                        V2292SparkDestination.NONE -> Unit
                                    }
                                },
                                modifier = Modifier.testTag("v2292-spark-action"),
                            ) {
                                Text(action.actionLabel, color = MutedGold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(V2292SparkConversationPolicy.INPUT_MAX) },
                modifier = Modifier.weight(1f).testTag("v2292-spark-input"),
                label = { Text("Ask Spark", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = { send(draft) }
                ),
            )
            Spacer(Modifier.width(7.dp))
            Button(onClick = { send(draft) }, enabled = draft.isNotBlank(),
                modifier = Modifier.testTag("v2292-spark-send")) {
                Text("SEND", fontSize = 11.sp)
            }
        }
    }
}
