package ru.purebytestudio.eventparser.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.purebytestudio.eventparser.data.local.dao.EventDao
import ru.purebytestudio.eventparser.data.local.entity.EventEntity
import ru.purebytestudio.eventparser.data.local.entity.EventEntityConverters

@Database(
    entities = [EventEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(EventEntityConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        const val DATABASE_NAME = "event_parser_db"
    }
}