from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"Could not find {label}")
    return text.replace(old, new, 1)


# Activate professional Content-DNA workflows while preserving the existing legacy fallback.
workflow = Path("app/src/main/java/com/framebynavin/app/data/CreatorWorkflow.kt")
s = workflow.read_text()
s = replace_once(
    s,
    "    fun templateFor(task: CreatorTask): WorkflowTemplate = templateFor(task.platform, task.contentType)",
    "    fun templateFor(task: CreatorTask): WorkflowTemplate = CreatorWorkflowV2.templateFor(task) ?: templateFor(task.platform, task.contentType)",
    "CreatorWorkflowEngine task resolver",
)
workflow.write_text(s)


# Simplify Idea Vault's scan path. All advanced controls stay available in V150IdeaFilterSheet.
ideas = Path("app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt")
s = ideas.read_text()

state_anchor = "    var captureFilter by rememberSaveable { mutableStateOf<IdeaCaptureType?>(null) }\n"
if "var showFilters by rememberSaveable" not in s:
    if state_anchor not in s:
        raise RuntimeError("Could not find Idea Vault filter state anchor")
    s = s.replace(state_anchor, state_anchor + "    var showFilters by rememberSaveable { mutableStateOf(false) }\n", 1)

count_anchor = "    val voiceCount by remember { derivedStateOf { ideas.count { it.captureType == IdeaCaptureType.VOICE } } }\n"
if "val activeFilterCount by remember" not in s:
    if count_anchor not in s:
        raise RuntimeError("Could not find Idea Vault count anchor")
    s = s.replace(
        count_anchor,
        count_anchor
        + """    val activeFilterCount by remember {
        derivedStateOf {
            listOfNotNull(
                statusFilter?.takeIf { it !in setOf(IdeaStatus.INBOX, IdeaStatus.READY_TO_PRODUCE, IdeaStatus.CONVERTED) },
                categoryFilter,
                captureFilter,
            ).size
        }
    }
""",
        1,
    )

# Replace the tall opportunity banner with one quiet, high-signal row.
opp_start_marker = "            opportunityMatch?.let { match ->"
search_marker = "            OutlinedTextField("
if "MATCHED TO YOUR CHANNEL MOMENTUM" not in s:
    opp_start = s.index(opp_start_marker)
    opp_end = s.index(search_marker, opp_start)
    compact_opportunity = """            opportunityMatch?.let { match ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MutedGold.copy(alpha = .055f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MutedGold.copy(alpha = .24f)),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("MATCHED TO YOUR CHANNEL MOMENTUM", color = MutedGold, fontSize = 7.2.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                            Text(match.ideaTitle ?: match.title, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

"""
    s = s[:opp_start] + compact_opportunity + s[opp_end:]

# Replace three permanent filter rails with four core states + one advanced Filters entry point.
filter_start_marker = "            Spacer(Modifier.height(10.dp))\n            Row(\n                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),"
filter_end_marker = "            Spacer(Modifier.height(6.dp))\n            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {"
if "Filters ($activeFilterCount)" not in s:
    filter_start = s.index(filter_start_marker)
    filter_end = s.index(filter_end_marker, filter_start)
    compact_filters = """            Spacer(Modifier.height(9.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All", fontSize = 10.sp) })
                FilterChip(selected = statusFilter == IdeaStatus.INBOX, onClick = { statusFilter = IdeaStatus.INBOX }, label = { Text("Inbox", fontSize = 10.sp) })
                FilterChip(selected = statusFilter == IdeaStatus.READY_TO_PRODUCE, onClick = { statusFilter = IdeaStatus.READY_TO_PRODUCE }, label = { Text("Ready", fontSize = 10.sp) })
                FilterChip(selected = statusFilter == IdeaStatus.CONVERTED, onClick = { statusFilter = IdeaStatus.CONVERTED }, label = { Text("Converted", fontSize = 10.sp) })
                FilterChip(
                    selected = activeFilterCount > 0,
                    onClick = { showFilters = true },
                    label = { Text(if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters", fontSize = 10.sp) },
                )
            }

"""
    s = s[:filter_start] + compact_filters + s[filter_end:]

# Preserve every advanced filter in a bottom sheet instead of keeping it permanently visible.
sheet_anchor = "    if (creating) {\n"
if "V150IdeaFilterSheet(" not in s:
    if sheet_anchor not in s:
        raise RuntimeError("Could not find Idea Vault dialog anchor")
    sheet = """    if (showFilters) {
        V150IdeaFilterSheet(
            visibleCategories = visibleCategories,
            statusFilter = statusFilter,
            categoryFilter = categoryFilter,
            captureFilter = captureFilter,
            voiceCount = voiceCount,
            onStatus = { statusFilter = it },
            onCategory = { categoryFilter = it },
            onCapture = { captureFilter = it },
            onDismiss = { showFilters = false },
        )
    }

"""
    s = s.replace(sheet_anchor, sheet + sheet_anchor, 1)

# Keep cards scannable. Full notes, playback, potential and organization remain in the opened Idea editor.
notes_block = """            if (idea.notes.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(idea.notes, color = MutedText, fontSize = 9.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (idea.hasOriginalRecording) {
                Spacer(Modifier.height(9.dp))
                VoiceIdeaPlaybackControl(idea = idea)
            }
"""
if notes_block in s:
    s = s.replace(notes_block, "", 1)

card_tail = """                Spacer(Modifier.weight(1f))
                if (isOpportunity) {
                    Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .12f)) {
                        Text("MATCHED", color = MutedGold, fontSize = 7.2.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(idea.potential.name, color = if (idea.potential == IdeaPotential.HIGH) RecRed else MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
"""
compact_tail = """                Spacer(Modifier.weight(1f))
                if (isOpportunity) {
                    Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .12f)) {
                        Text("⚡ MOMENTUM", color = MutedGold, fontSize = 7.2.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                }
"""
if card_tail in s:
    s = s.replace(card_tail, compact_tail, 1)

ideas.write_text(s)
print("V150 product patch applied")
