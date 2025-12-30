package ru.purebytestudio.eventparser.ui.screens.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.purebytestudio.eventparser.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsTopBar(
    isRefreshing: Boolean,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val titleBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            ru.purebytestudio.eventparser.ui.theme.MaterialPink,
            MaterialTheme.colorScheme.tertiary
        )
    )

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        title = {
            Column {
                Text(
                    text = stringResource(R.string.events_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = titleBrush
                    ),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(R.string.events_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.events_refresh)
                    )
                }
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}