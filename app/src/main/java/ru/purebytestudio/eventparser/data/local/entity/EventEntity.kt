package ru.purebytestudio.eventparser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ru.purebytestudio.eventparser.data.util.TagSanitizer
import ru.purebytestudio.eventparser.domain.model.Event
import ru.purebytestudio.eventparser.domain.model.EventCategory
import ru.purebytestudio.eventparser.domain.model.EventSource
import ru.purebytestudio.eventparser.domain.model.EventType
import java.time.LocalDateTime

/**
 * Сущность базы данных Room для хранения события.
 */
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["category"]),
        Index(value = ["source"]),
        Index(value = ["eventType"]),
        Index(value = ["isFavorite"]),
        Index(value = ["dateTime"]),
        Index(value = ["isFree"]),
        Index(value = ["title", "description"])
    ]
)
@TypeConverters(EventEntityConverters::class)
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val dateTime: String?,
    val endDateTime: String? = null,
    val location: String?,
    val isOnline: Boolean,
    val format: String? = null,
    val url: String,
    val source: String,
    val category: String,
    val eventType: String,
    val organizer: String?,
    val price: String?,
    val isFree: Boolean = false,
    val prizeFund: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val registrationDeadline: String? = null,
    val maxParticipants: Int? = null,
    val hasSpecificTime: Boolean = false,
    val localImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Event =
        Event(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            dateTime = dateTime?.let { LocalDateTime.parse(it) },
            endDateTime = endDateTime?.let { LocalDateTime.parse(it) },
            location = location,
            isOnline = isOnline,
            format = format,
            url = url,
            source = EventSource.entries.find { it.name == source } ?: EventSource.UNKNOWN,
            category = EventCategory.entries.find { it.name == category } ?: EventCategory.OTHER,
            eventType = EventType.entries.find { it.name == eventType } ?: EventType.OTHER,
            organizer = organizer,
            price = price,
            isFree = isFree,
            prizeFund = prizeFund,
            tags = TagSanitizer.sanitize(tags),
            isFavorite = isFavorite,
            registrationDeadline = registrationDeadline?.let { LocalDateTime.parse(it) },
            maxParticipants = maxParticipants,
            hasSpecificTime = hasSpecificTime,
            localImagePath = localImagePath
        )

    companion object {
        fun fromDomain(event: Event): EventEntity = EventEntity(
            id = event.id,
            title = event.title,
            description = event.description,
            imageUrl = event.imageUrl,
            dateTime = event.dateTime?.toString(),
            endDateTime = event.endDateTime?.toString(),
            location = event.location,
            isOnline = event.isOnline,
            format = event.format,
            url = event.url,
            source = event.source.name,
            category = event.category.name,
            eventType = event.eventType.name,
            organizer = event.organizer,
            price = event.price,
            isFree = event.isFree,
            prizeFund = event.prizeFund,
            tags = TagSanitizer.sanitize(event.tags),
            isFavorite = event.isFavorite,
            registrationDeadline = event.registrationDeadline?.toString(),
            maxParticipants = event.maxParticipants,
            hasSpecificTime = event.hasSpecificTime,
            localImagePath = event.localImagePath
        )
    }
}