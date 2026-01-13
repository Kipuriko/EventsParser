package ru.purebytestudio.eventparser.data.remote.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import ru.purebytestudio.eventparser.data.remote.parser.TelegramParser.Companion.PARSE_LIMIT
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSource
import ru.purebytestudio.eventparser.domain.util.EventIdentityNormalizer
import timber.log.Timber

/**
 * Парсер публичных Telegram каналов через веб-превью t.me.
 * Ограничение: парсит только последние 10 сообщений.
 *
 * Как работает:
 * - для выбранных категорий берёт список каналов из [TelegramChannelProvider];
 * - для каждого канала скачивает web-страницу `https://t.me/s/<channel>`;
 * - берёт последние [PARSE_LIMIT] сообщений и прогоняет каждое через [TelegramMessagePipeline];
 * - на выходе удаляет «дубли» по нормализованному ключу ([EventIdentityNormalizer]).
 */
internal class TelegramParser(
    private val channelProvider: TelegramChannelProvider,
    private val messagePipeline: TelegramMessagePipeline
) : EventParser {
    override val source: EventSource = EventSource.TELEGRAM

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        private const val TIMEOUT_MS = 15000
        private const val TELEGRAM_URL_FORMAT = "https://t.me/s/%s"
        private const val MESSAGE_SELECTOR = ".tgme_widget_message"
        private const val PARSE_LIMIT = 30  // Увеличено с 10 до 30 для большего охвата событий
    }

    override suspend fun parseEvents(categories: List<EventCategory>): Result<List<Event>> =
        withContext(Dispatchers.IO) {
            try {
                // Определяем, какие каналы нужно парсить под выбранные категории.
                val channels = channelProvider.channelsFor(categories)

                // Парсим каналы параллельно, чтобы не «упираться» в сеть по одному каналу.
                val allEvents = channels.map { channel ->
                    async {
                        runCatching { parseChannel(channel) }
                            .onFailure {
                                Timber.e(
                                    t = it,
                                    message = "Failed to parse channel: ${channel.name}"
                                )
                            }
                            .getOrElse { emptyList() }
                    }
                }.awaitAll().flatten()

                val uniqueEvents = deduplicateEvents(allEvents)

                Timber.d("Total parsed events: ${uniqueEvents.size}")
                Result.success(uniqueEvents)
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Global parsing error"
                )
                Result.failure(e)
            }
        }

    private fun deduplicateEvents(events: List<Event>): List<Event> {
        // Группируем по нормализованному ключу и выбираем «лучший» дубль
        // (например, с картинкой или с более длинным описанием).
        return events
            .groupBy { EventIdentityNormalizer.fromEvent(it) }
            .values
            .map { duplicates ->
                // Предпочитаем вариант с картинкой, затем — с более длинным описанием.
                duplicates.maxByOrNull(::dedupScore) ?: duplicates.first()
            }
    }

    private fun dedupScore(event: Event): Int =
        (if (event.imageUrl != null) 1000 else 0) + event.description.length

    private fun parseChannel(channel: TelegramChannel): List<Event> {
        val url = String.format(
            TELEGRAM_URL_FORMAT,
            channel.name
        )
        Timber.d("Connecting to $url")

        val doc = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .get()

        val messages = doc.select(MESSAGE_SELECTOR)

        // Берём последние PARSE_LIMIT сообщений
        val recentMessages = messages.takeLast(PARSE_LIMIT)
        Timber.d("Канал ${channel.name}: парсим ${recentMessages.size}/${messages.size} сообщений")

        val events = recentMessages.mapNotNull { message ->
            runCatching {
                messagePipeline.parse(
                    message,
                    channel.name,
                    channel.category
                )
            }.onFailure {
                Timber.w(
                    it,
                    "Ошибка при парсинге сообщения в ${channel.name}"
                )
            }.getOrNull()
        }.flatten()

        if (events.isNotEmpty()) {
            Timber.d("Канал ${channel.name}: спарсено ${events.size} событий")
        }

        return events
    }
}