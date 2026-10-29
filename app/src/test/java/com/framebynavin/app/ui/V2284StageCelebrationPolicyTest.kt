package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorRewardEventType
import com.framebynavin.app.data.CreatorRewardLedgerEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V2284StageCelebrationPolicyTest {
    private fun reward(type: CreatorRewardEventType, key: String) = CreatorRewardLedgerEntry(
        eventKey = key,
        type = type,
        subjectId = "script",
        projectId = "project-1",
        label = "Completed Script",
        xp = 10,
        momentum = 10,
        occurredAtMillis = 1_800_000_000_000L,
    )

    @Test fun onlyDurablyCommittedStageRewardMayCelebrate() {
        val valid = reward(CreatorRewardEventType.PROJECT_STAGE_COMPLETED,
            "stage:project-1:script")
        assertEquals(valid.eventKey, V2284StageCelebrationPolicy.confirmedStageKey(valid))
        assertNull(V2284StageCelebrationPolicy.confirmedStageKey(null))
    }

    @Test fun ideaAndPublicationAndOtherAwardsNeverImpersonateStage() {
        for (type in listOf(
            CreatorRewardEventType.IDEA_DAILY_CAPTURE,
            CreatorRewardEventType.IDEA_CONVERTED,
            CreatorRewardEventType.PROJECT_PUBLISHED,
            CreatorRewardEventType.POST_PUBLISH_COMPLETED,
        )) {
            assertNull(V2284StageCelebrationPolicy.confirmedStageKey(
                reward(type, "stage:project-1:script")))
        }
        assertNull(V2284StageCelebrationPolicy.confirmedStageKey(
            reward(CreatorRewardEventType.PROJECT_STAGE_COMPLETED, "other-event")))
    }

    @Test fun missingProjectIdentityIsNeverCelebrated() {
        val item = reward(CreatorRewardEventType.PROJECT_STAGE_COMPLETED,
            "stage:project-1:script").copy(projectId = "")
        assertNull(V2284StageCelebrationPolicy.confirmedStageKey(item))
    }
}
