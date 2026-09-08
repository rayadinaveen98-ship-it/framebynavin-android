package com.framebynavin.app.data

/** Compare-and-apply changes against the last persisted snapshot. Never silently replace a newer record. */
object CreatorDeltaEngine {
    fun <T> merge(
        base: List<T>,
        desired: List<T>,
        latest: List<T>,
        idOf: (T) -> String,
    ): List<T> {
        fun indexed(items: List<T>): Map<String, T> {
            val result = items.associateBy(idOf)
            require(result.size == items.size) { "Duplicate record id" }
            return result
        }
        val before = indexed(base)
        val after = indexed(desired)
        val current = indexed(latest)
        val changed = (before.keys + after.keys).filter { before[it] != after[it] }.toSet()
        changed.forEach { id ->
            if (current[id] != before[id])
                throw CreatorWriteConflict("This record changed elsewhere. Your older edit was not saved. Review the latest version before retrying.")
        }
        if (changed.isEmpty()) return latest
        val additions = desired.filter { idOf(it) in changed }
        return additions + latest.filterNot { idOf(it) in changed }
    }
}
