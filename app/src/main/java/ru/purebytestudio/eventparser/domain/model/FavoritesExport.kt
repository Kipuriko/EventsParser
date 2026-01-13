package ru.purebytestudio.eventparser.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoritesExport(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val events: List<Event>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}