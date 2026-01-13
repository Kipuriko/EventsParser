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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.remember
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
import ru.purebytestudio.eventparser.presentation.settings.SettingsState
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
    val notifier = LocalNotifier.current
    val context = LocalContext.current

    val cacheClearedMessage = stringResource(R.string.settings_cache_cleared)
    val shareChooserText = stringResource(R.string.share_chooser)
    val appLink = remember {
        "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
    }
    val shareText = stringResource(
        R.string.settings_share_app_text,
        appLink
    )

    val duplicatesCleanedMessage = stringResource(R.string.settings_duplicates_cleaned_format)
    
    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is SettingsSideEffect.ShowMessage -> {
                notifier.showInfo(sideEffect.message)
            }

            SettingsSideEffect.CacheCleared -> {
                notifier.showSuccess(cacheClearedMessage)
            }

            is SettingsSideEffect.DuplicatesCleaned -> {
                val message = duplicatesCleanedMessage.format(sideEffect.count)
                if (sideEffect.count > 0) {
                    notifier.showSuccess(message)
                } else {
                    notifier.showInfo("Дубликаты не найдены")
                }
            }
        }
    }

    // Фоновый градиент (как в EventsScreen)
    val surface = MaterialTheme.colorScheme.surface
    val surfaceLowest = MaterialTheme.colorScheme.surfaceContainerLowest
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow
    val backgroundBrush = remember(
        surface,
        surfaceLowest,
        surfaceLow
    ) {
        Brush.verticalGradient(
            colors = listOf(
                surface,
                surfaceLowest,
                surfaceLow
            )
        )
    }

    // Градиент для заголовка
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val titleBrush = remember(
        primary,
        tertiary
    ) {
        Brush.linearGradient(
            colors = listOf(
                primary,
                Color(0xFFE91E63),
                tertiary
            )
        )
    }

    val onShareClick = remember(
        context,
        shareChooserText,
        shareText
    ) {
        {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    shareText
                )
            }
            context.startActivity(
                Intent.createChooser(
                    intent,
                    shareChooserText
                )
            )
        }
    }

    val onRateClick = remember(
        context,
        appLink
    ) {
        {
            openUrlSafely(
                context = context,
                url = appLink
            )
        }
    }

    val onOpenNotificationSettings = remember(context) {
        { openAppNotificationSettings(context) }
    }

    val onOpenAppDetailsSettings = remember(context) {
        { openAppDetailsSettings(context) }
    }

    val feedbackEmail = remember { "kipuriko@gmail.com" }
    val feedbackChooserTitle = stringResource(R.string.settings_feedback_chooser)
    val feedbackSubject = stringResource(
        R.string.settings_feedback_email_subject_format,
        BuildConfig.VERSION_NAME
    )
    val feedbackBody = stringResource(R.string.settings_feedback_email_body)
    val onFeedbackClick = remember(
        context,
        feedbackEmail,
        feedbackSubject,
        feedbackBody,
        feedbackChooserTitle
    ) {
        {
            openEmailComposer(
                context = context,
                email = feedbackEmail,
                subject = feedbackSubject,
                body = feedbackBody,
                chooserTitle = feedbackChooserTitle
            )
        }
    }

    val donationUrl = remember { "https://pay.cloudtips.ru/p/d48da922" }
    val onDonationClick = remember(
        context,
        donationUrl
    ) {
        {
            openUrlSafely(
                context = context,
                url = donationUrl
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                SettingsTopBar(
                    titleBrush = titleBrush,
                    onNavigateBack = onNavigateBack
                )
            }
        ) { paddingValues ->
            SettingsContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = state,
                onThemeSelected = { isDark ->
                    if (isDark == null) {
                        viewModel.resetThemeToSystem()
                    } else {
                        viewModel.setDarkTheme(isDark)
                    }
                },
                onClearCacheClick = viewModel::clearCache,
                onCleanupDuplicatesClick = viewModel::cleanupDuplicates,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenAppSettings = onOpenAppDetailsSettings,
                onShareApp = onShareClick,
                onFeedbackClick = onFeedbackClick,
                onRateApp = onRateClick,
                onDonationClick = onDonationClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    titleBrush: Brush,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        windowInsets = TopAppBarDefaults.windowInsets,
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall.copy(brush = titleBrush),
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
}

@Composable
private fun SettingsContent(
    modifier: Modifier,
    state: SettingsState,
    onThemeSelected: (Boolean?) -> Unit,
    onClearCacheClick: () -> Unit,
    onCleanupDuplicatesClick: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onShareApp: () -> Unit,
    onFeedbackClick: () -> Unit,
    onRateApp: () -> Unit,
    onDonationClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(title = stringResource(R.string.settings_theme))
            Spacer(modifier = Modifier.height(8.dp))
            ThemeSelectorRow(
                currentTheme = state.isDarkTheme,
                onThemeSelected = onThemeSelected
            )
        }

        item {
            SectionTitle(title = stringResource(R.string.settings_actions))
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                Column {
                    SettingsActionItem(
                        title = stringResource(R.string.settings_clear_cache),
                        subtitle = stringResource(R.string.settings_clear_cache_subtitle),
                        icon = Icons.Default.Delete,
                        onClick = onClearCacheClick,
                        showDivider = true,
                        isLoading = false
                    )
                    SettingsActionItem(
                        title = stringResource(R.string.settings_cleanup_duplicates),
                        subtitle = stringResource(R.string.settings_cleanup_duplicates_subtitle),
                        icon = Icons.Default.Delete,
                        onClick = onCleanupDuplicatesClick,
                        showDivider = false,
                        isLoading = state.isCleaningDuplicates
                    )
                }
            }
        }

        item {
            SectionTitle(title = stringResource(R.string.settings_system))
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                Column {
                    SettingsActionItem(
                        title = stringResource(R.string.settings_notifications),
                        subtitle = stringResource(R.string.settings_notifications_subtitle),
                        icon = Icons.Default.Notifications,
                        onClick = onOpenNotificationSettings
                    )
                    SettingsActionItem(
                        title = stringResource(R.string.settings_app_settings),
                        subtitle = stringResource(R.string.settings_app_settings_subtitle),
                        icon = Icons.Default.Android,
                        onClick = onOpenAppSettings,
                        showDivider = false
                    )
                }
            }
        }

        item {
            SectionTitle(title = stringResource(R.string.settings_more))
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                Column {
                    SettingsActionItem(
                        title = stringResource(R.string.settings_share_app),
                        subtitle = stringResource(R.string.settings_share_app_subtitle),
                        icon = Icons.Default.Share,
                        onClick = onShareApp
                    )
                    SettingsActionItem(
                        title = stringResource(R.string.settings_feedback_title),
                        subtitle = stringResource(R.string.settings_feedback_subtitle),
                        icon = Icons.Default.Email,
                        onClick = onFeedbackClick
                    )
                    SettingsActionItem(
                        title = stringResource(R.string.settings_rate_app),
                        subtitle = stringResource(R.string.settings_rate_app_subtitle),
                        icon = Icons.Default.StarRate,
                        onClick = onRateApp,
                        showDivider = false
                    )
                }
            }
        }

        item {
            SectionTitle(title = stringResource(R.string.settings_donation))
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                Column {
                    SettingsActionItem(
                        title = stringResource(R.string.settings_donation_action),
                        icon = Icons.Default.Favorite,
                        onClick = onDonationClick,
                        showDivider = false
                    )
                }
            }
        }

        item {
            SectionTitle(title = stringResource(R.string.settings_about))
            Spacer(modifier = Modifier.height(8.dp))
            AboutSection()
        }
    }
}

@Composable
private fun AboutSection() {
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
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Text(
                text = stringResource(R.string.settings_app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}