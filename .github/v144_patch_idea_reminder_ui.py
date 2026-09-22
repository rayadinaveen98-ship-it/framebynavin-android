from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 match, found {count}')
    return text.replace(old, new, 1)


# Idea Vault: deep-link target, reminder state, picker and persistence.
p = Path('app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt')
text = p.read_text()
text = replace_once(
    text,
    'import com.framebynavin.app.youtube.YouTubeOpportunityEngine\n',
    'import com.framebynavin.app.youtube.YouTubeOpportunityEngine\nimport com.framebynavin.app.widget.CreatorWidgetContract\n',
    'idea vault widget contract import',
)
text = replace_once(
    text,
    '''    onArchive: (String) -> Unit,
    onConvert: (String, String, String, Long) -> String?,
) {''',
    '''    onArchive: (String) -> Unit,
    onConvert: (String, String, String, Long) -> String?,
    externalIdeaId: String = "",
    externalIdeaMode: String = "",
    externalLaunchNonce: Long = 0L,
) {''',
    'idea vault external parameters',
)
text = replace_once(
    text,
    '''    var editing by remember { mutableStateOf<CreatorIdea?>(null) }
    var creating by remember { mutableStateOf(false) }
    var converting by remember { mutableStateOf<CreatorIdea?>(null) }

    val opportunityReport''',
    '''    var editing by remember { mutableStateOf<CreatorIdea?>(null) }
    var creating by remember { mutableStateOf(false) }
    var converting by remember { mutableStateOf<CreatorIdea?>(null) }
    var handledExternalNonce by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(externalLaunchNonce, externalIdeaId, ideas.size) {
        if (externalLaunchNonce == 0L || externalLaunchNonce == handledExternalNonce || externalIdeaId.isBlank()) {
            return@LaunchedEffect
        }
        val target = ideas.firstOrNull { it.id == externalIdeaId } ?: return@LaunchedEffect
        handledExternalNonce = externalLaunchNonce
        when (externalIdeaMode) {
            CreatorWidgetContract.IDEA_MODE_CONVERT -> converting = target
            else -> editing = target
        }
    }

    val opportunityReport''',
    'idea vault deep link effect',
)
text = replace_once(
    text,
    '''    var notes by remember(idea.id) { mutableStateOf(idea.notes) }
    var showOrganize by rememberSaveable(idea.id) { mutableStateOf(false) }''',
    '''    var notes by remember(idea.id) { mutableStateOf(idea.notes) }
    var reminderAtMillis by remember(idea.id) { mutableLongStateOf(idea.reminderAtMillis) }
    var reminderCadence by remember(idea.id) { mutableStateOf(idea.reminderCadence) }
    var showOrganize by rememberSaveable(idea.id) { mutableStateOf(false) }''',
    'idea editor reminder state',
)
text = replace_once(
    text,
    '''                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { showOrganize = !showOrganize },''',
    '''                )
                Spacer(Modifier.height(10.dp))
                V144IdeaReminderPicker(
                    reminderAtMillis = reminderAtMillis,
                    cadence = reminderCadence,
                    enabled = status != IdeaStatus.CONVERTED && status != IdeaStatus.ARCHIVED,
                    onChange = { atMillis, cadence ->
                        reminderAtMillis = atMillis
                        reminderCadence = cadence
                    },
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { showOrganize = !showOrganize },''',
    'idea reminder picker placement',
)
text = replace_once(
    text,
    '''                            notes = notes.trim(),
                            createdAtMillis = idea.createdAtMillis.takeIf { it > 0L } ?: now,
                            updatedAtMillis = now,''',
    '''                            notes = notes.trim(),
                            reminderAtMillis = reminderAtMillis,
                            reminderCadence = reminderCadence,
                            createdAtMillis = idea.createdAtMillis.takeIf { it > 0L } ?: now,
                            updatedAtMillis = now,''',
    'idea reminder save fields',
)
p.write_text(text)

# Main shell: pass external notification target into Idea Vault.
p = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')
text = p.read_text()
text = replace_once(
    text,
    '''                    onConvert = { ideaId, platform, format, dueAtMillis ->
                        val projectId = vm.convertIdeaToProject(ideaId, platform, format, dueAtMillis)
                        if (V18CreatorJourney.afterProjectCreated(projectId) == V18JourneyDestination.CREATE) {
                            externalStudioId = projectId
                            externalStudioNonce += 1L
                        }
                        routeJourney(V18CreatorJourney.afterProjectCreated(projectId))
                        projectId
                    },
                )''',
    '''                    onConvert = { ideaId, platform, format, dueAtMillis ->
                        val projectId = vm.convertIdeaToProject(ideaId, platform, format, dueAtMillis)
                        if (V18CreatorJourney.afterProjectCreated(projectId) == V18JourneyDestination.CREATE) {
                            externalStudioId = projectId
                            externalStudioNonce += 1L
                        }
                        routeJourney(V18CreatorJourney.afterProjectCreated(projectId))
                        projectId
                    },
                    externalIdeaId = externalLaunch?.ideaId.orEmpty(),
                    externalIdeaMode = externalLaunch?.ideaMode.orEmpty(),
                    externalLaunchNonce = externalLaunch?.nonce ?: 0L,
                )''',
    'idea vault external arguments',
)
p.write_text(text)
