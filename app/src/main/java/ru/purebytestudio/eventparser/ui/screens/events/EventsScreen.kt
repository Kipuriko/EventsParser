package ru.purebytestudio.eventparser.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.presentation.events.EventsSideEffect
import ru.purebytestudio.eventparser.presentation.events.EventsViewModel
import ru.purebytestudio.eventparser.ui.components.ErrorState
import ru.purebytestudio.eventparser.ui.components.EventCard
import ru.purebytestudio.eventparser.ui.components.LocalNotifier
import ru.purebytestudio.eventparser.ui.components.NoEventsState
import ru.purebytestudio.eventparser.ui.components.ShimmerEventsList

/**
 * Экран списка событий.
 * Отображает список мероприятий с возможностью фильтрации, сортировки и поиска.
 * Поддерживает pull-to-refresh и обработку состояний (загрузка, ошибка, пустой список).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onEventClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: EventsViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp) // Принимаем отступы от Scaffold родителя
) {
    val state by viewModel.collectAsState()
    // Save scroll positions across navigation (e.g., when returning from detail screen)
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val categoriesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Состояние bottom sheet (панель фильтров)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (showFilterSheet, setShowFilterSheet) = remember { mutableStateOf(false) }

    // Вспомогательный «нотификатор» (snackbar/toast-логика)
    val notifier = LocalNotifier.current

    // Забираем строковые ресурсы внутри @Composable контекста
    val refreshCompleteMessage = stringResource(R.string.events_refreshed)

    // Не прокручиваем список к началу на первой композиции (важно для восстановления состояния при back-навигации).
    val didInitScrollEffect = remember { mutableStateOf(false) }

    LaunchedEffect(
        state.selectedCategory,
        state.activeQuickFilters,
        state.sortType
    ) {
        if (didInitScrollEffect.value) {
            listState.scrollToItem(0)
        } else {
            didInitScrollEffect.value = true
        }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is EventsSideEffect.ShowError -> {
                notifier.showError(sideEffect.message)
            }

            is EventsSideEffect.NavigateToDetail -> {
                onEventClick(sideEffect.eventId)
            }

            EventsSideEffect.RefreshComplete -> {
                notifier.showSuccess(refreshCompleteMessage)
                listState.animateScrollToItem(0)
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Background Gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.surfaceContainerLow
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            EventsTopBar(
                isRefreshing = state.isRefreshing,
                onSettingsClick = onSettingsClick,
                onRefreshClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.refreshEvents()
                },
                scrollBehavior = scrollBehavior,
            )
        }) { paddingValues ->
        // Склеиваем внутренние отступы Scaffold (top bar) с внешними отступами контента (bottom bar).
        // Дополнительно добавляем «страховочный» нижний отступ, чтобы контент не упирался в край
        // при скрытом bottom bar и при разных insets.
        val bottomPadding =
            contentPadding.calculateBottomPadding() + androidx.compose.ui.res.dimensionResource(R.dimen.bottom_bar_spacer)

        val combinedPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = bottomPadding
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.refreshEvents()
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = combinedPadding.calculateTopPadding())
                .background(Color.Transparent)
        ) {
            when {
                state.error != null && state.events.isEmpty() -> {
                    ErrorState(
                        errorMessage = state.error!!,
                        onRetry = { viewModel.retryLoad() })
                }

                state.isLoading && state.events.isEmpty() -> {
                    ShimmerEventsList(
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        )
                    )
                }

                state.showNoResultsState -> {
                    // Still show filters even if no results
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = combinedPadding.calculateBottomPadding())
                    ) {
                        CategoriesRow(
                            state = state,
                            onCategorySelected = viewModel::selectCategory,
                            listState = categoriesListState
                        )
                        ControlsRow(
                            state = state,
                            onFilterClick = { setShowFilterSheet(true) })

                        NoEventsState(
                            modifier = Modifier.weight(1f),
                            onRefresh = { viewModel.refreshEvents() }
                        )
                    }
                }

                state.showEmptyState -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = combinedPadding.calculateBottomPadding())
                    ) {
                        CategoriesRow(
                            state = state,
                            onCategorySelected = viewModel::selectCategory,
                            listState = categoriesListState
                        )
                        ControlsRow(
                            state = state,
                            onFilterClick = { setShowFilterSheet(true) })

                        NoEventsState(
                            modifier = Modifier.weight(1f),
                            onRefresh = { viewModel.refreshEvents() }
                        )
                    }
                }

                state.showContent -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = combinedPadding.calculateBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Categories Row (Horizontal)
                        item {
                            Column {
                                CategoriesRow(
                                    state = state,
                                    onCategorySelected = viewModel::selectCategory,
                                    listState = categoriesListState
                                )
                                ControlsRow(
                                    state = state,
                                    onFilterClick = { setShowFilterSheet(true) })
                            }
                        }

                        // Events List
                        items(
                            items = state.filteredEvents,
                            key = { event -> event.id },
                            contentType = { "event_card" }) { event ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EventCard(
                                    event = event,
                                    onClick = remember(event.id) { { viewModel.onEventClick(event.id) } },
                                    onFavoriteClick = remember(event.id) { { viewModel.toggleFavorite(event.id) } },
                                    showSourceBadge = false
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { setShowFilterSheet(false) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                FilterSheetContent(
                    state = state,
                    onSortSelected = {
                        viewModel.setSortType(it)
                    },
                    onQuickFilterToggle = viewModel::toggleQuickFilter,
                    onDismiss = {
                        scope.launch { sheetState.hide() }
                            .invokeOnCompletion { setShowFilterSheet(false) }
                    }
                )
            }
        }
    }
}