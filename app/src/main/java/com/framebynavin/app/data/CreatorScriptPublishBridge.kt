package com.framebynavin.app.data

/**
 * Manual handoff from the explicitly selected Script title into one unpublished output.
 * Script owns its title alternatives; Publish owns final output metadata and release history.
 * No workflow or publication transition takes place here.
 */
object CreatorScriptPublishBridge {
    fun selectedTitle(script: CreatorScriptStudio?): String =
        script?.titles?.firstOrNull { it.selected }?.text?.trim().orEmpty()

    fun canUseTitle(deliverable: CreatorDeliverable, selectedTitle: String): Boolean =
        selectedTitle.isNotBlank() &&
            selectedTitle.trim() != deliverable.title.trim() &&
            deliverable.status != CreatorDeliverableStatus.PUBLISHED &&
            deliverable.publishedAtMillis <= 0L &&
            deliverable.publishedUrl.isBlank() &&
            deliverable.publicationHistory.isEmpty()

    /** Mutates only the selected output's title, in the editor draft, after explicit confirmation. */
    fun useTitle(deliverable: CreatorDeliverable, selectedTitle: String): CreatorDeliverable {
        require(canUseTitle(deliverable, selectedTitle)) {
            "This script title cannot replace this output's title. Published and historical releases are protected."
        }
        return deliverable.copy(title = selectedTitle.trim())
    }
}
