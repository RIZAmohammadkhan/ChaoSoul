package com.yourdomain.chaosoul.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.yourdomain.chaosoul.data.model.TypingEvent

@Dao
interface TypingEventDao {
    @Insert
    suspend fun insert(event: TypingEvent): Long

    @Query("SELECT SUM(durationMillis) FROM typing_events WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getTotalDurationBetween(startTime: Long, endTime: Long): Long?

    @Query("DELETE FROM typing_events WHERE timestamp < :olderThan")
    suspend fun clearOldEvents(olderThan: Long): Int
}