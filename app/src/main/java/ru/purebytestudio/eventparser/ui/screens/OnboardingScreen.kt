package ru.purebytestudio.eventparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.purebytestudio.eventparser.domain.repository.UserPreferencesRepository

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color
)

/**
 * Экран онбординга.
 * Показывается при первом запуске приложения и знакомит пользователя с основными функциями.
 */
@Composable
fun OnboardingScreen() {
    val userPreferencesRepository: UserPreferencesRepository = koinInject()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    val pages = listOf(
        OnboardingPage(
            title = "Добро пожаловать!",
            description = "EventParser поможет вам найти интересные IT-мероприятия: митапы, конференции и воркшопы.",
            icon = Icons.Default.FilterList,
            iconTint = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            title = "Избранное",
            description = "Сохраняйте интересные события в избранное, чтобы не пропустить важные мероприятия.",
            icon = Icons.Default.Favorite,
            iconTint = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPage(
            title = "Уведомления",
            description = "Получайте напоминания о предстоящих событиях из избранного.",
            icon = Icons.Default.Notifications,
            iconTint = MaterialTheme.colorScheme.tertiary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            // Индикаторы страниц
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }

            // Кнопки
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < pages.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                scope.launch {
                                    userPreferencesRepository.setOnboardingSeen(true)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) {
                            "Далее"
                        } else {
                            "Начать"
                        }
                    )
                }

                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                userPreferencesRepository.setOnboardingSeen(true)
                            }
                        }
                    ) {
                        Text("Пропустить")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = page.iconTint
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}