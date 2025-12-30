package ru.purebytestudio.eventparser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.purebytestudio.eventparser.domain.model.EventCategory

/**
 * UI-компонент чипа для выбора категории событий.
 */
@Composable
fun CategoryChip(
    category: EventCategory?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = category?.displayName ?: "Все"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(),
        label = "chip_scale"
    )

    val categoryColor = category?.let { getCategoryColor(it) } ?: MaterialTheme.colorScheme.primary

    val gradientBrush = remember(categoryColor) {
        Brush.linearGradient(
            colors = listOf(
                categoryColor,
                categoryColor.copy(alpha = 0.7f),
                categoryColor
            )
        )
    }

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else categoryColor,
        label = "chip_content"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else categoryColor.copy(alpha = 0.5f),
        label = "chip_border"
    )

    val contentDesc = if (isSelected) {
        "Выбрана категория: $label"
    } else {
        "Категория: $label. Нажмите для выбора"
    }

    val backgroundModifier = if (isSelected) {
        Modifier.background(gradientBrush)
    } else {
        Modifier.background(Color.Transparent)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .then(backgroundModifier)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDesc
            }
            .padding(
                horizontal = 20.dp,
                vertical = 8.dp
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}