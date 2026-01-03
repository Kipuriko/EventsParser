package ru.purebytestudio.eventparser.data.remote.parser

import org.jsoup.nodes.Element

internal class TelegramImageExtractor {
    fun extractImageUrl(message: Element): String? {
        val styleUrlRegex = Regex("""background-image:url\('(.+?)'\)""")

        val candidates = listOfNotNull(
            message.selectFirst(".tgme_widget_message_photo_wrap"),
            message.selectFirst(".tgme_widget_message_link_preview_image"),
            message.selectFirst(".link_preview_image"),
            message.selectFirst(".tgme_widget_message_document_thumb"),
            message.selectFirst(".tgme_widget_message_video_thumb")
        )

        for (el in candidates) {
            val style = el.attr("style")
            val match = styleUrlRegex.find(style) ?: continue
            return match.groupValues[1]
        }

        // Запасной вариант: иногда фото приходит как ссылка на внешний хостинг прямо в тексте.
        val inline =
            message.selectFirst(".tgme_widget_message_text")?.let { extractInlineImageUrl(it) }
        return inline
    }

    fun extractInlineImageUrl(textElement: Element): String? {
        val href = textElement.select("a[href]")
            .mapNotNull { it.attr("href").trim() }
            .firstOrNull { TelegramParsingRegex.directImageUrlRegex.matches(it) }
        return href
    }
}