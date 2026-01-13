package ru.purebytestudio.eventparser.presentation.favorites

import android.net.Uri
import ru.purebytestudio.eventparser.domain.model.Event

/**
 * Контракт экрана избранного (MVI).
 */
data class FavoritesState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false
)

sealed class FavoritesSideEffect {
    data class NavigateToDetail(val eventId: String) : FavoritesSideEffect()
    data class ExportSuccess(val uri: Uri) : FavoritesSideEffect()
    data class ExportError(val message: String) : FavoritesSideEffect()
    data class ImportSuccess(val count: Int) : FavoritesSideEffect()
    data class ImportError(val message: String) : FavoritesSideEffect()
}