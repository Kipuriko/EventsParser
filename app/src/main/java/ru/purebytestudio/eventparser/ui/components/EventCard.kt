package ru.purebytestudio.eventparser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSource
import ru.purebytestudio.eventparser.domain.model.EventType
import ru.purebytestudio.eventparser.ui.theme.AndroidColor
import ru.purebytestudio.eventparser.ui.theme.DevOpsColor
import ru.purebytestudio.eventparser.ui.theme.ElectricCyan
import ru.purebytestudio.eventparser.ui.theme.ElectricPink
import ru.purebytestudio.eventparser.ui.theme.GameDevColor
import ru.purebytestudio.eventparser.ui.theme.MLColor
import ru.purebytestudio.eventparser.ui.theme.SuccessColor
import ru.purebytestudio.eventparser.ui.theme.VioletPrimary
import ru.purebytestudio.eventparser.ui.theme.WebColor
import ru.purebytestudio.eventparser.ui.theme.iOSColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.File

@Composable
fun EventCard(
    modifier: Modifier = Modifier,
    event: Event,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    showSourceBadge: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column {
            // Обложка события (картинка + градиентная подложка для читаемости)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                val hasImage = event.imageUrl != null || event.localImagePath != null
                if (hasImage) {
                    val context = LocalContext.current
                    val imageModel = if (!event.localImagePath.isNullOrBlank()) {
                        File(event.localImagePath)
                    } else {
                        event.imageUrl
                    }
                    val imageRequest = remember(
                        imageModel,
                        context
                    ) {
                        ImageRequest.Builder(context).data(imageModel)
                            .crossfade(true).crossfade(300)
                            .build()
                    }

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        placeholder = ColorPainter(
                            getCategoryColor(event.category).copy(alpha = 0.3f)
                        ),
                        error = ColorPainter(
                            getCategoryColor(event.category).copy(alpha = 0.5f)
                        )
                    )
                } else {
                    // Если картинки нет — рисуем градиентный плейсхолдер по цвету категории.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        getCategoryColor(event.category),
                                        getCategoryColor(event.category).copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                }

                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                // Event type badge (moved to where source badge was)
                EventTypeBadge(
                    eventType = event.eventType,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )

                // Source badge (optional)
                if (showSourceBadge) {
                    SourceBadge(
                        source = event.source,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }

                // Date badge (Today, Tomorrow, Soon)
                event.dateTime?.let { dateTime ->
                    getDateBadgeType(dateTime)?.let { badgeType ->
                        DateBadge(
                            badgeType = badgeType,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )
                    }
                }

                // Online indicator
                if (event.isOnline) {
                    OnlineBadge(
                        modifier = Modifier
                            .align(
                                if (event.dateTime != null && getDateBadgeType(event.dateTime) != null) Alignment.BottomCenter
                                else Alignment.BottomStart
                            )
                            .padding(12.dp)
                    )
                }

                // Favorite button
                FavoriteButton(
                    isFavorite = event.isFavorite,
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Строка: дата и локация
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Дата
                    if (event.dateTime != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = stringResource(R.string.event_detail_date),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatEventDate(event),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Location
                    if (event.location != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(
                                1f,
                                fill = false
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.event_detail_location),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Price if available
                event.price?.let { price ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = price,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (price.lowercase().contains("бесплатно") || price.lowercase()
                                .contains("free")
                        ) {
                            SuccessColor
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                }

                // Tags
                if (event.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TagsRow(tags = event.tags.take(4))
                }
            }
        }
    }
}

@Composable
fun SourceBadge(
    modifier: Modifier = Modifier,
    source: EventSource
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    ) {
        Text(
            text = source.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun CategoryBadge(
    modifier: Modifier = Modifier,
    category: EventCategory
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(getCategoryColor(category).copy(alpha = 0.9f))
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    ) {
        Text(
            text = "${category.emoji} ${category.displayName}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun EventTypeBadge(
    modifier: Modifier = Modifier,
    eventType: EventType
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(getEventTypeColor(eventType).copy(alpha = 0.9f))
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    ) {
        Text(
            text = eventType.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsRow(
    modifier: Modifier = Modifier,
    tags: List<String>
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.height(24.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
fun OnlineBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ElectricCyan.copy(alpha = 0.9f))
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudQueue,
            contentDescription = stringResource(R.string.online),
            modifier = Modifier.size(14.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Онлайн",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.1f else 1f,
        animationSpec = spring(),
        label = "favorite_scale"
    )

    val tint by animateColorAsState(
        targetValue = if (isFavorite) ElectricPink else Color.White.copy(alpha = 0.8f),
        label = "favorite_color"
    )

    IconButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Удалить из избранного" else "Добавить в избранное",
            tint = tint
        )
    }
}

fun getCategoryColor(category: EventCategory): Color {
    return when (category) {
        EventCategory.ANDROID_DEV -> AndroidColor
        EventCategory.GAME_DEV -> GameDevColor
        EventCategory.IOS_DEV -> iOSColor
        EventCategory.WEB_DEV -> WebColor
        EventCategory.ML_AI -> MLColor
        EventCategory.DEVOPS -> DevOpsColor
        EventCategory.DESIGN -> ElectricPink
        EventCategory.DATA_SCIENCE -> ElectricCyan
        EventCategory.SECURITY -> Color(0xFFE91E63)
        EventCategory.BLOCKCHAIN -> Color(0xFFF57C00)
        EventCategory.MANAGEMENT -> Color(0xFF7B1FA2)
        EventCategory.QA -> Color(0xFF00897B)
        EventCategory.OTHER -> VioletPrimary
    }
}

fun getEventTypeColor(eventType: EventType): Color {
    return when (eventType) {
        EventType.MEETUP -> Color(0xFFE91E63)
        EventType.CONFERENCE -> Color(0xFF673AB7)
        EventType.WORKSHOP -> Color(0xFF00BCD4)
        EventType.HACKATHON -> Color(0xFFFF5722)
        EventType.GAME_JAM -> GameDevColor
        EventType.WEBINAR -> Color(0xFF2196F3)
        EventType.STREAM -> Color(0xFF3DDC84)
        EventType.DIGEST -> Color(0xFF607D8B)
        EventType.CONTEST -> Color(0xFFFF9800)
        EventType.FESTIVAL -> Color(0xFF9C27B0)
        EventType.QUEST -> Color(0xFF4CAF50)
        EventType.ACCELERATOR -> Color(0xFF3F51B5)
        EventType.OTHER -> VioletPrimary
    }
}

private fun formatEventDate(event: Event): String {
    val dateTime = event.dateTime ?: return ""
    val formatter = if (event.hasSpecificTime) {
        DateTimeFormatter.ofPattern(
            "d MMM, HH:mm",
            Locale.forLanguageTag("ru")
        )
    } else {
        DateTimeFormatter.ofPattern(
            "d MMM",
            Locale.forLanguageTag("ru")
        )
    }
    return dateTime.format(formatter)
}

private enum class DateBadgeType {
    TODAY, TOMORROW, SOON
}

@Composable
private fun DateBadge(
    badgeType: DateBadgeType,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (badgeType) {
        DateBadgeType.TODAY -> stringResource(R.string.badge_today) to ElectricPink
        DateBadgeType.TOMORROW -> stringResource(R.string.badge_tomorrow) to ElectricCyan
        DateBadgeType.SOON -> stringResource(R.string.badge_soon) to SuccessColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.9f))
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun getDateBadgeType(dateTime: java.time.LocalDateTime): DateBadgeType? {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val weekLater = today.plusDays(7)

    val eventDate = dateTime.toLocalDate()

    return when {
        eventDate == today -> DateBadgeType.TODAY
        eventDate == tomorrow -> DateBadgeType.TOMORROW
        eventDate.isAfter(today) && eventDate.isBefore(weekLater) -> DateBadgeType.SOON
        else -> null
    }
}