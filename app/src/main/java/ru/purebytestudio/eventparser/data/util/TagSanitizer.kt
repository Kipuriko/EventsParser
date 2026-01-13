package ru.purebytestudio.eventparser.data.util

/**
 * Санитайзер тегов, приходящих из внешних источников (например, Telegram).
 *
 * Цель: вычистить "шум" (пустые, чисто числовые, слишком короткие и т.п.),
 * чтобы UI и дальнейшая логика работали с осмысленными тегами.
 */
internal object TagSanitizer {
    fun sanitize(
        raw: List<String>,
        limit: Int = 12
    ): List<String> {
        if (raw.isEmpty()) return emptyList()

        val out = linkedSetOf<String>()
        for (t in raw) {
            val cleaned = t.trim().trim('#')
            if (!isMeaningful(cleaned)) continue
            out.add(cleaned)
            if (out.size >= limit) break
        }
        return out.toList()
    }

    private fun isMeaningful(tag: String): Boolean {
        if (tag.length < 2) return false

        // Отсекаем чисто числовые "теги" вроде "11", "15" (часто это служебные метки).
        if (tag.all { it.isDigit() }) return false

        // Тег должен содержать хотя бы одну букву (кириллица/латиница и т.п.).
        if (tag.none { it.isLetter() }) return false

        return true
    }
}