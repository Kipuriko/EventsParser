package ru.purebytestudio.eventparser.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.domain.model.EventSortType
import ru.purebytestudio.eventparser.domain.model.QuickFilter
import ru.purebytestudio.eventparser.presentation.events.EventsState
import ru.purebytestudio.eventparser.ui.components.QuickFilterChip

@Composable
fun FilterSheetContent(
    state: EventsState,
    onSortSelected: (EventSortType) -> Unit,
    onQuickFilterToggle: (QuickFilter) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = 24.dp,
                vertical = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Sort Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.filters_sort_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventSortType.entries.forEach { sortType ->
                    val isSelected = state.sortType == sortType
                    val icon = when (sortType) {
                        EventSortType.DATE_ASC, EventSortType.DATE_DESC -> Icons.Default.DateRange
                        EventSortType.TITLE_ASC, EventSortType.TITLE_DESC -> Icons.Default.SortByAlpha
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSortSelected(sortType) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.3f
                                ) else Color.Transparent
                            )
                            .padding(
                                vertical = 12.dp,
                                horizontal = 16.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = sortType.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Quick Filters Section + Apply Button (make gap smaller)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.filters_quick_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickFilter.entries.forEach { filter ->
                    QuickFilterChip(
                        filter = filter,
                        isSelected = state.activeQuickFilters.contains(filter),
                        onClick = { onQuickFilterToggle(filter) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Gradient Apply Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                ru.purebytestudio.eventparser.ui.theme.MaterialPink // Pink accent to match title
                            )
                        )
                    )
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.filters_apply),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}