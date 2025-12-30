package ru.purebytestudio.eventparser.data.remote.parser

import ru.purebytestudio.eventparser.domain.model.EventCategory

internal data class TelegramChannel(
    val name: String,
    val category: EventCategory
)

/**
 * Предоставляет маппинг между категориями событий и Telegram каналами.
 */
internal class TelegramChannelProvider {
    // Список каналов для парсинга
    private val eventChannels = mapOf(
        EventCategory.ANDROID_DEV to listOf(
            "android_broadcast",
            "android_events"
        ),
        EventCategory.DEVOPS to listOf("devops_events"),
        EventCategory.GAME_DEV to listOf(
            "gamedev_events",
            "progamedev",
            "myindieru", // myindie.ru mentioned in text often implies channel myindieru or similar
            "gamedevafisha"
        ),
        EventCategory.OTHER to listOf(
            "itevents",     // t.me/itevents
            "iteventsrus",  // t.me/iteventsrus
            "ITMeeting",    // t.me/ITMeeting
            "js_events",    // Example inferred, keeping generic lists
            "qa_events"
        )
    )

    fun channelsFor(categories: List<EventCategory>): List<TelegramChannel> {
        val selectedCategories =
            if (categories.isEmpty() || categories.contains(EventCategory.OTHER)) {
                EventCategory.entries
            } else {
                categories
            }

        return selectedCategories.flatMap { category ->
            val channelNames = eventChannels[category].orEmpty()
            channelNames.map {
                TelegramChannel(
                    name = it,
                    category = category
                )
            }
        }.distinctBy { it.name }
    }
}