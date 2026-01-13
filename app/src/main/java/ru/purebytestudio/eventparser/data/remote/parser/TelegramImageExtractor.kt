package ru.purebytestudio.eventparser.data.remote.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import timber.log.Timber

internal class TelegramImageExtractor {
    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        private const val TIMEOUT_MS = 10000

        // Домены, исключенные из Open Graph парсинга
        private val EXCLUDED_DOMAINS = setOf(
            "youtube.com",
            "youtu.be",
            "instagram.com",
            "twitter.com",
            "x.com",
            "facebook.com",
            "vk.com",
            "linkedin.com"
        )

        // Паттерны для контентных изображений
        private val CONTENT_IMAGE_PATHS = setOf(
            "/upload/",
            "/images/",
            "/img/"
        )
        private val EXCLUDED_IMAGE_KEYWORDS = setOf(
            "logo",
            "icon"
        )
    }

    fun extractImageUrl(message: Element): String? {
        // 1. Попытка извлечь из Telegram message (прямые изображения, превью)
        extractFromTelegramMessage(message)?.let { return it }

        // 2. Попытка извлечь inline изображения из текста
        message.selectFirst(".tgme_widget_message_text")
            ?.let { extractInlineImageUrl(it) }
            ?.let { return it }

        // 3. Попытка извлечь из внешней ссылки (Open Graph)
        extractFirstExternalUrl(message)
            ?.let { extractOpenGraphImage(it) }
            ?.let { return it }

        return null
    }

    /**
     * Извлекает изображение из элементов Telegram сообщения.
     */
    private fun extractFromTelegramMessage(message: Element): String? {
        val styleUrlRegex = Regex("""background-image:url\('(.+?)'\)""")
        val dataBackgroundRegex = Regex("""url\('(.+?)'\)""")

        val candidates = listOfNotNull(
            message.selectFirst(".tgme_widget_message_photo_wrap"),
            message.selectFirst(".tgme_widget_message_link_preview_image"),
            message.selectFirst(".link_preview_image"),
            message.selectFirst(".tgme_widget_message_link_preview"),
            message.selectFirst(".tgme_widget_message_document_thumb"),
            message.selectFirst(".tgme_widget_message_video_thumb"),
            message.selectFirst("a.tgme_widget_message_link_preview i"),
            message.selectFirst(".message_media_view_wrap")
        )

        for (el in candidates) {
            // Проверяем style атрибут
            val style = el.attr("style")
            if (style.isNotEmpty()) {
                styleUrlRegex.find(style)?.groupValues?.get(1)?.let { return it }
            }

            // Проверяем data-background атрибут
            val dataBackground = el.attr("data-background")
            if (dataBackground.isNotEmpty()) {
                dataBackgroundRegex.find(dataBackground)?.groupValues?.get(1)?.let { return it }
            }
        }

        return null
    }

    fun extractInlineImageUrl(textElement: Element): String? {
        val href = textElement.select("a[href]")
            .mapNotNull { it.attr("href").trim() }
            .firstOrNull { TelegramParsingRegex.directImageUrlRegex.matches(it) }
        return href
    }

    /**
     * Извлекает первую внешнюю ссылку (не t.me) из сообщения.
     */
    private fun extractFirstExternalUrl(message: Element): String? {
        val textElement = message.selectFirst(".tgme_widget_message_text") ?: return null

        return textElement.select("a[href]")
            .mapNotNull { it.attr("href").trim() }
            .firstOrNull { url ->
                url.startsWith("http") &&
                        !url.contains("t.me") &&
                        !url.contains("telegram.org") &&
                        !TelegramParsingRegex.directImageUrlRegex.matches(url) &&
                        EXCLUDED_DOMAINS.none { domain ->
                            url.contains(
                                domain,
                                ignoreCase = true
                            )
                        }
            }
            ?.let { normalizeUrl(it) }
    }

    /**
     * Нормализует URL: заменяет HTTP на HTTPS.
     */
    private fun normalizeUrl(url: String): String =
        if (url.startsWith(
                "http://",
                ignoreCase = true
            )
        ) {
            "https://" + url.substring(7)
        } else {
            url
        }

    /**
     * Извлекает изображение из внешней ссылки (Open Graph, Twitter Card, контент).
     * Делает HTTP-запрос, поэтому может быть медленным.
     */
    private fun extractOpenGraphImage(url: String): String? {
        return try {
            val doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .get()

            // Пробуем различные источники изображений
            extractFromMetaTags(
                doc,
                url
            )
                ?: extractFromContentImages(doc)
                ?: null.also { Timber.d("Open Graph изображение не найдено для: $url") }

        } catch (e: java.net.SocketTimeoutException) {
            Timber.w("Timeout при извлечении изображения из: $url")
            null
        } catch (e: java.io.IOException) {
            if (e.message?.contains("Cleartext HTTP") == true) {
                Timber.w("Cleartext HTTP не поддерживается: $url")
            }
            null
        } catch (e: Exception) {
            Timber.w(
                e,
                "Ошибка при извлечении изображения из: $url"
            )
            null
        }
    }

    /**
     * Извлекает изображение из meta-тегов (Open Graph, Twitter Card).
     */
    private fun extractFromMetaTags(
        doc: Element,
        baseUrl: String
    ): String? {
        // Open Graph
        doc.select(
            "meta[property=og:image], meta[property='og:image'], " +
                    "meta[name=og:image], meta[name='og:image']"
        ).attr("content").takeIf { it.isNotEmpty() }?.let { ogImage ->
            return normalizeImageUrl(
                ogImage,
                baseUrl
            )
        }

        // Twitter Card
        doc.select(
            "meta[name=twitter:image], meta[name='twitter:image'], " +
                    "meta[property=twitter:image], meta[property='twitter:image']"
        ).attr("content").takeIf { it.isNotEmpty() }?.let { return it }

        // Link rel="image_src"
        doc.select("link[rel=image_src], link[rel='image_src']")
            .attr("href").takeIf { it.isNotEmpty() }?.let { return it }

        return null
    }

    /**
     * Извлекает первое подходящее изображение из контента страницы.
     */
    private fun extractFromContentImages(doc: Element): String? {
        return doc.select("img[src]").firstNotNullOfOrNull { img ->
            val src = img.attr("abs:src")
            if (src.isNotEmpty() &&
                EXCLUDED_IMAGE_KEYWORDS.none {
                    src.contains(
                        it,
                        ignoreCase = true
                    )
                } &&
                CONTENT_IMAGE_PATHS.any {
                    src.contains(
                        it,
                        ignoreCase = true
                    )
                }
            ) {
                src
            } else {
                null
            }
        }
    }

    /**
     * Нормализует URL изображения (преобразует относительные пути в абсолютные).
     */
    private fun normalizeImageUrl(
        imageUrl: String,
        baseUrl: String
    ): String =
        if (imageUrl.startsWith("/")) {
            baseUrl.split("/").take(3).joinToString("/") + imageUrl
        } else {
            imageUrl
        }
}