package com.framebynavin.app.ui

import com.framebynavin.app.personal.spaces.V289SpaceEntity

/** Read-only Home projection of owner-authored local Spaces. */
internal data class V340HomeSpacesPreview(
    val active: List<V289SpaceEntity>,
    val archivedCount: Int,
) {
    val activeCount: Int get() = active.size
    val featured: List<V289SpaceEntity> get() = active.take(2)
}

internal object V340HomeSpacesPolicy {
    fun preview(records: List<V289SpaceEntity>): V340HomeSpacesPreview {
        val sorted = records.sortedWith(
            compareBy<V289SpaceEntity> { it.displayOrder }
                .thenBy { it.createdAtMillis }
                .thenBy { it.id }
        )
        return V340HomeSpacesPreview(
            active = sorted.filter { it.archivedAtMillis == null },
            archivedCount = records.count { it.archivedAtMillis != null },
        )
    }

    /**
     * A Home chip opens only an active Space present in this snapshot.
     * A stale/deleted/archived ID falls back to the existing Spaces overview,
     * never to another owner's Space or a synthetic replacement.
     */
    fun openableSpaceId(
        requestedId: String?,
        current: List<V289SpaceEntity>,
    ): String? = requestedId?.takeIf { id ->
        id.isNotBlank() && current.any { it.id == id && it.archivedAtMillis == null }
    }
}
