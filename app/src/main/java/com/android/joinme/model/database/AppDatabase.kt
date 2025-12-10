package com.android.joinme.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for offline caching. Stores events locally for offline viewing.
 *
 * This is a singleton database - only one instance exists per application. Use
 * [AppDatabase.getDatabase] to get the instance.
 */
@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun eventDao(): EventDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    /**
     * Gets the singleton database instance. Creates it if it doesn't exist.
     *
     * @param context Application context
     * @return The database instance
     */
    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE
          ?: synchronized(this) {
            val instance =
                Room.databaseBuilder(
                        context.applicationContext, AppDatabase::class.java, "joinme_database")
                    .build()
            INSTANCE = instance
            instance
          }
    }

    /** For testing only - allows setting a custom database instance. */
    @androidx.annotation.VisibleForTesting
    fun setTestInstance(database: AppDatabase?) {
      INSTANCE?.close() // Close existing instance to prevent leaks
      INSTANCE = database
    }
  }
}
