package ru.purebytestudio.eventparser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

enum class NotificationType { Success, Error, Info }

data class NotificationMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: NotificationType = NotificationType.Info,
    val durationMs: Long = 2500L
)

class Notifier internal constructor(
    private val events: MutableSharedFlow<NotificationMessage>,
    private val scope: CoroutineScope
) {
    fun showInfo(
        message: String,
        durationMs: Long = 2500L
    ) {
        post(
            message = message,
            type = NotificationType.Info,
            durationMs = durationMs
        )
    }

    fun showSuccess(
        message: String,
        durationMs: Long = 2500L
    ) {
        post(
            message = message,
            type = NotificationType.Success,
            durationMs = durationMs
        )
    }

    fun showError(
        message: String,
        durationMs: Long = 3000L
    ) {
        post(
            message = message,
            type = NotificationType.Error,
            durationMs = durationMs
        )
    }

    fun post(
        message: String,
        type: NotificationType = NotificationType.Info,
        durationMs: Long = 2500L
    ) {
        scope.launch {
            events.emit(
                NotificationMessage(
                    message = message,
                    type = type,
                    durationMs = durationMs
                )
            )
        }
    }
}

val LocalNotifier = compositionLocalOf<Notifier> {
    error("LocalNotifier not provided")
}

@Composable
fun NotificationsProvider(content: @Composable () -> Unit) {
    val events = remember { MutableSharedFlow<NotificationMessage>(extraBufferCapacity = 64) }
    val scope = rememberCoroutineScope()
    val notifier = remember {
        Notifier(
            events = events,
            scope = scope
        )
    }

    CompositionLocalProvider(LocalNotifier provides notifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            NotificationsHost(events)
        }
    }
}

@Composable
private fun NotificationsHost(events: MutableSharedFlow<NotificationMessage>) {
    val messages = remember { mutableStateListOf<NotificationMessage>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(events) {
        events.collect { msg ->
            // Не добавляем повтор, если такой же (по тексту и типу) уже отображается
            val isDuplicateVisible =
                messages.any { it.message == msg.message && it.type == msg.type }
            if (!isDuplicateVisible) {
                messages.add(msg)
                val id = msg.id
                scope.launch {
                    delay(msg.durationMs)
                    messages.removeAll { it.id == id }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(
                    top = 8.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            messages.take(3).forEach { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    NotificationCard(
                        message = message
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(message: NotificationMessage) {
    val containerColor = MaterialTheme.colorScheme.inverseSurface
    val contentColor = MaterialTheme.colorScheme.inverseOnSurface

    val leadingIcon = when (message.type) {
        NotificationType.Success -> Icons.Default.CheckCircle
        NotificationType.Error -> Icons.Default.Error
        NotificationType.Info -> Icons.Default.Info
    }

    // Tint для иконок в зависимости от типа, но на темном фоне
    val iconColor = when (message.type) {
        NotificationType.Success -> Color(0xFF4CAF50)
        NotificationType.Error -> Color(0xFFF44336)
        NotificationType.Info -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )

            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
        }
    }
}