package ru.purebytestudio.eventparser.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSortType
import ru.purebytestudio.eventparser.domain.model.QuickFilter
import ru.purebytestudio.eventparser.domain.repository.UserPreferencesRepository
import ru.purebytestudio.eventparser.domain.usecase.GetFilteredEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.RefreshEventsUseCase
import ru.purebytestudio.eventparser.domain.usecase.ToggleFavoriteUseCase
import ru.purebytestudio.eventparser.platform.ErrorMessageProvider
import ru.purebytestudio.eventparser.platform.NetworkStatusProvider
import ru.purebytestudio.eventparser.platform.ResourceProvider
import timber.log.Timber

/**
 * ViewModel для экрана списка событий.
 */
class EventsViewModel(
    private val getFilteredEventsUseCase: GetFilteredEventsUseCase,
    private val refreshEventsUseCase: RefreshEventsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val networkStatusProvider: NetworkStatusProvider,
    private val errorMessageProvider: ErrorMessageProvider,
    private val resourceProvider: ResourceProvider
) : ViewModel(), ContainerHost<EventsState, EventsSideEffect> {
    override val container = container<EventsState, EventsSideEffect>(EventsState()) {
        observeEvents()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeEvents() {
        combine(
            userPreferencesRepository.lastSelectedCategory,
            userPreferencesRepository.lastActiveQuickFilters,
            userPreferencesRepository.sortType
        ) { category, filters, sortType ->
            Triple(
                category,
                filters,
                sortType
            )
        }
            .flatMapLatest { (category, filters, sortType) ->
                getFilteredEventsUseCase(
                    category,
                    filters,
                    sortType
                )
                    .map { events ->
                        EventsData(
                            events = events,
                            category = category,
                            filters = filters,
                            sortType = sortType
                        )
                    }
            }
            .flowOn(Dispatchers.Default)
            .onStart {
                intent {
                    reduce {
                        state.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                }
            }
            .onEach { data ->
                intent {
                    reduce {
                        state.copy(
                            events = data.events,
                            filteredEvents = data.events,
                            selectedCategory = data.category,
                            activeQuickFilters = data.filters,
                            sortType = data.sortType,
                            isLoading = false,
                            isInitialLoad = false,
                            error = null
                        )
                    }
                }
            }
            .catch { e ->
                handleError(e)
            }
            .launchIn(viewModelScope)
    }

    private fun handleError(e: Throwable) {
        Timber.e(
            t = e,
            message = "Error loading events"
        )
        val errorMessage = errorMessageProvider.fromThrowable(
            throwable = e,
            fallback = resourceProvider.getString(R.string.error_loading_data)
        )
        intent {
            reduce {
                state.copy(
                    isLoading = false,
                    isInitialLoad = false,
                    error = errorMessage
                )
            }
        }
    }

    fun refreshEvents() = intent {
        if (!networkStatusProvider.isOnline()) {
            val errorMessage = errorMessageProvider.noInternet()
            reduce {
                state.copy(
                    isRefreshing = false,
                    error = errorMessage
                )
            }
            postSideEffect(EventsSideEffect.ShowError(errorMessage))
            return@intent
        }

        reduce {
            state.copy(
                isRefreshing = true,
                error = null
            )
        }

        val selectedCategory = state.selectedCategory
        val result = if (selectedCategory != null) {
            refreshEventsUseCase(selectedCategory)
        } else {
            refreshEventsUseCase()
        }

        result.fold(
            onSuccess = { _ ->
                reduce { state.copy(isRefreshing = false) }
                postSideEffect(EventsSideEffect.RefreshComplete)
            },
            onFailure = { error ->
                Timber.e(
                    t = error,
                    message = "Failed to refresh events"
                )
                val errorMessage = errorMessageProvider.fromThrowable(
                    throwable = error,
                    fallback = resourceProvider.getString(R.string.error_refresh)
                )
                reduce {
                    state.copy(
                        isRefreshing = false,
                        error = errorMessage
                    )
                }
                postSideEffect(EventsSideEffect.ShowError(errorMessage))
            }
        )
    }

    fun selectCategory(category: EventCategory?) = intent {
        userPreferencesRepository.setLastSelectedCategory(category)
    }

    fun toggleQuickFilter(filter: QuickFilter) = intent {
        val currentFilters = state.activeQuickFilters
        val newFilters = if (currentFilters.contains(filter)) {
            currentFilters - filter
        } else {
            // Logic to handle mutually exclusive filters if needed, 
            // currently simplified to toggle logic in original code
            when (filter) {
                QuickFilter.TODAY -> (currentFilters - QuickFilter.THIS_WEEK) + filter
                QuickFilter.THIS_WEEK -> (currentFilters - QuickFilter.TODAY) + filter
                else -> currentFilters + filter
            }
        }
        userPreferencesRepository.setLastActiveQuickFilters(newFilters)
    }

    fun setSortType(sortType: EventSortType) = intent {
        userPreferencesRepository.setSortType(sortType)
    }

    fun toggleFavorite(eventId: String) = intent {
        toggleFavoriteUseCase(eventId)
    }

    fun onEventClick(eventId: String) = intent {
        postSideEffect(EventsSideEffect.NavigateToDetail(eventId))
    }

    fun retryLoad() = intent {
        reduce {
            state.copy(
                isLoading = true,
                error = null
            )
        }
        if (state.events.isEmpty()) {
            refreshEvents()
        }
    }

    private data class EventsData(
        val events: List<Event>,
        val category: EventCategory?,
        val filters: Set<QuickFilter>,
        val sortType: EventSortType
    )
}