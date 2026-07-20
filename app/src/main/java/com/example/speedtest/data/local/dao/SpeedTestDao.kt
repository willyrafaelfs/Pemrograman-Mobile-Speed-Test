package com.example.speedtest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.speedtest.data.local.entity.SpeedTestResult
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk tabel speed_test_results.
 */
@Dao
interface SpeedTestDao {
    @Query("SELECT * FROM speed_test_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<SpeedTestResult>>

    @Insert
    suspend fun insertResult(result: SpeedTestResult)

    @Query("DELETE FROM speed_test_results")
    suspend fun clearHistory()
}
