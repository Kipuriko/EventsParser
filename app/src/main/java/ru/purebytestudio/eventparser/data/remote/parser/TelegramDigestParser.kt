package ru.purebytestudio.eventparser.data.remote.parser

import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSource
import ru.purebytestudio.eventparser.domain.model.EventType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Парсинг "дайджестов"/"афиш", где один пост содержит несколько событий.
 *
 * Важно: возвращает null, если пост не похож на дайджест.
 */
internal class TelegramDigestParser(
    private val dateExtractor: TelegramDateExtractor,
    private val titleExtractor: TelegramTitleExtractor,
    private val attributesExtractor: TelegramEventAttributesExtractor
) {
    fun parseDigestItems(
        fullText: String,
        fallbackUrl: String,
        channelName: String,
        organizer: String,
        fallbackCategory: EventCategory,
        imageUrl: String?
    ): List<Event>? {
        val lower = fullText.lowercase()
        val looksLikeDigest =
            lower.contains("дайджест") || lower.contains("#дайджест") || lower.contains("афиша")
        if (!looksLikeDigest) return null

        val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 4) return emptyList()

        val events = mutableListOf<Event>()

        for (i in lines.indices) {
            val dateMatch = TelegramParsingRegex.numericDateRegex.find(lines[i]) ?: continue
            val titleLine = lines.getOrNull(i - 1) ?: continue

            val title =
                titleExtractor.pickNamePart(titleExtractor.cleanTitleLine(titleLine))
                    .takeIf { it.length >= 4 } ?: continue

            val day = dateMatch.groupValues[1].toIntOrNull() ?: continue
            val month = dateMatch.groupValues[2].toIntOrNull() ?: continue
            val year = dateExtractor.parseYearFlexible(dateMatch.groupValues[3]) ?: continue
            val date = dateExtractor.safeDate(
                year,
                month,
                day
            ) ?: continue

            val location = Regex("""\[(.+)]""").find(lines[i])?.groupValues?.getOrNull(1)?.trim()
            val url = extractFirstRealUrl(titleLine) ?: fallbackUrl

            val detectedType = EventType.fromText(title)
            val detectedCategory = EventCategory.fromText(title)
            val finalCategory =
                if (detectedCategory != EventCategory.OTHER) detectedCategory else fallbackCategory

            events.add(
                Event(
                    id = "tg_${channelName}_${
                        extractStableSuffix(
                            date,
                            title
                        )
                    }_${events.size}",
                    title = title,
                    description = fullText,
                    imageUrl = imageUrl,
                    dateTime = LocalDateTime.of(
                        date,
                        LocalTime.NOON
                    ),
                    location = location,
                    isOnline = false,
                    format = if (location == null) null else "Офлайн",
                    url = url,
                    source = EventSource.TELEGRAM,
                    category = finalCategory,
                    eventType = detectedType,
                    organizer = organizer,
                    tags = attributesExtractor.extractTags(
                        fullText,
                        detectedType,
                        finalCategory,
                        null
                    ),
                    hasSpecificTime = false
                )
            )
        }

        return events
    }

    private fun extractFirstRealUrl(line: String): String? {
        val raw = TelegramParsingRegex.urlRegex.find(line)?.value ?: return null
        val cleaned = raw.replace(
            "&amp;",
            "&"
        )
        // отсекаем telegram search links вида t.me/s/<channel>?q=%23...
        if (cleaned.contains(
                "t.me/s/",
                ignoreCase = true
            ) && cleaned.contains(
                "?q=%23",
                ignoreCase = true
            )
        ) return null
        return cleaned
    }

    private fun extractStableSuffix(
        date: LocalDate,
        title: String
    ): String {
        val compactTitle = title.lowercase()
            .replace(
                TelegramParsingRegex.emojiLikeRegex,
                ""
            )
            .replace(
                Regex("""[^\p{L}\p{N}\s]"""),
                ""
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
            .take(30)
        return "${date}_$compactTitle"
    }
}
