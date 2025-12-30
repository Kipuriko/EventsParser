package ru.purebytestudio.eventparser.presentation.favorites

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.data.io.DocumentTextStorage
import ru.purebytestudio.eventparser.domain.model.FavoritesExport
import ru.purebytestudio.eventparser.domain.usecase.GetFavoriteEventsSnapshotUseCase
import ru.purebytestudio.eventparser.domain.usecase.GetFavoriteEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.ImportFavoriteEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.ToggleFavoriteUseCase
import ru.purebytestudio.eventparser.platform.ResourceProvider
import ru.purebytestudio.eventparser.platform.TimeProvider
import java.time.ZoneId

/**
 * ViewModel для экрана избранных событий.
 */
class FavoritesViewModel(
    private val getFavoriteEventsUseCase: GetFavoriteEventsUseCase,
    private val getFavoriteEventsSnapshotUseCase: GetFavoriteEventsSnapshotUseCase,
    private val importFavoriteEventsUseCase: ImportFavoriteEventsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val documentTextStorage: DocumentTextStorage,
    private val json: Json,
    private val timeProvider: TimeProvider,
    private val resourceProvider: ResourceProvider
) : ViewModel(), ContainerHost<FavoritesState, FavoritesSideEffect> {
    override val container = container<FavoritesState, FavoritesSideEffect>(FavoritesState()) {
        loadFavorites()
    }

    private fun loadFavorites() = intent {
        reduce { state.copy(isLoading = true) }

        getFavoriteEventsUseCase().collect { events ->
            reduce {
                state.copy(
                    events = events,
                    isLoading = false
                )
            }
        }
    }

    fun toggleFavorite(eventId: String) = intent {
        toggleFavoriteUseCase(eventId)
    }

    fun onEventClick(eventId: String) = intent {
        postSideEffect(FavoritesSideEffect.NavigateToDetail(eventId))
    }

    fun exportFavoritesTo(uri: Uri) = intent {
        try {
            val favorites = getFavoriteEventsSnapshotUseCase()
            val exportedAtEpochMillis = timeProvider.now()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val payload = FavoritesExport(
                exportedAtEpochMillis = exportedAtEpochMillis,
                events = favorites.map { it.copy(isFavorite = true) }
            )
            val text = json.encodeToString(
                FavoritesExport.serializer(),
                payload
            )
            documentTextStorage.writeText(
                uri,
                text
            )
            postSideEffect(FavoritesSideEffect.ExportSuccess(uri))
        } catch (e: Exception) {
            val fallback = resourceProvider.getString(R.string.favorites_export_failed)
            postSideEffect(FavoritesSideEffect.ExportError(e.message ?: fallback))
        }
    }

    fun importFavoritesFrom(uri: Uri) = intent {
        try {
            val text = documentTextStorage.readText(uri)
            val payload = json.decodeFromString(
                FavoritesExport.serializer(),
                text
            )
            if (payload.schemaVersion != FavoritesExport.CURRENT_SCHEMA_VERSION) {
                postSideEffect(
                    FavoritesSideEffect.ImportError(
                        resourceProvider.getString(
                            R.string.favorites_import_unsupported_version_format,
                            payload.schemaVersion
                        )
                    )
                )
                return@intent
            }
            val count = importFavoriteEventsUseCase(payload.events)
            postSideEffect(FavoritesSideEffect.ImportSuccess(count))
        } catch (e: Exception) {
            val fallback = resourceProvider.getString(R.string.favorites_import_failed)
            postSideEffect(FavoritesSideEffect.ImportError(e.message ?: fallback))
        }
    }
}