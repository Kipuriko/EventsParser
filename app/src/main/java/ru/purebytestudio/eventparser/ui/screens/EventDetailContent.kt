package ru.purebytestudio.eventparser.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ru.purebytestudio.eventparser.R
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.ui.components.CategoryBadge
import ru.purebytestudio.eventparser.ui.components.EventTypeBadge
import ru.purebytestudio.eventparser.ui.components.LinkText
import ru.purebytestudio.eventparser.ui.components.TagsRow
import ru.purebytestudio.eventparser.ui.components.getCategoryColor
import ru.purebytestudio.eventparser.ui.theme.ElectricCyan
import ru.purebytestudio.eventparser.ui.theme.ElectricPink
import ru.purebytestudio.eventparser.ui.theme.MaterialPink
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun EventDetailContent(
    event: Event,
    onOpenUrl: () -> Unit,
    onAddToCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val hapticFeedback = LocalHapticFeedback.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
        ) {
            // Hero image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                if (event.imageUrl != null) {
                    val context = LocalContext.current
                    val imageRequest = remember(
                        event.imageUrl,
                        context
                    ) {
                        ImageRequest.Builder(context).data(event.imageUrl)
                            .crossfade(true).crossfade(400).memoryCacheKey(event.imageUrl)
                            .diskCacheKey(event.imageUrl).build()
                    }

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        placeholder = androidx.compose.ui.graphics.painter.ColorPainter(
                            getCategoryColor(event.category).copy(alpha = 0.3f)
                        ),
                        error = androidx.compose.ui.graphics.painter.ColorPainter(
                            getCategoryColor(event.category).copy(alpha = 0.5f)
                        )
                    )
                } else {
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

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventTypeBadge(eventType = event.eventType)
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideInVertically(
                    spring(stiffness = Spring.StiffnessLow)
                ) { it / 4 }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Title
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category badge
                    CategoryBadge(category = event.category)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Online indicator
                    if (event.isOnline) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricCyan.copy(alpha = 0.1f))
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.event_detail_online),
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Info cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        event.dateTime?.let { dateTime ->
                            InfoRow(
                                icon = Icons.Default.Schedule,
                                label = stringResource(R.string.event_detail_date),
                                value = formatFullDate(
                                    dateTime,
                                    event.hasSpecificTime
                                ),
                                iconTint = MaterialTheme.colorScheme.primary
                            )
                        }

                        event.registrationDeadline?.let { deadline ->
                            val now = LocalDateTime.now()
                            val isUrgent = deadline.isBefore(now.plusDays(3))
                            InfoRow(
                                icon = Icons.Default.CalendarToday,
                                label = stringResource(R.string.event_detail_registration_deadline),
                                value = formatFullDate(
                                    deadline,
                                    false
                                ),
                                iconTint = if (isUrgent) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.tertiary,
                                valueColor = if (isUrgent) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        event.location?.let { location ->
                            InfoRow(
                                icon = Icons.Default.LocationOn,
                                label = stringResource(R.string.event_detail_location),
                                value = location,
                                iconTint = ElectricPink
                            )
                        }

                        event.organizer?.let { organizer ->
                            InfoRow(
                                icon = Icons.Default.Person,
                                label = stringResource(R.string.event_detail_organizer),
                                value = organizer,
                                iconTint = ElectricCyan
                            )
                        }

                        event.prizeFund?.let { prizeFund ->
                            InfoRow(
                                icon = Icons.Default.EmojiEvents,
                                label = stringResource(R.string.event_detail_prize_fund),
                                value = prizeFund,
                                iconTint = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        event.maxParticipants?.let { maxParticipants ->
                            InfoRow(
                                icon = null,
                                label = stringResource(R.string.event_detail_max_participants),
                                value = stringResource(
                                    R.string.event_detail_max_participants_value_format,
                                    maxParticipants
                                ),
                                iconTint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Tags
                    if (event.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = stringResource(R.string.event_detail_tags),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        TagsRow(tags = event.tags)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description
                    Text(
                        text = stringResource(R.string.event_detail_description),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinkText(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = TextUnit.Unspecified),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Gradient Buttons Box
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (event.dateTime != null) {
                    // Calendar Button (Solid Surface Variant)
                    val buttonColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    val contentColor = MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(buttonColor)
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAddToCalendar()
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = contentColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.event_detail_add_to_calendar),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = MaterialTheme.typography.labelLarge.lineHeight * 0.95
                            )
                        }
                    }
                }

                // Open Button (Gradient)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialPink
                                )
                            )
                        )
                        .clickable {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenUrl()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.event_detail_open),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun InfoRow(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    label: String,
    value: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

internal fun formatFullDate(
    dateTime: LocalDateTime,
    hasSpecificTime: Boolean = true
): String {
    val formatter = if (!hasSpecificTime) {
        DateTimeFormatter.ofPattern(
            "d MMMM yyyy",
            Locale.forLanguageTag("ru")
        )
    } else {
        DateTimeFormatter.ofPattern(
            "d MMMM yyyy, HH:mm",
            Locale.forLanguageTag("ru")
        )
    }
    return dateTime.format(formatter)
}
