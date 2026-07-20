package com.example.speedtest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity untuk menyimpan hasil pengetesan kecepatan internet.
 */
@Entity(tableName = "speed_test_results")
data class SpeedTestResult(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ping: Double,
    val download: Double,
    val upload: Double,
    val timestamp: Long = System.currentTimeMillis()
)
