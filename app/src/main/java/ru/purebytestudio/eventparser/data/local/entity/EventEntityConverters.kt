package ru.purebytestudio.eventparser.data.local.entity

import androidx.room.TypeConverter

/**
 * TypeConverters для Room, относящиеся к сущности [EventEntity].
 *
 * Важно: формат сериализации должен оставаться стабильным, иначе сломается чтение старых данных.
 */
internal class EventEntityConverters {
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return list.joinToString(",")
    }
}