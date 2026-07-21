package com.example.speedtest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedtest.data.SpeedTestManager
import com.example.speedtest.data.local.AppDatabase
import com.example.speedtest.data.local.entity.SpeedTestResult
import com.example.speedtest.model.PingUpdate
import com.example.speedtest.model.ServerInfo
import com.example.speedtest.model.SpeedTestUiState
import com.example.speedtest.model.SpeedUpdate
import com.example.speedtest.model.TestPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════
 *  SpeedTestViewModel.kt
 *  ViewModel Layer: mengelola state pengujian dan menjembatani
 *  antara SpeedTestManager (data) dan UI (Composable).
 *
 *  Pola yang digunakan:
 *  - Unidirectional Data Flow (UDF)
 *  - StateFlow sebagai single source of truth
 *  - viewModelScope untuk lifecycle-aware coroutine
 *
 *  Alur data:
 *  UI (event) → ViewModel → SpeedTestManager → Flow<Update>
 *       ↑                                          │
 *       └────── StateFlow<UiState> ←──────────────┘
 * ═══════════════════════════════════════════════════════════════
 *  Author: Kukuh Yudhistiro, S.Kom., M.Kom.
 * ═══════════════════════════════════════════════════════════════
 */
class SpeedTestViewModel(private val database: AppDatabase) : ViewModel() {

    companion object {
        // ── Daftar server default yang bisa dipilih user ─────────
        // Catatan: Google & OVH tidak menyediakan endpoint upload
        // publik, sehingga uploadUrl keduanya menggunakan endpoint
        // Cloudflare (kecepatan upload ditentukan oleh koneksi
        // klien, bukan oleh server tujuan, sehingga hasil tetap valid).
        val AVAILABLE_SERVERS = listOf(
            ServerInfo(
                name = "Cloudflare",
                provider = "Cloudflare",
                location = "Global (Anycast)",
                pingHost = "1.1.1.1",
                downloadUrl = "https://speed.cloudflare.com/__down?bytes=10000000",
                uploadUrl = "https://speed.cloudflare.com/__up"
            ),
            ServerInfo(
                name = "Google",
                provider = "Google",
                location = "Global",
                pingHost = "8.8.8.8",
                downloadUrl = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip",
                uploadUrl = "https://speed.cloudflare.com/__up"
            ),
            ServerInfo(
                name = "OVH",
                provider = "OVH",
                location = "Roubaix, France",
                pingHost = "ovh.net",
                downloadUrl = "https://proof.ovh.net/files/10Mb.dat",
                uploadUrl = "https://speed.cloudflare.com/__up"
            )
        )
    }

    // ── Data Layer ───────────────────────────────────────────
    private val speedTestManager = SpeedTestManager()
    private val speedTestDao = database.speedTestDao()

    // ── State Management ─────────────────────────────────────
    // MutableStateFlow hanya diakses internal ViewModel
    private val _uiState = MutableStateFlow(
        SpeedTestUiState(selectedServer = AVAILABLE_SERVERS.first())
    )

    // Public StateFlow yang di-observe oleh UI (read-only)
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    // Flow histori hasil tes
    val historyResults: StateFlow<List<SpeedTestResult>> = speedTestDao.getAllResults()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Dipanggil ketika user menekan tombol "Start Test".
     * Menjalankan tiga pengujian secara sekuensial:
     * 1. Ping → 2. Download → 3. Upload
     *
     * Menggunakan viewModelScope agar coroutine otomatis
     * di-cancel ketika ViewModel dihancurkan (lifecycle-safe).
     */
    fun startSpeedTest() {
        // Cegah multiple test berjalan bersamaan
        if (_uiState.value.phase != TestPhase.IDLE &&
            _uiState.value.phase != TestPhase.FINISHED
        ) return

        val server = _uiState.value.selectedServer ?: AVAILABLE_SERVERS.first()

        viewModelScope.launch {
            // Reset state ke kondisi awal, tetap pertahankan server terpilih
            _uiState.update {
                SpeedTestUiState(phase = TestPhase.IDLE, selectedServer = server)
            }

            // ── Tahap 1: Ping Test ──────────────────────────
            runPingTest(server)

            // ── Tahap 2: Download Test ──────────────────────
            runDownloadTest(server)

            // ── Tahap 3: Upload Test ────────────────────────
            runUploadTest(server)

            // ── Selesai ─────────────────────────────────────
            _uiState.update { current ->
                current.copy(
                    phase = TestPhase.FINISHED,
                    currentSpeed = 0.0,
                    progress = 1f
                )
            }

            // Simpan hasil ke database
            saveResultToHistory()
        }
    }

