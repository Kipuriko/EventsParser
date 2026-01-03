package ru.purebytestudio.eventparser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import ru.purebytestudio.eventparser.domain.repository.UserPreferencesRepository
import ru.purebytestudio.eventparser.navigation.AppNavigation
import ru.purebytestudio.eventparser.navigation.EventParserBottomBar
import ru.purebytestudio.eventparser.navigation.Screen
import ru.purebytestudio.eventparser.navigation.bottomNavItems
import ru.purebytestudio.eventparser.ui.components.NotificationsProvider
import ru.purebytestudio.eventparser.ui.permissions.PostNotificationsPermissionRequester
import ru.purebytestudio.eventparser.ui.screens.OnboardingScreen
import ru.purebytestudio.eventparser.ui.theme.EventParserTheme

/**
 * Главная Activity приложения.
 * Управляет навигацией, темой и отображением Splash Screen / Onboarding.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Обработка deep links и уведомлений
        val eventId = intent.getStringExtra("event_id") ?: intent.data?.lastPathSegment

        setContent {
            val userPreferencesRepository: UserPreferencesRepository = koinInject()
            val isDarkTheme by userPreferencesRepository.isDarkTheme.collectAsState(initial = null)
            val hasRequestedPostNotificationsPermission by userPreferencesRepository
                .hasRequestedPostNotificationsPermission
                .collectAsState(initial = false)

            EventParserTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotificationsProvider {
                        PostNotificationsPermissionRequester(
                            shouldRequest = !hasRequestedPostNotificationsPermission,
                            markRequested = {
                                userPreferencesRepository.setHasRequestedPostNotificationsPermission(
                                    requested = true
                                )
                            }
                        )
                        MainContent(
                            eventId = eventId,
                            darkThemePref = isDarkTheme
                        )
                    }
                }
            }
        }

        splashScreen.setKeepOnScreenCondition { false }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Повторная обработка при новом intent
        recreate()
    }
}

/**
 * Основной контент приложения с логикой отображения Onboarding или главной навигации.
 */
@Composable
private fun MainContent(
    eventId: String?,
    darkThemePref: Boolean?
) {
    val userPreferencesRepository: UserPreferencesRepository = koinInject()
    val hasSeenOnboarding by userPreferencesRepository.hasSeenOnboarding.collectAsState(initial = null)

    when (hasSeenOnboarding) {
        false -> {
            OnboardingScreen()
        }

        true -> {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }
            var isBottomBarVisible by remember { mutableStateOf(true) }
            val useDarkTheme = darkThemePref ?: isSystemInDarkTheme()

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (available.y < -5) {
                            isBottomBarVisible = false
                        } else if (available.y > 5) {
                            isBottomBarVisible = true
                        }
                        return Offset.Zero
                    }
                }
            }

            // Навигация к событию, если открыто из уведомления или deep link
            eventId?.let { id ->
                LaunchedEffect(id) {
                    navController.navigate(Screen.EventDetail.createRoute(id))
                }
            }

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar && isBottomBarVisible,
                        enter = slideInVertically(initialOffsetY = { it }) + expandVertically(),
                        exit = slideOutVertically(targetOffsetY = { it }) + shrinkVertically()
                    ) {
                        EventParserBottomBar(
                            navController = navController,
                            currentRoute = currentDestination?.route,
                            isDarkTheme = useDarkTheme
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        navController = navController,
                        contentPadding = innerPadding
                    )
                }
            }
        }

        null -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {}
        }
    }
}