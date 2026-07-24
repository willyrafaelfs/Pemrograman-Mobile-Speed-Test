package com.example.speedtest.model

/**
 * ═══════════════════════════════════════════════════════════════
 *  SpeedTestModels.kt
 *  Model layer: mendefinisikan state & sealed class untuk
 *  komunikasi antara SpeedTestManager ↔ ViewModel ↔ UI.
 * ═══════════════════════════════════════════════════════════════
 *  Author: Kukuh Yudhistiro, S.Kom., M.Kom.
 * ═══════════════════════════════════════════════════════════════
 */

// ──────────────────────────────────────────────────────
//  Fase pengujian — UI bereaksi berdasarkan fase ini
// ──────────────────────────────────────────────────────
enum class TestPhase {
    IDLE,               // Belum dimulai / menunggu tombol ditekan
    TESTING_PING,       // Sedang mengukur latency (ping)
    TESTING_JITTER,     // Sedang menampilkan hasil jitter
    TESTING_DOWNLOAD,   // Sedang mengukur kecepatan download
    TESTING_UPLOAD,     // Sedang mengukur kecepatan upload
    FINISHED            // Semua pengujian selesai
}

// ──────────────────────────────────────────────────────
//  UI State — satu sumber kebenaran untuk seluruh UI
// ──────────────────────────────────────────────────────
data class SpeedTestUiState(
    val phase: TestPhase = TestPhase.IDLE,

    // Hasil akhir masing-masing tes
    val pingResult: Double = 0.0,       // dalam milidetik (ms)
    val jitterResult: Double = 0.0,     // dalam milidetik (ms)
    val downloadSpeed: Double = 0.0,    // dalam Megabit per second (Mbps)
    val uploadSpeed: Double = 0.0,      // dalam Megabit per second (Mbps)

    // Nilai kecepatan real-time (untuk animasi gauge)
    val currentSpeed: Double = 0.0,

    // Progress bar 0f..1f untuk keseluruhan tes
    val progress: Float = 0f,

    // Pesan error (null jika tidak ada error)
    val errorMessage: String? = null,

    // Server yang sedang dipilih untuk pengetesan
    val selectedServer: ServerInfo? = null,

    // Informasi jaringan saat ini
    val networkInfo: NetworkInfo = NetworkInfo()
)

/**
 * Model data untuk informasi jaringan.
 */
data class NetworkInfo(
    val type: String = "Offline",
    val ipAddress: String = "—",
    val isConnected: Boolean = false
)

/**
 * Model data untuk informasi server pengetesan.
 */
data class ServerInfo(
    val name: String,
    val provider: String,
    val location: String,
    val pingHost: String,
    val downloadUrl: String,
    val uploadUrl: String
)

// ──────────────────────────────────────────────────────
//  Sealed class untuk update dari SpeedTestManager
//  ke ViewModel secara streaming via Flow
// ──────────────────────────────────────────────────────

/** Update hasil pengukuran Ping */
sealed class PingUpdate {
    data object Started : PingUpdate()
    data class Progress(val rttMs: Double) : PingUpdate()
    data class Completed(
        val avgRttMs: Double,
        val jitterMs: Double
    ) : PingUpdate()
    data class Error(val message: String) : PingUpdate()
}

/** Update hasil pengukuran Download / Upload speed */
sealed class SpeedUpdate {
    data object Started : SpeedUpdate()
    data class Progress(
        val speedMbps: Double,      // Kecepatan real-time saat ini
        val totalBytes: Long        // Total bytes yang sudah ditransfer
    ) : SpeedUpdate()
    data class Completed(
        val finalSpeedMbps: Double, // Kecepatan rata-rata akhir
        val totalBytes: Long        // Total bytes yang ditransfer
    ) : SpeedUpdate()
    data class Error(val message: String) : SpeedUpdate()
}
