package ru.purebytestudio.eventparser.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.purebytestudio.eventparser.R

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val hapticFeedback = LocalHapticFeedback.current
    var animationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    val scale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0.8f,
        animationSpec = spring(),
        label = "empty_state_scale"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .scale(scale)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAction()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
fun NoEventsState(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    EmptyState(
        icon = Icons.Outlined.EventBusy,
        title = stringResource(R.string.empty_events_title),
        description = stringResource(R.string.empty_events_description),
        actionLabel = stringResource(R.string.action_refresh),
        onAction = onRefresh,
        modifier = modifier
    )
}

@Composable
fun NoFavoritesState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.FavoriteBorder,
        title = stringResource(R.string.empty_favorites_title),
        description = stringResource(R.string.empty_favorites_description),
        modifier = modifier
    )
}

@Composable
fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.ErrorOutline,
        title = stringResource(R.string.error_title),
        description = errorMessage,
        actionLabel = stringResource(R.string.error_retry),
        onAction = onRetry,
        modifier = modifier
    )
}