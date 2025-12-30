package ru.purebytestudio.eventparser.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

import androidx.compose.ui.unit.sp

@Composable
fun LinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary
) {
    val uriHandler = LocalUriHandler.current
    var layoutResult: TextLayoutResult? by remember { mutableStateOf(null) }

    val normalizedText = remember(text) {
        text.replace(
            Regex("""(\b[\p{L}\p{N}][\p{L}\p{N}_-]{0,24})\s+((?:https?://|t\.me/)[^\s)\]]+)""")
        ) { m ->
            val label = m.groupValues[1]
            val url = m.groupValues[2]
            "[$label]($url)"
        }
    }

    val annotatedString = buildAnnotatedString {
        // Ищем markdown-ссылки вида [текст](url) и «сырые» URL (http..., t.me/...).
        // Проходим по строке и добавляем фрагменты либо как обычный текст, либо как кликабельные ссылки.

        var currentIndex = 0
        // Матчим либо markdown-ссылку [текст](url), либо «сырой» URL.
        val regex = Regex("""\[([^]]+)]\(([^)]+)\)|((?:https?://|t\.me/)[^\s)\]]+)""")

        regex.findAll(normalizedText).forEach { match ->
            if (match.range.first > currentIndex) {
                append(
                    normalizedText.substring(
                        currentIndex,
                        match.range.first
                    )
                )
            }

            val linkText = match.groups[1]?.value // текст в markdown-ссылке
            val linkUrl = match.groups[2]?.value  // url в markdown-ссылке
            val rawUrl = match.groups[3]?.value   // «сырой» url

            val displayPart = linkText ?: rawUrl ?: ""
            val urlPart = linkUrl ?: rawUrl ?: ""

            val start = length
            append(displayPart)
            val end = length

            addStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                ),
                start = start,
                end = end
            )

            addStringAnnotation(
                tag = "URL",
                annotation = urlPart,
                start = start,
                end = end
            )

            currentIndex = match.range.last + 1
        }

        if (currentIndex < normalizedText.length) {
            append(normalizedText.substring(currentIndex))
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier.pointerInput(annotatedString) {
            detectTapGestures { position ->
                val result = layoutResult ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(position)
                annotatedString.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    try {
                        val url = annotation.item
                        uriHandler.openUri(
                            if (url.startsWith(
                                    prefix = "t.me",
                                    ignoreCase = true
                                )
                            ) "https://$url"
                            else url
                        )
                    } catch (_: Exception) {
                        // ignore
                    }
                }
            }
        },
        color = color,
        style = style.copy(lineHeight = 24.sp),
        onTextLayout = { layoutResult = it }
    )
}