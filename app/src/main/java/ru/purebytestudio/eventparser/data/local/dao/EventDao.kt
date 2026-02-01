package ru.purebytestudio.eventparser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.purebytestudio.eventparser.data.local.entity.EventEntity

/**
 * Data Access Object (DAO) для работы с таблицей событий.
 */
@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE isFavorite = 1 ORDER BY dateTime ASC")
    fun getFavoriteEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isFavorite = 1 ORDER BY dateTime ASC")
    suspend fun getFavoriteEventsList(): List<EventEntity>

    @Query("SELECT * FROM events ORDER BY dateTime ASC")
    suspend fun getAllEvents(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?

    @Query(
        """
        SELECT * FROM events
        WHERE (:category IS NULL OR category = :category)
        AND (:requireOnline = 0 OR isOnline = 1)
        AND (:requireFree = 0 OR isFree = 1)
        AND (:startDate IS NULL OR dateTime >= :startDate)
        AND (:endDate IS NULL OR dateTime <= :endDate)
        ORDER BY dateTime ASC
        """
    )
    fun getFilteredEvents(
        category: String?,
        requireOnline: Boolean,
        requireFree: Boolean,
        startDate: String?,
        endDate: String?
    ): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("UPDATE events SET isFavorite = :isFavorite WHERE id = :eventId")
    suspend fun setFavorite(
        eventId: String,
        isFavorite: Boolean
    )

    @Query("UPDATE events SET localImagePath = :path WHERE id = :eventId")
    suspend fun setLocalImagePath(
        eventId: String,
        path: String?
    )

    @Query("SELECT isFavorite FROM events WHERE id = :eventId")
    suspend fun isFavorite(eventId: String): Boolean?

    /**
     * Удалить прошедшие события (кроме избранных).
     */
    @Query(
        """
        DELETE FROM events
        WHERE isFavorite = 0
        AND datetime(replace(dateTime, 'T', ' ')) < datetime('now')
    """
    )
    suspend fun deletePastEvents()

    /**
     * Удалить события по их ID (для дедупликации).
     */
    @Query("DELETE FROM events WHERE id IN (:ids) AND isFavorite = 0")
    suspend fun deleteEventsByIds(ids: List<String>)

    /**
     * Получить события в диапазоне дат (включительно).
     */
    @Query("SELECT * FROM events WHERE dateTime >= :startDate AND dateTime <= :endDate")
    suspend fun getEventsBetweenDates(
        startDate: String,
        endDate: String
    ): List<EventEntity>
}