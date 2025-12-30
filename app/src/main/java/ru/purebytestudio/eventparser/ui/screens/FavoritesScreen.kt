package ru.purebytestudio.eventparser.ui.screens

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.presentation.favorites.FavoritesSideEffect
import ru.purebytestudio.eventparser.presentation.favorites.FavoritesViewModel
import ru.purebytestudio.eventparser.ui.components.EventCard
import ru.purebytestudio.eventparser.ui.components.LocalNotifier
import ru.purebytestudio.eventparser.ui.components.NoFavoritesState
import ru.purebytestudio.eventparser.ui.components.ShimmerEventsList

/**
 * Экран "Избранное".
 * Отображает список сохраненных пользователем событий.
 * Позволяет экспортировать список в JSON.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onEventClick: (String) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val notifier = LocalNotifier.current
    val exportTitle = stringResource(R.string.favorites_export_title)
    val exportDoneMessage = stringResource(R.string.favorites_export_done)
    val exportDoneMessageState = rememberUpdatedState(exportDoneMessage)

    var importSuccessToken by remember { mutableLongStateOf(0L) }
    val importDoneFormat = stringResource(R.string.favorites_import_done_format)
    val importDoneFormatState = rememberUpdatedState(importDoneFormat)
    var importSuccessMessage by remember { mutableStateOf("") }

    val latestViewModel by rememberUpdatedState(viewModel)

    LaunchedEffect(importSuccessToken) {
        if (importSuccessToken == 0L) return@LaunchedEffect
        notifier.showSuccess(importSuccessMessage)
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri: Uri? ->
            if (uri != null) latestViewModel.exportFavoritesTo(uri)
        }
    )
    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) latestViewModel.importFavoritesFrom(uri)
        }
    )

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is FavoritesSideEffect.NavigateToDetail -> {
                onEventClick(sideEffect.eventId)
            }

            is FavoritesSideEffect.ExportSuccess -> {
                val uri = sideEffect.uri
                notifier.showSuccess(exportDoneMessageState.value)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newUri(
                        context.contentResolver,
                        "favorites.json",
                        uri
                    )
                }
                runCatching {
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            exportTitle
                        )
                    )
                }
            }

            is FavoritesSideEffect.ExportError -> {
                notifier.showError(sideEffect.message)
            }

            is FavoritesSideEffect.ImportSuccess -> {
                importSuccessMessage = String.format(
                    importDoneFormatState.value,
                    sideEffect.count
                )
                importSuccessToken++
            }

            is FavoritesSideEffect.ImportError -> {
                notifier.showError(sideEffect.message)
            }
        }
    }

    // Background Gradient matching EventsScreen logic
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
            .background(backgroundBrush),
        containerColor = Color.Transparent,
        topBar = {
            FavoritesTopBar(
                onExportClick = {
                    createJsonLauncher.launch("eventparser-favorites.json")
                },
                onImportClick = {
                    openJsonLauncher.launch(
                        arrayOf(
                            "application/json",
                            "text/*"
                        )
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when {
                state.isLoading -> {
                    ShimmerEventsList(
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        )
                    )
                }

                state.events.isEmpty() -> {
                    NoFavoritesState(
                        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 16.dp,
                            bottom = contentPadding.calculateBottomPadding() + 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(
                            items = state.events,
                            key = { _, event -> event.id }
                        ) { _, event ->
                            EventCard(
                                event = event,
                                onClick = remember(event.id) { { viewModel.onEventClick(event.id) } },
                                onFavoriteClick = remember(event.id) { { viewModel.toggleFavorite(event.id) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesTopBar(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    // Градиент для TopBar.
    // Используем тот же подход, что и в EventsScreen, чтобы UI выглядел единообразно.
    val titleBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFFE91E63),
            MaterialTheme.colorScheme.tertiary
        )
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Избранное",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            brush = titleBrush
                        ),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Ваши сохраненные мероприятия",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = onImportClick) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = stringResource(R.string.favorites_import_title)
                    )
                }
                IconButton(onClick = onExportClick) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = stringResource(R.string.favorites_export_title)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}