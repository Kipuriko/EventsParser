package ru.purebytestudio.eventparser.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.domain.usecase.GetEventByIdUseCase
import ru.purebytestudio.eventparser.domain.usecase.ToggleFavoriteUseCase
import ru.purebytestudio.eventparser.platform.ResourceProvider

/**
 * ViewModel для экрана деталей события.
 */
class EventDetailViewModel(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val resourceProvider: ResourceProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel(), ContainerHost<EventDetailState, EventDetailSideEffect> {
    private val eventId: String = savedStateHandle.get<String>("eventId") ?: ""

    override val container =
        container<EventDetailState, EventDetailSideEffect>(EventDetailState()) {
            loadEvent()
        }

    private fun loadEvent() = intent {
        if (eventId.isEmpty()) {
            reduce { state.copy(error = resourceProvider.getString(R.string.event_detail_error_missing_id)) }
            return@intent
        }

        reduce { state.copy(isLoading = true) }

        val event = getEventByIdUseCase(eventId)

        if (event != null) {
            reduce {
                state.copy(
                    event = event,
                    isLoading = false
                )
            }
        } else {
            reduce {
                state.copy(
                    error = resourceProvider.getString(R.string.event_detail_error_not_found),
                    isLoading = false
                )
            }
        }
    }

    fun toggleFavorite() = intent {
        state.event?.let { event ->
            toggleFavoriteUseCase(event.id)
            // Reload event to update favorite status
            val updatedEvent = getEventByIdUseCase(event.id)
            reduce { state.copy(event = updatedEvent) }
        }
    }

    fun openEventUrl() = intent {
        state.event?.let { event ->
            postSideEffect(EventDetailSideEffect.OpenUrl(event.url))
        }
    }

    fun shareEvent() = intent {
        state.event?.let { event ->
            postSideEffect(EventDetailSideEffect.ShareEvent(event))
        }
    }

    fun addToCalendar() = intent {
        state.event?.let { event ->
            postSideEffect(EventDetailSideEffect.AddToCalendar(event))
        }
    }
}