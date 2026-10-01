package com.framebynavin.app.ui

/**
 * First-run setup should only block on fields required by CreatorProfile.isComplete.
 * Production styles are optional V2 enrichment and can be added later from creator setup.
 */
internal object V149CreatorOnboardingPolicy {
    fun canContinue(
        page: Int,
        primaryMode: String,
        platforms: Set<String>,
        selectedGoals: Set<String>,
        primaryGoal: String,
    ): Boolean = when (page) {
        0 -> primaryMode.isNotBlank()
        1 -> platforms.isNotEmpty()
        2 -> true
        3 -> selectedGoals.isNotEmpty() && primaryGoal in selectedGoals
        else -> true
    }

    fun primaryActionLabel(page: Int, hasProductionStyles: Boolean): String = when {
        page == 2 && !hasProductionStyles -> "SKIP FOR NOW"
        page < 4 -> "CONTINUE"
        else -> "ENTER CREATOR OS"
    }
}
