package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V289SpacesRepository
import kotlinx.coroutines.CancellationException

/**
 * Optional, deliberate links chosen on the personal reminder Review step.
 * These are pointers only: no reminder is copied into Room/group storage.
 */
internal data class V310LinkTargets(
    val spaceId: String? = null,
    val groupId: String? = null,
) {
    fun checkedAgainst(
        validSpaceIds: Set<String>,
        validGroupIds: Set<String>,
    ): V310LinkTargets {
        require(spaceId == null || spaceId in validSpaceIds) {
            "Selected Space is unavailable. Review your links before saving."
        }
        require(groupId == null || groupId in validGroupIds) {
            "Selected Group is unavailable. Review your links before saving."
        }
        return this
    }

    val hasLinks: Boolean get() = spaceId != null || groupId != null
}

internal data class V310CreateResult(
    val reminder: PersonalReminder,
    val linkedSpace: Boolean,
    val linkedGroup: Boolean,
    /** Never claim all linked if a post-save store write failed. */
    val linkProblems: List<String>,
)

/**
 * Existing PersonalReminderCommands remains the only scheduling/write boundary.
 *
 * Cross-store writes are NOT atomic: if the reminder saves but a Space or Group
 * write fails, the saved reminder is retained and the caller must clearly
 * notify the owner so they can link manually. No silent rollback of alarms.
 */
internal class V310CreateAndLink(context: Context) {
    private val app = context.applicationContext
    private val commands = PersonalReminderCommands(app)
    private val groups = V298ReminderGroupStore(app)

    suspend fun create(
        draft: PersonalReminderDraft,
        targets: V310LinkTargets,
    ): V310CreateResult {
        // Preflight existing selections *before* creating a reminder, and fail
        // closed if the independent Space/Group stores are damaged.
        if (targets.spaceId != null) {
            withSpaceDao { dao ->
                val chosen = dao.getSpace(targets.spaceId)
                require(chosen != null && chosen.archivedAtMillis == null) {
                    "Selected Space is unavailable or archived. No reminder was created."
                }
            }
        }
        if (targets.groupId != null) {
            require(groups.load().any { it.id == targets.groupId }) {
                "Selected Group no longer exists. No reminder was created."
            }
        }

        val reminder = commands.create(draft)
        val problems = mutableListOf<String>()
        var linkedSpace = false
        var linkedGroup = false
        if (targets.spaceId != null) {
            try {
                linkedSpace = withSpaceDao { dao ->
                    val current = dao.getSpace(targets.spaceId)
                    check(current != null && current.archivedAtMillis == null) {
                        "Space was removed or archived while saving"
                    }
                    val repository = V289SpacesRepository(dao)
                    repository.linkExistingReminder(
                        link = V289SpaceReminderLink(
                            spaceId = targets.spaceId,
                            reminderId = reminder.id,
                            linkedAtMillis = System.currentTimeMillis(),
                        ),
                        existingReminderIds = setOf(reminder.id),
                    )
                }
                if (!linkedSpace) problems.add("Space link wasn't added")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                problems.add("Space link wasn't added")
            }
        }
        if (targets.groupId != null) {
            try {
                val now = System.currentTimeMillis()
                groups.update(targets.groupId) { group ->
                    group.linkExisting(reminder.id, setOf(reminder.id), now)
                }
                linkedGroup = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                problems.add("Group link wasn't added")
            }
        }
        return V310CreateResult(reminder, linkedSpace, linkedGroup, problems)
    }

    private suspend fun <T> withSpaceDao(
        block: suspend (com.framebynavin.app.personal.spaces.V289SpacesDao) -> T,
    ): T {
        val db = V289SpacesDatabase.open(app)
        return try { block(db.spaces()) } finally { db.close() }
    }
}
