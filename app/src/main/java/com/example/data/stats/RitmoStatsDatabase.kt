package com.example.data.stats

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ListeningEventEntity::class,
        TrackStatsEntity::class,
        SavedWrappedEntity::class,
        TrackMetadataOverrideEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RitmoStatsDatabase : RoomDatabase() {
    abstract fun statsDao(): RitmoStatsDao

    companion object {
        @Volatile
        private var INSTANCE: RitmoStatsDatabase? = null

        fun getInstance(context: Context): RitmoStatsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RitmoStatsDatabase::class.java,
                    "ritmo_local_stats.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
