package ru.purebytestudio.eventparser.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.presentation.events.EventsState
import ru.purebytestudio.eventparser.ui.components.CategoryChip

@Composable
fun CategoriesRow(
    state: EventsState,
    onCategorySelected: (EventCategory?) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        item {
            CategoryChip(
                category = null,
                isSelected = state.selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = stringResource(R.string.category_all)
            )
        }
        items(
            items = state.availableCategories,
            key = { it.name }) { category ->
            CategoryChip(
                category = category,
                isSelected = state.selectedCategory == category,
                onClick = { onCategorySelected(category) })
        }
    }
}

@Composable
fun ControlsRow(
    state: EventsState,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Result Count Text
        Text(
            text = "${state.filteredEvents.size} ${stringResource(R.string.summary_total).lowercase()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Filter & Sort Button
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = onFilterClick,
                label = {
                    Text(
                        text = if (state.activeQuickFilters.isEmpty())
                            stringResource(R.string.filter_button)
                        else
                            stringResource(
                                R.string.filter_button_count,
                                state.activeQuickFilters.size
                            )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = if (state.activeQuickFilters.isNotEmpty()) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                },
                border = if (state.activeQuickFilters.isNotEmpty()) null else AssistChipDefaults.assistChipBorder(
                    enabled = true
                )
            )
        }
    }
}