    private suspend fun saveResultToHistory() {
        val state = _uiState.value
        if (state.pingResult >= 0 && state.downloadSpeed >= 0 && state.uploadSpeed >= 0) {
            val result = SpeedTestResult(
                ping = state.pingResult,
                download = state.downloadSpeed,
                upload = state.uploadSpeed
            )
            speedTestDao.insertResult(result)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            speedTestDao.clearHistory()
        }
    }

    /**
     * Reset state ke IDLE agar user bisa memulai test baru.
     */
    fun resetTest() {
        _uiState.update { current ->
            SpeedTestUiState(selectedServer = current.selectedServer)
        }
    }

    /**
     * Dipanggil ketika user memilih server baru dari daftar.
     * Diabaikan jika sedang ada test yang berjalan.
     */
    fun selectServer(server: ServerInfo) {
        if (_uiState.value.phase != TestPhase.IDLE &&
            _uiState.value.phase != TestPhase.FINISHED
        ) return

        _uiState.update { it.copy(selectedServer = server) }
    }

    // ╔═══════════════════════════════════════════════════════╗
    // ║  Private: Menjalankan setiap fase test                 ║
    // ╚═══════════════════════════════════════════════════════╝

    private suspend fun runPingTest(server: ServerInfo) {
        _uiState.update { it.copy(phase = TestPhase.TESTING_PING, progress = 0f) }

        speedTestManager.measurePing(server).collect { update ->
            when (update) {
                is PingUpdate.Started -> {
                    // Ping dimulai — UI bisa menampilkan animasi loading
                }
                is PingUpdate.Progress -> {
                    // Update ping progress — tampilkan RTT per-paket
                    _uiState.update { current ->
                        current.copy(
                            currentSpeed = update.rttMs,
                            progress = 0.1f  // Ping = 10% dari total proses
                        )
                    }
                }
                is PingUpdate.Completed -> {
                    // Simpan hasil rata-rata ping
                    _uiState.update { current ->
                        current.copy(
                            pingResult = update.avgRttMs,
                            currentSpeed = update.avgRttMs,
                            progress = 0.15f
                        )
                    }
                }
                is PingUpdate.Error -> {
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = "Ping: ${update.message}",
                            pingResult = -1.0  // -1 menandakan error
                        )
                    }
                }
            }
        }
    }

    private suspend fun runDownloadTest(server: ServerInfo) {
        _uiState.update { current ->
            current.copy(
                phase = TestPhase.TESTING_DOWNLOAD,
                currentSpeed = 0.0,
                progress = 0.15f
            )
        }

        speedTestManager.measureDownload(server).collect { update ->
            when (update) {
                is SpeedUpdate.Started -> { /* Download dimulai */ }
                is SpeedUpdate.Progress -> {
                    _uiState.update { current ->
                        current.copy(
                            currentSpeed = update.speedMbps,
                            // Download = 15%..65% dari total progress
                            progress = 0.15f + (0.5f * (update.totalBytes.toFloat() / 10_000_000f))
                                .coerceAtMost(0.5f)
                        )
                    }
                }
                is SpeedUpdate.Completed -> {
                    _uiState.update { current ->
                        current.copy(
                            downloadSpeed = update.finalSpeedMbps,
                            progress = 0.65f
                        )
                    }
                }
                is SpeedUpdate.Error -> {
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = "Download: ${update.message}",
                            downloadSpeed = -1.0
                        )
                    }
                }
            }
        }
    }

    private suspend fun runUploadTest(server: ServerInfo) {
        _uiState.update { current ->
            current.copy(
                phase = TestPhase.TESTING_UPLOAD,
                currentSpeed = 0.0,
                progress = 0.65f
            )
        }

        speedTestManager.measureUpload(server).collect { update ->
            when (update) {
                is SpeedUpdate.Started -> { /* Upload dimulai */ }
                is SpeedUpdate.Progress -> {
                    _uiState.update { current ->
                        current.copy(
                            currentSpeed = update.speedMbps,
                            // Upload = 65%..95% dari total progress
                            progress = 0.65f + (0.3f * (update.totalBytes.toFloat() / 5_000_000f))
                                .coerceAtMost(0.3f)
                        )
                    }
                }
                is SpeedUpdate.Completed -> {
                    _uiState.update { current ->
                        current.copy(
                            uploadSpeed = update.finalSpeedMbps,
                            progress = 0.95f
                        )
                    }
                }
                is SpeedUpdate.Error -> {
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = "Upload: ${update.message}",
                            uploadSpeed = -1.0
                        )
                    }
                }
            }
        }
    }
}
