package ru.purebytestudio.eventparser.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.purebytestudio.eventparser.BuildConfig
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.presentation.settings.SettingsSideEffect
import ru.purebytestudio.eventparser.presentation.settings.SettingsViewModel
import ru.purebytestudio.eventparser.ui.components.LocalNotifier

/**
 * Экран настроек приложения.
 * Позволяет изменить тему, очистить кэш и посмотреть информацию о приложении.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.collectAsState()
    val cacheClearedMessage = stringResource(R.string.settings_cache_cleared)
    val notifier = LocalNotifier.current
    val context = LocalContext.current
    val shareChooserText = stringResource(R.string.share_chooser)
    val appLink = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
    val shareText = stringResource(
        R.string.settings_share_app_text,
        appLink
    )
    val shareTextState = rememberUpdatedState(shareText)

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is SettingsSideEffect.ShowMessage -> {
                notifier.showInfo(sideEffect.message)
            }

            SettingsSideEffect.CacheCleared -> {
                notifier.showSuccess(cacheClearedMessage)
            }
        }
    }

    // Фоновый градиент (как в EventsScreen)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.surfaceContainerLow
        )
    )

    // Градиент для заголовка
    val titleBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFFE91E63), // розовый акцент
            MaterialTheme.colorScheme.tertiary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .background(backgroundBrush)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = TopAppBarDefaults.windowInsets,
                    title = {
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                brush = titleBrush
                            ),
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.event_detail_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Theme section
                item {
                    SectionTitle(title = stringResource(R.string.settings_theme))
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeSelectorRow(
                        currentTheme = state.isDarkTheme,
                        onThemeSelected = { isDark ->
                            if (isDark == null) {
                                viewModel.resetThemeToSystem()
                            } else {
                                viewModel.setDarkTheme(isDark)
                            }
                        })
                }

                // Actions section
                item {
                    SectionTitle(title = stringResource(R.string.settings_actions))
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCard {
                        Column {
                            SettingsActionItem(
                                title = stringResource(R.string.settings_clear_cache),
                                subtitle = stringResource(R.string.settings_clear_cache_subtitle),
                                icon = Icons.Default.Delete,
                                onClick = { viewModel.clearCache() },
                                showDivider = false
                            )
                        }
                    }
                }

                // Notifications & system section
                item {
                    SectionTitle(title = stringResource(R.string.settings_system))
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCard {
                        Column {
                            SettingsActionItem(
                                title = stringResource(R.string.settings_notifications),
                                subtitle = stringResource(R.string.settings_notifications_subtitle),
                                icon = Icons.Default.Notifications,
                                onClick = { openAppNotificationSettings(context) }
                            )
                            SettingsActionItem(
                                title = stringResource(R.string.settings_app_settings),
                                subtitle = stringResource(R.string.settings_app_settings_subtitle),
                                icon = Icons.Default.Android,
                                onClick = { openAppDetailsSettings(context) },
                                showDivider = false
                            )
                        }
                    }
                }

                // More section
                item {
                    SectionTitle(title = stringResource(R.string.settings_more))
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCard {
                        Column {
                            SettingsActionItem(
                                title = stringResource(R.string.settings_share_app),
                                subtitle = stringResource(R.string.settings_share_app_subtitle),
                                icon = Icons.Default.Share,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            shareTextState.value
                                        )
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            intent,
                                            shareChooserText
                                        )
                                    )
                                }
                            )
                            SettingsActionItem(
                                title = stringResource(R.string.settings_rate_app),
                                subtitle = stringResource(R.string.settings_rate_app_subtitle),
                                icon = Icons.Default.StarRate,
                                onClick = {
                                    val appLink =
                                        "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
                                    openUrlSafely(
                                        context,
                                        appLink
                                    )
                                },
                                showDivider = false
                            )
                        }
                    }
                }

                // About section
                item {
                    SectionTitle(title = stringResource(R.string.settings_about))
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.size(16.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_app_display_name),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.settings_app_version_format,
                                            BuildConfig.VERSION_NAME
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                )
                            )
                            Text(
                                text = stringResource(R.string.settings_app_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}