package ru.purebytestudio.eventparser.ui.screens

import android.content.ClipData
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.data.export.IcsExporter
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.presentation.detail.EventDetailSideEffect
import ru.purebytestudio.eventparser.presentation.detail.EventDetailViewModel
import ru.purebytestudio.eventparser.ui.theme.ElectricPink
import timber.log.Timber
import java.util.Locale

/**
 * Экран детальной информации о событии.
 * Показывает полное описание, местоположение, дату, организатора и позволяет добавить в избранное, календарь или поделиться.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: EventDetailViewModel = koinViewModel()
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val shareChooserText = stringResource(R.string.share_chooser)
    val shareTextTemplate = stringResource(R.string.share_text)
    val icsExporter: IcsExporter = koinInject()
    val icsChooser = stringResource(R.string.event_detail_share_ics_chooser)
    val shareIcsTitle = stringResource(R.string.event_detail_share_ics)

    var shareMenuExpanded by remember { mutableStateOf(false) }

    fun shareIcs(event: Event) {
        runCatching {
            val uri = icsExporter.exportToCache(event)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(
                    context.contentResolver,
                    "event.ics",
                    uri
                )
            }
            context.startActivity(
                Intent.createChooser(
                    intent,
                    icsChooser
                )
            )
        }.onFailure {
            Timber.e(
                it,
                "Failed to share .ics"
            )
        }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is EventDetailSideEffect.OpenUrl -> {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    sideEffect.url.toUri()
                )
                context.startActivity(intent)
            }

            is EventDetailSideEffect.ShareEvent -> {
                val shareText = String.format(
                    Locale.getDefault(),
                    shareTextTemplate,
                    sideEffect.event.title,
                    sideEffect.event.description,
                    sideEffect.event.url
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        sideEffect.event.title
                    )
                    putExtra(
                        Intent.EXTRA_TEXT,
                        shareText
                    )
                }
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        shareChooserText
                    )
                )
            }

            is EventDetailSideEffect.AddToCalendar -> {
                val event = sideEffect.event
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(
                        CalendarContract.Events.TITLE,
                        event.title
                    )
                    putExtra(
                        CalendarContract.Events.DESCRIPTION,
                        event.description
                    )
                    event.dateTime?.let { dateTime ->
                        val startMillis =
                            dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()
                                .toEpochMilli()
                        putExtra(
                            CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                            startMillis
                        )
                        // По умолчанию событие длится 2 часа
                        putExtra(
                            CalendarContract.EXTRA_EVENT_END_TIME,
                            startMillis + 2 * 60 * 60 * 1000
                        )
                    }
                    event.location?.let {
                        putExtra(
                            CalendarContract.Events.EVENT_LOCATION,
                            it
                        )
                    }
                    // URL события - используем строковую константу напрямую
                    putExtra(
                        "url",
                        event.url
                    )
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Если нет приложения календаря, логируем ошибку
                    Timber.e(
                        t = e,
                        message = "Failed to open calendar"
                    )
                }
            }

            EventDetailSideEffect.NavigateBack -> {
                onNavigateBack()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: stringResource(R.string.error_title),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            state.event != null -> {
                EventDetailContent(
                    event = state.event!!,
                    onOpenUrl = { viewModel.openEventUrl() },
                    onAddToCalendar = { viewModel.addToCalendar() })
            }
        }

        // TopAppBar overlay
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.event_detail_back)
                    )
                }
            },
            actions = {
                state.event?.let { event ->
                    Box {
                        IconButton(onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            shareMenuExpanded = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.event_detail_share)
                            )
                        }

                        DropdownMenu(
                            expanded = shareMenuExpanded,
                            onDismissRequest = { shareMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.event_detail_share)) },
                                onClick = {
                                    shareMenuExpanded = false
                                    viewModel.shareEvent()
                                }
                            )

                            if (event.dateTime != null) {
                                DropdownMenuItem(
                                    text = { Text(shareIcsTitle) },
                                    onClick = {
                                        shareMenuExpanded = false
                                        shareIcs(event)
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleFavorite()
                    }) {
                        Icon(
                            imageVector = if (event.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (event.isFavorite) stringResource(R.string.event_detail_favorite_remove) else stringResource(
                                R.string.event_detail_favorite_add
                            ),
                            tint = if (event.isFavorite) ElectricPink else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            windowInsets = TopAppBarDefaults.windowInsets,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}