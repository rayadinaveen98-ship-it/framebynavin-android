package com.framebynavin.app.personal

/**
 * An owner-selected Personal reminder version. No Creator or 21-day challenge
 * identifier can be supplied through a unified agenda row: the caller must
 * possess the original PersonalReminder record to mint this ticket.
 */
internal data class PersonalDeletionTicket(
    val id: String,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

internal object PersonalBulkDeletePolicy {
    fun ticket(reminder: PersonalReminder): PersonalDeletionTicket {
        reminder.checked()
        return PersonalDeletionTicket(
            reminder.id, reminder.title, reminder.createdAtMillis, reminder.updatedAtMillis)
    }

    /**
     * Fail closed for ANY missing/stale/mismatched ID rather than deleting a
     * partial batch. Lease tokens may rotate during normal Android scheduling;
     * only owner-facing version/identity fields must match the confirmation.
     */
    fun deleteReviewed(
        current: List<PersonalReminder>,
        selection: Collection<PersonalDeletionTicket>,
    ): List<PersonalReminder> {
        require(selection.isNotEmpty()) { "Select at least one Personal reminder" }
        require(selection.size <= 500) { "Too many reminders selected" }
        val ids = selection.map { it.id }
        require(ids.size == ids.distinct().size) { "Duplicate selection; no reminders removed" }
        val recordsById = current.associateBy { it.id }
        require(recordsById.size == current.size) { "Saved Personal IDs conflict" }
        selection.forEach { wanted ->
            val saved = recordsById[wanted.id]
            require(saved != null && ticket(saved) == wanted) {
                "A selected Personal reminder changed or was removed. Review selection and retry."
            }
        }
        val removeIds = ids.toSet()
        return current.filterNot { it.id in removeIds }
    }

    /** Select-all respects the visible agenda scope and ignores challenge rows. */
    fun visiblePersonalTickets(
        groups: List<V337AgendaGroup>,
    ): List<PersonalDeletionTicket> = groups.asSequence()
        .flatMap { it.entries.asSequence() }
        .filterIsInstance<V337AgendaEntry.Personal>()
        .map { ticket(it.record) }
        .distinctBy { it.id }
        .toList()
}
