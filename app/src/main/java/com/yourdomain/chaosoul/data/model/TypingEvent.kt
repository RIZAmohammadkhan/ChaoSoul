package com.yourdomain.chaosoul.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "typing_events")
data class TypingEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val durationMillis: Long
)