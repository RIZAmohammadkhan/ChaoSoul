package com.yourdomain.chaosoul.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yourdomain.chaosoul.data.model.TypingEvent

@Database(entities = [TypingEvent::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun typingEventDao(): TypingEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chaosoul_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}