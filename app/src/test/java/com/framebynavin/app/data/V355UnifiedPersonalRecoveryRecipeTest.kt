package com.framebynavin.app.data

import com.framebynavin.app.personal.PersonalCompletionBackupCodec
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitCodec
import com.framebynavin.app.personal.V297ChallengeCodec
import com.framebynavin.app.personal.V298ReminderGroupCodec
import com.framebynavin.app.personal.V300PersonalComponents
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure original-plus-selected Personal recovery tests; no live app writers. */
class V355UnifiedPersonalRecoveryRecipeTest {
    private val day = LocalDate.of(2026, 11, 13)
    private val confirmed = Phase1HabitChallenge(
        id = "saved-21", activity = "Read daily", startDate = day,
        zoneId = "Asia/Kolkata",
    ).confirmToday(Instant.parse("2026-11-13T05:30:00Z"))
    private val incoming = Phase1HabitChallenge(
        id = "new-21", activity = "Walk daily", startDate = day,
        zoneId = "Asia/Kolkata",
    )

    private fun bundle(
        habits: List<Phase1HabitChallenge> = emptyList(),
        reminders: List<PersonalReminder> = emptyList(),
    ) = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(reminders, emptyList()),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        singleActivityChallenges = Phase1HabitCodec.encode(habits),
    )

    @Test fun selectedNewChallengeIsMergedWithoutLosingExistingDoneReceipt() {
        val original = bundle(habits = listOf(confirmed))
        val selected = bundle(habits = listOf(incoming))
        val recipe = V355UnifiedPersonalRecoveryRecipePolicy.build(
            original, emptyList(), selected, emptyList(),
        )
        assertEquals(1, recipe.incomingAdditions)
        assertNotEquals(recipe.originalDigest, recipe.mergedTargetDigest)
        assertEquals(0, recipe.originalPhotoCount)
        assertEquals(0, recipe.mergedTargetPhotoCount)
        assertTrue(recipe.allOriginalPhotoReferencesPreserved)
        assertFalse(recipe.mayAutoApply)
        assertFalse(recipe.mayDeleteExistingPhotos)
        assertFalse(recipe.canReleaseCombinedJournal)
        assertEquals(
            Instant.parse("2026-11-13T05:30:00Z"),
            confirmed.confirmations.values.single(),
        )
    }

    @Test fun selectingIdenticalPersonalBackupIsIdempotentNotDestructive() {
        val original = bundle(habits = listOf(confirmed))
        val recipe = V355UnifiedPersonalRecoveryRecipePolicy.build(
            original, emptyList(), original, emptyList(),
        )
        assertEquals(0, recipe.incomingAdditions)
        assertEquals(recipe.originalDigest, recipe.mergedTargetDigest)
        assertTrue(recipe.noMutationAuthority)
        assertFalse(recipe.mayAutoApply)
    }

    @Test fun conflictingReminderIsNotAcceptedAsARecoveryTarget() {
        val reminder = PersonalReminder(
            id = "same-id", title = "Original reminder", startDate = day,
            time = LocalTime.of(8, 30), createdAtMillis = 1L, updatedAtMillis = 1L,
        )
        val original = bundle(reminders = listOf(reminder))
        val changed = bundle(reminders = listOf(reminder.copy(title = "Changed remotely")))
        assertThrows(IllegalArgumentException::class.java) {
            V355UnifiedPersonalRecoveryRecipePolicy.build(
                original, emptyList(), changed, emptyList(),
            )
        }
    }

    @Test fun differentExactDoneInstantOrMissingChallengeCoverageBlocksMerge() {
        val changed = confirmed.copy(confirmations = mapOf(
            day to Instant.parse("2026-11-13T08:30:00Z"),
        ))
        assertThrows(IllegalArgumentException::class.java) {
            V355UnifiedPersonalRecoveryRecipePolicy.build(
                bundle(listOf(confirmed)), emptyList(),
                bundle(listOf(changed)), emptyList(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            V355UnifiedPersonalRecoveryRecipePolicy.build(
                bundle(listOf(confirmed)), emptyList(),
                bundle(listOf(incoming)).copy(singleActivityChallenges = null),
                emptyList(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            V355UnifiedPersonalRecoveryRecipePolicy.build(
                bundle(listOf(confirmed)).copy(singleActivityChallenges = null),
                emptyList(), bundle(listOf(incoming)), emptyList(),
            )
        }
    }
}
