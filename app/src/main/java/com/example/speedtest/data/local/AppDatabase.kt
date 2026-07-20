package com.example.speedtest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.speedtest.data.local.dao.SpeedTestDao
import com.example.speedtest.data.local.entity.SpeedTestResult

/**
 * Database utama aplikasi.
 */
@Database(entities = [SpeedTestResult::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun speedTestDao(): SpeedTestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speedtest_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
