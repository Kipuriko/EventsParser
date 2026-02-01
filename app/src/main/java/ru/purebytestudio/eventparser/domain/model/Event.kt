package ru.purebytestudio.eventparser.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * Доменная модель, представляющая событие/митап.
 *
 * @property id Уникальный идентификатор события
 * @property title Заголовок события
 * @property description Подробное описание события
 * @property imageUrl URL изображения/баннера события
 * @property dateTime Дата и время начала события
 * @property endDateTime Дата и время окончания события
 * @property location Физическое местоположение или "Online"
 * @property isOnline True, если событие только онлайн
 * @property url Оригинальный URL источника события
 * @property source Платформа-источник, откуда было спарсено событие
 * @property category Категория события (например, Android, iOS и т.д.)
 * @property eventType Тип события (например, Митап, Конференция и т.д.)
 * @property organizer Имя организатора или сообщества
 * @property price Описание цены (например, "Бесплатно", "1000 RUB")
 * @property isFree True, если событие бесплатное (вычисляется на основе цены)
 * @property tags Список тегов, связанных с событием
 * @property isFavorite True, если добавлено в избранное пользователем
 * @property registrationDeadline Крайний срок регистрации
 * @property maxParticipants Максимальное количество участников
 */
@Serializable
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val dateTime: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endDateTime: LocalDateTime? = null,
    val location: String? = null,
    val isOnline: Boolean = false,
    /**
     * Формат проведения (например, "Онлайн", "Офлайн", "Гибрид", "Онлайн (стрим)").
     * UI может показывать это как отдельную строку в деталях.
     */
    val format: String? = null,
    val url: String,
    val source: EventSource = EventSource.UNKNOWN,
    val category: EventCategory = EventCategory.OTHER,
    val eventType: EventType = EventType.OTHER,
    val organizer: String? = null,
    val price: String? = null,
    val isFree: Boolean = false,
    /**
     * Призовой фонд / гранты (если извлекается из текста).
     */
    val prizeFund: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrationDeadline: LocalDateTime? = null,
    val maxParticipants: Int? = null,
    val hasSpecificTime: Boolean = false,
    val localImagePath: String? = null
)

/**
 * Тип мероприятия для более точной категоризации
 */
@Serializable
enum class EventType(
    val displayName: String,
    val keywords: List<String>
) {
    MEETUP(
        displayName = "Митап",
        keywords = listOf(
            "митап",
            "meetup",
            "встреча",
            "gathering"
        )
    ),
    CONFERENCE(
        displayName = "Конференция",
        keywords = listOf(
            "конференция",
            "conference",
            "саммит",
            "summit",
            "форум",
            "forum"
        )
    ),
    WORKSHOP(
        displayName = "Воркшоп",
        keywords = listOf(
            "воркшоп",
            "workshop",
            "мастер-класс",
            "masterclass",
            "семинар",
            "seminar"
        )
    ),
    HACKATHON(
        displayName = "Хакатон",
        keywords = listOf(
            "хакатон",
            "hackathon",
            "hack",
            "хак"
        )
    ),
    GAME_JAM(
        displayName = "Геймджем",
        keywords = listOf(
            "gamejam",
            "геймджем",
            "game jam",
            "джем",
            "jam"
        )
    ),
    WEBINAR(
        displayName = "Вебинар",
        keywords = listOf(
            "вебинар",
            "webinar",
            "онлайн-встреча",
            "online meeting"
        )
    ),
    STREAM(
        displayName = "Стрим",
        keywords = listOf(
            "стрим",
            "stream",
            "live",
            "трансляция",
            "youtube live"
        )
    ),
    DIGEST(
        displayName = "Дайджест",
        keywords = listOf(
            "дайджест",
            "digest",
            "подборка",
            "афиша"
        )
    ),
    CONTEST(
        displayName = "Конкурс",
        keywords = listOf(
            "конкурс",
            "contest",
            "соревнование",
            "competition",
            "турнир",
            "tournament",
            "чемпионат",
            "ctf"
        )
    ),
    FESTIVAL(
        displayName = "Фестиваль",
        keywords = listOf(
            "фестиваль",
            "festival",
            "fest"
        )
    ),
    QUEST(
        displayName = "Квест",
        keywords = listOf(
            "квест",
            "quest",
            "челлендж",
            "challenge"
        )
    ),
    ACCELERATOR(
        displayName = "Акселератор",
        keywords = listOf(
            "акселератор",
            "accelerator",
            "программа",
            "program"
        )
    ),
    OTHER(
        displayName = "Другое",
        keywords = listOf()
    );

    companion object {
        /**
         * Определяет тип события на основе текста (заголовка/описания).
         */
        fun fromText(text: String): EventType {
            val lowerText = text.lowercase()
            return entries.firstOrNull { type ->
                type.keywords.any { keyword -> lowerText.contains(keyword) }
            } ?: OTHER
        }
    }
}

