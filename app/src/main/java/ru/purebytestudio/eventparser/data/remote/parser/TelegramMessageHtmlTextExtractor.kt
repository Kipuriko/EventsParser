package ru.purebytestudio.eventparser.data.remote.parser

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Конвертация HTML Telegram-сообщения в текст с сохранением:
 * - переносов строк
 * - абзацев
 * - ссылок (если в тексте ссылки не видно, добавляем "(url)")
 *
 * Важно: логика достаточно хрупкая — любые изменения лучше фиксировать тестами.
 */
internal class TelegramMessageHtmlTextExtractor {
    fun toTextPreservingFormatting(root: Element): String {
        val sb = StringBuilder()

        fun appendNewline() {
            if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
        }

        fun appendParagraphBreak() {
            // 1 пустая строка между абзацами
            if (sb.isNotEmpty()) {
                val needsBreak = when (sb.length) {
                    0 -> false
                    1 -> sb[sb.length - 1] != '\n'
                    else -> !(sb[sb.length - 1] == '\n' && sb[sb.length - 2] == '\n')
                }
                if (needsBreak) {
                    appendNewline()
                    appendNewline()
                }
            }
        }

        fun walk(node: Node) {
            when (node) {
                is TextNode -> sb.append(node.text())
                is Element -> {
                    when (node.tagName().lowercase()) {
                        "br" -> appendNewline()
                        "p" -> {
                            appendParagraphBreak()
                            node.childNodes().forEach(::walk)
                            appendParagraphBreak()
                        }

                        "a" -> {
                            val href = node.attr("href").trim()
                            val label = node.text()
                            // Telegram часто оборачивает хэштеги в ссылки вида "/s/<channel>?q=%23tag" или "?q=%23tag".
                            // Такие href в текст **не добавляем**, иначе в описании/заголовке появятся артефакты "(?q=...)".
                            val isTelegramQueryLink =
                                href.startsWith("?") ||
                                        href.startsWith("/") ||
                                        href.contains(
                                            "?q=%23",
                                            ignoreCase = true
                                        )

                            // Если ссылка ведёт напрямую на картинку и текст ссылки пустой — не добавляем её в описание.
                            // Картинка будет извлечена в imageUrl через extractInlineImageUrl()/extractImageUrl().
                            val isDirectImage =
                                TelegramParsingRegex.directImageUrlRegex.matches(href)

                            if (href.isBlank() || isTelegramQueryLink || (label.isBlank() && isDirectImage)) {
                                sb.append(label)
                            } else if (label.contains(
                                    href,
                                    ignoreCase = true
                                )
                            ) {
                                sb.append(label)
                            } else {
                                // Для “настоящих” ссылок сохраняем связь label->href прямо в тексте,
                                // чтобы UI мог отрисовать кликабельный label без “хвоста” URL.
                                val isRealLink =
                                    href.startsWith(
                                        "http",
                                        ignoreCase = true
                                    ) ||
                                            href.startsWith(
                                                "t.me",
                                                ignoreCase = true
                                            )

                                when {
                                    label.isBlank() && isRealLink -> sb.append(href)
                                    label.isNotBlank() && isRealLink -> sb.append("[$label]($href)")
                                    else -> sb.append(label)
                                }
                            }
                        }

                        else -> node.childNodes().forEach(::walk)
                    }
                }

                else -> {}
            }
        }

        root.childNodes().forEach(::walk)

        // Нормализация неразрывных пробелов и хвостовых пробелов в конце строк
        return sb.toString()
            .replace(
                '\u00A0',
                ' '
            )
            .replace(
                "&amp;",
                "&"
            )
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .trim()
    }
}
