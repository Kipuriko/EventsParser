package ru.purebytestudio.eventparser.data.remote.parser

import ru.purebytestudio.eventparser.data.util.TagSanitizer
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventType

/**
 * Извлекает дополнительные поля события из текста поста: location/format/price/tags/etc.
 *
 * Важно: набор эвристик зависит от реальных формулировок постов.
 */
internal class TelegramEventAttributesExtractor {
    fun extractLocation(text: String): String? {
        val lines = text.lines()

        val patterns = listOf(
            Regex("""^\s*📍\s*(.+)$"""),
            Regex(
                """^\s*(?:🏞\s*)?(?:где|место|локация)\s*:\s*(.+)$""",
                RegexOption.IGNORE_CASE
            )
        )

        for (line in lines) {
            val trimmed = line.trim()
            for (p in patterns) {
                val m = p.find(trimmed) ?: continue
                val value = m.groupValues[1].trim()
                    .removeSuffix(".")
                    .trim()
                if (value.isNotBlank()) return value.take(140)
            }
        }

        // Упрощённый запасной вариант: если явно видим «онлайн», но локацию не нашли.
        if (text.contains(
                "онлайн",
                ignoreCase = true
            ) || text.contains(
                "online",
                ignoreCase = true
            )
        ) {
            return "Онлайн"
        }

        return null
    }

    fun extractFormat(
        text: String,
        location: String?
    ): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("гибрид") -> "Гибрид"
            lower.contains("стрим") || lower.contains("трансляц") || lower.contains("youtube") ->
                "Онлайн (стрим)"

            lower.contains("онлайн") || lower.contains("webinar") || lower.contains("zoom") ||
                    (location?.equals(
                        "Онлайн",
                        ignoreCase = true
                    ) == true) ->
                "Онлайн"

            lower.contains("офлайн") -> "Офлайн"
            else -> null
        }
    }

    fun isOnline(
        format: String?,
        text: String,
        location: String?
    ): Boolean {
        if (format?.contains(
                "Онлайн",
                ignoreCase = true
            ) == true
        ) return true
        if (location?.equals(
                "Онлайн",
                ignoreCase = true
            ) == true
        ) return true
        val lower = text.lowercase()
        return lower.contains("онлайн") || lower.contains("webinar") || lower.contains("zoom")
    }

    fun extractPrice(text: String): String? {
        val lines = text.lines()
        if (lines.any {
                it.trim().equals(
                    "Бесплатно",
                    ignoreCase = true
                )
            }) return "Бесплатно"
        if (text.contains(
                "бесплатно",
                ignoreCase = true
            )
        ) return "Бесплатно"

        val explicit = Regex("""(?i)\b(стоимость|цена|билет|вход)\s*:\s*(.+)$""")
        for (line in lines) {
            val m = explicit.find(line.trim()) ?: continue
            val value = m.groupValues[2].trim()
            if (value.isNotBlank()) return value.take(60)
        }

        return null
    }

    fun isFree(
        price: String?,
        text: String
    ): Boolean {
        if (price == null) return false
        return price.contains(
            "бесплатно",
            ignoreCase = true
        ) || text.contains(
            "бесплатно",
            ignoreCase = true
        )
    }

    fun extractPrizeFund(text: String): String? {
        val patterns = listOf(
            Regex("""(?i)\bпризов(?:ой|ого)\s+фонд\b\D*(до\s*)?(\d[\d\s]*\s*(?:₽|руб(?:\.|лей)?|rubl?e?s?))"""),
            Regex("""(?i)\bпризов(?:ой|ого)\s+фонд\b\D*(до\s*)?(\d+\s*млн\s*(?:₽|руб(?:\.|лей)?))"""),
            Regex("""(?i)\bгрант[аы]?\b\D*(до\s*)?(\d[\d\s]*\s*(?:млн\s*)?(?:₽|руб(?:\.|лей)?))""")
        )
        for (p in patterns) {
            val m = p.find(text) ?: continue
            val value = m.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() } ?: continue
            return value.replace(
                Regex("""\s+"""),
                " "
            ).trim().take(60)
        }
        return null
    }

    fun extractTags(
        text: String,
        eventType: EventType,
        category: EventCategory,
        format: String?
    ): List<String> {
        val tags = linkedSetOf<String>()

        // Хэштеги из текста
        val hashtagRegex = Regex("""#([\p{L}\p{N}_]+)""")
        hashtagRegex.findAll(text).forEach { m ->
            val t = m.groupValues[1].trim()
            if (t.length >= 2) tags.add(t)
        }

        // Производные теги
        tags.add(category.displayName)
        if (eventType != EventType.OTHER) tags.add(eventType.displayName)
        format?.let { tags.add(it) }

        return TagSanitizer.sanitize(
            tags.toList(),
            limit = 12
        )
    }
}

