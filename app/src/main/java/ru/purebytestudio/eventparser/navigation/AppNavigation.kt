package ru.purebytestudio.eventparser.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.ui.screens.EventDetailScreen
import ru.purebytestudio.eventparser.ui.screens.FavoritesScreen
import ru.purebytestudio.eventparser.ui.screens.SettingsScreen
import ru.purebytestudio.eventparser.ui.screens.events.EventsScreen

/**
 * Описание экранов и маршрутов приложения.
 *
 * Мы держим `route` и ресурсы UI (заголовок/иконки) в одном месте, чтобы:
 * - нижняя навигация и NavHost использовали единый источник правды;
 * - было проще добавлять новые экраны, не забывая про заголовки/иконки.
 */
sealed class Screen(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Events : Screen(
        route = "events",
        titleResId = R.string.nav_events,
        selectedIcon = Icons.Filled.Event,
        unselectedIcon = Icons.Outlined.Event
    )

    data object Favorites : Screen(
        route = "favorites",
        titleResId = R.string.nav_favorites,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )

    data object EventDetail : Screen(
        route = "event/{eventId}",
        titleResId = R.string.nav_details,
        selectedIcon = Icons.Filled.Event,
        unselectedIcon = Icons.Outlined.Event
    ) {
        fun createRoute(eventId: String) = "event/$eventId"
    }

    data object Settings : Screen(
        route = "settings",
        titleResId = R.string.settings_title,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavItems = listOf(
    Screen.Events,
    Screen.Favorites
)

/**
 * Корневой NavHost приложения.
 *
 * Если `navController` не передан, создаём внутренний (удобно для превью/композиции).
 * `contentPadding` пробрасывается из `Scaffold` (например, чтобы списки не залезали под bottom bar).
 */
@Composable
fun AppNavigation(
    navController: NavHostController? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val defaultNavController = rememberNavController()
    val finalNavController = navController ?: defaultNavController
    val tabs = listOf(
        Screen.Events.route,
        Screen.Favorites.route
    )

    NavHost(
        navController = finalNavController,
        startDestination = Screen.Events.route,
        enterTransition = {
            val initial = initialState.destination.route
            val target = targetState.destination.route
            if (initial in tabs && target in tabs) {
                fadeIn(animationSpec = tween(300))
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            }
        },
        exitTransition = {
            val initial = initialState.destination.route
            val target = targetState.destination.route
            if (initial in tabs && target in tabs) {
                fadeOut(animationSpec = tween(300))
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        },
        popEnterTransition = {
            val initial = initialState.destination.route
            val target = targetState.destination.route
            if (initial in tabs && target in tabs) {
                fadeIn(animationSpec = tween(300))
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            }
        },
        popExitTransition = {
            val initial = initialState.destination.route
            val target = targetState.destination.route
            if (initial in tabs && target in tabs) {
                fadeOut(animationSpec = tween(300))
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        }) {
        composable(Screen.Events.route) {
            EventsScreen(
                onEventClick = { eventId ->
                    finalNavController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                onSettingsClick = {
                    finalNavController.navigate(Screen.Settings.route)
                },
                contentPadding = contentPadding
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onEventClick = { eventId ->
                    finalNavController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                contentPadding = contentPadding
            )
        }

        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType })
        ) {
            EventDetailScreen(onNavigateBack = { finalNavController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { finalNavController.popBackStack() })
        }
    }
}

/**
 * Нижняя навигация (таббар) с сохранением состояния вкладок.
 *
 * Ключевой момент: `saveState/restoreState` позволяют не пересоздавать стэк вкладок при переключениях,
 * а `launchSingleTop` предотвращает дублирование одного и того же экрана в back stack.
 */
@Composable
fun EventParserBottomBar(
    navController: NavHostController,
    currentRoute: String?
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp
                )
            )
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp
                )
            ),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            val label = stringResource(screen.titleResId)

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}