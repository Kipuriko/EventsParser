package ru.purebytestudio.eventparser.data.remote.parser

import org.jsoup.nodes.Element
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSource
import ru.purebytestudio.eventparser.domain.model.EventType
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Пайплайн парсинга 1 Telegram-сообщения в список доменных [Event].
 *
 * Цели:
 * - сохранить исходное форматирование текста (переносы строк, абзацы, ссылки)
 * - корректно извлечь дату/время с учетом отсутствующего года (ориентируясь на дату публикации)
 * - заполнить поля, которые используются UI: category/eventType/tags/location/organizer/price/prizeFund/format/hasSpecificTime
 * - поддержать "дайджесты" (один пост -> несколько событий)
 */
internal class TelegramMessagePipeline(
    private val dateExtractor: TelegramDateExtractor,
    private val htmlTextExtractor: TelegramMessageHtmlTextExtractor,
    private val textSanitizer: TelegramTextSanitizer,
    private val titleExtractor: TelegramTitleExtractor,
    private val heuristics: TelegramEventHeuristics,
    private val attributesExtractor: TelegramEventAttributesExtractor,
    private val imageExtractor: TelegramImageExtractor,
    private val digestParser: TelegramDigestParser
) {

    fun parse(
        message: Element,
        channelName: String,
        category: EventCategory
    ): List<Event> {
        val messageId = extractMessageId(message) ?: return emptyList()
        val organizer = "@$channelName"

        val publishedAt = extractPublishedAt(message) ?: LocalDateTime.now()
        val messageUrl = "https://t.me/$channelName/$messageId"
        val imageUrl = imageExtractor.extractImageUrl(message)

        val textElement = message.selectFirst(".tgme_widget_message_text") ?: return emptyList()
        val description =
            textSanitizer.sanitizeTelegramArtifacts(
                htmlTextExtractor.toTextPreservingFormatting(textElement)
            ).trim()
        if (description.isBlank()) return emptyList()

        // 1) Спец-кейс: дайджесты/афиши (в одном сообщении несколько событий)
        digestParser.parseDigestItems(
            fullText = description,
            fallbackUrl = messageUrl,
            channelName = channelName,
            organizer = organizer,
            fallbackCategory = category,
            imageUrl = imageUrl
        )?.let { events ->
            if (events.isNotEmpty()) return events
        }

        // 2) Обычный пост: 1 событие
        // 0) Фильтр: отсеиваем рекламные/промо-посты и “акции”, даже если там есть дата
        if (!heuristics.looksLikeEvent(description)) return emptyList()

        val dateInfo = extractDateInfo(
            text = description,
            publishedAt = publishedAt
        ) ?: return emptyList()

        if (!heuristics.isInAllowedWindow(dateInfo.start)) {
            Timber.d("Skip: date out of window: ${dateInfo.start} ($organizer)")
            return emptyList()
        }

        val title = titleExtractor.extractTitle(description)
        if (title.isBlank()) return emptyList()

        val detectedType = EventType.fromText("$title\n$description")
        val detectedCategory = EventCategory.fromText("$title\n$description")
        val finalCategory =
            if (detectedCategory != EventCategory.OTHER) detectedCategory else category

        val location = attributesExtractor.extractLocation(description)
        val format = attributesExtractor.extractFormat(
            description,
            location
        )
        val isOnline = attributesExtractor.isOnline(
            format,
            description,
            location
        )
        val price = attributesExtractor.extractPrice(description)
        val isFree = attributesExtractor.isFree(
            price,
            description
        )
        val prizeFund = attributesExtractor.extractPrizeFund(description)
        val tags = attributesExtractor.extractTags(
            description,
            detectedType,
            finalCategory,
            format
        )

        return listOf(
            Event(
                id = "tg_${channelName}_$messageId",
                title = title,
                description = description,
                imageUrl = imageUrl ?: imageExtractor.extractInlineImageUrl(textElement),
                dateTime = dateInfo.start,
                endDateTime = dateInfo.end,
                location = location,
                isOnline = isOnline,
                format = format,
                url = messageUrl,
                source = EventSource.TELEGRAM,
                category = finalCategory,
                eventType = detectedType,
                organizer = organizer,
                price = price,
                isFree = isFree,
                prizeFund = prizeFund,
                tags = tags,
                hasSpecificTime = dateInfo.hasSpecificTime
            )
        )
    }

    private data class DateInfo(
        val start: LocalDateTime,
        val end: LocalDateTime? = null,
        val hasSpecificTime: Boolean
    )

    private fun extractMessageId(message: Element): String? =
        message.attr("data-post").split("/").lastOrNull()?.takeIf { it.isNotBlank() }

    private fun extractPublishedAt(message: Element): LocalDateTime? {
        val timeElement = message.selectFirst(".tgme_widget_message_date time") ?: return null
        val datetime = timeElement.attr("datetime") // e.g. "2025-12-27T12:00:00+00:00"
        return try {
            LocalDateTime.parse(
                datetime,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            )
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun extractDateInfo(
        text: String,
        publishedAt: LocalDateTime
    ): DateInfo? =
        dateExtractor.extractDateInfo(
            text,
            publishedAt
        )
            ?.let {
                DateInfo(
                    start = it.start,
                    end = it.end,
                    hasSpecificTime = it.hasSpecificTime
                )
            }
}