/**
 * Источник события.
 */
@Serializable
enum class EventSource(val displayName: String) {
    TELEGRAM("Telegram"),
    UNKNOWN("Другое")
}

/**
 * Категория события (технология/сфера).
 */
@Serializable
enum class EventCategory(
    val displayName: String,
    val keywords: List<String>,
    val emoji: String
) {
    ANDROID_DEV(
        displayName = "Android",
        keywords = listOf(
            "android",
            "kotlin",
            "jetpack",
            "compose",
            "mobile",
            "мобильная разработка"
        ),
        emoji = "📱"
    ),
    GAME_DEV(
        displayName = "GameDev",
        keywords = listOf(
            "gamedev",
            "unity",
            "unreal",
            "game",
            "игры",
            "геймдев",
            "game development",
            "геймдизайн",
            "gamejam",
            "геймджем",
            "game jam",
            "джем",
            "jam"
        ),
        emoji = "🎮"
    ),
    IOS_DEV(
        displayName = "iOS",
        keywords = listOf(
            "ios",
            "swift",
            "swiftui",
            "apple",
            "iphone",
            "ipad"
        ),
        emoji = "🍎"
    ),
    WEB_DEV(
        displayName = "Web",
        keywords = listOf(
            "web",
            "frontend",
            "backend",
            "javascript",
            "react",
            "vue",
            "angular",
            "node",
            "fullstack"
        ),
        emoji = "🌐"
    ),
    ML_AI(
        displayName = "ML/AI",
        keywords = listOf(
            "machine learning",
            "ai",
            "artificial intelligence",
            "ml",
            "нейросети",
            "data science",
            "deep learning"
        ),
        emoji = "🤖"
    ),
    DEVOPS(
        displayName = "DevOps",
        keywords = listOf(
            "devops",
            "kubernetes",
            "docker",
            "ci/cd",
            "infrastructure",
            "облако",
            "cloud"
        ),
        emoji = "⚙️"
    ),
    DESIGN(
        displayName = "Дизайн",
        keywords = listOf(
            "design",
            "дизайн",
            "ui",
            "ux",
            "figma",
            "sketch",
            "product design"
        ),
        emoji = "🎨"
    ),
    DATA_SCIENCE(
        displayName = "Data Science",
        keywords = listOf(
            "data science",
            "analytics",
            "аналитика",
            "big data",
            "данные",
            "bi"
        ),
        emoji = "📊"
    ),
    SECURITY(
        displayName = "Безопасность",
        keywords = listOf(
            "security",
            "безопасность",
            "cybersecurity",
            "кибербезопасность",
            "infosec"
        ),
        emoji = "🔒"
    ),
    BLOCKCHAIN(
        displayName = "Blockchain",
        keywords = listOf(
            "blockchain",
            "блокчейн",
            "crypto",
            "web3",
            "bitcoin",
            "ethereum"
        ),
        emoji = "⛓️"
    ),
    MANAGEMENT(
        displayName = "Менеджмент",
        keywords = listOf(
            "management",
            "менеджмент",
            "pm",
            "product",
            "agile",
            "scrum",
            "управление"
        ),
        emoji = "📋"
    ),
    QA(
        displayName = "QA/Testing",
        keywords = listOf(
            "qa",
            "testing",
            "тестирование",
            "автотестирование",
            "test automation"
        ),
        emoji = "🧪"
    ),
    OTHER(
        displayName = "Другое",
        keywords = listOf(),
        emoji = "📌"
    );

    companion object {
        /**
         * Определяет категорию события на основе текста.
         */
        fun fromText(text: String): EventCategory {
            val lowerText = text.lowercase()
            return entries.firstOrNull { category ->
                category.keywords.any { keyword -> lowerText.contains(keyword) }
            } ?: OTHER
        }
    }
}