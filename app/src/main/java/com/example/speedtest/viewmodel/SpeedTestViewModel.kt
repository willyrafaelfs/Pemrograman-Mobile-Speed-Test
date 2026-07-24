package com.example.speedtest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedtest.data.NetworkMonitor
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ═══════════════════════════════════════════════════════════════
 *  SpeedTestViewModel.kt
 *  ViewModel Layer: mengelola state pengujian dan menjembatani
 *  antara SpeedTestManager (data) dan UI (Composable).
 * ═══════════════════════════════════════════════════════════════
 */
class SpeedTestViewModel(
    application: Application,
    private val database: AppDatabase
) : AndroidViewModel(application) {

    companion object {
        // ── Daftar server default yang bisa dipilih user ─────────
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
                name = "Linode",
                provider = "Linode (Akamai)",
                location = "Singapore",
                pingHost = "speedtest.singapore.linode.com",
                downloadUrl = "https://speedtest.singapore.linode.com/100MB-singapore.bin",
                uploadUrl = "https://speed.cloudflare.com/__up"
            )
        )

        // ── Jeda animasi gauge saat berpindah antar fase ──────────
        // Nilai ini disamakan dengan durasi tween di SpeedGauge (600ms)
        // agar jarum sempat terlihat turun kembali ke 0 sebelum
        // fase berikutnya dimulai.
        private const val GAUGE_SETTLE_DELAY_MS = 650L
        private const val JITTER_REVEAL_DELAY_MS = 200L
        private const val JITTER_HOLD_DELAY_MS = 700L
    }

    // ── Data Layer ───────────────────────────────────────────
    private val speedTestManager = SpeedTestManager()
    private val speedTestDao = database.speedTestDao()
    private val networkMonitor = NetworkMonitor(application)

    // ── State Management ─────────────────────────────────────
    private val _uiState = MutableStateFlow(
        SpeedTestUiState(selectedServer = AVAILABLE_SERVERS.first())
    )
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    // Job tes yang sedang berjalan, disimpan agar bisa dibatalkan
    private var testJob: Job? = null

    // Flow histori hasil tes
    val historyResults: StateFlow<List<SpeedTestResult>> = speedTestDao.getAllResults()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pantau perubahan jaringan secara real-time
        networkMonitor.networkInfo
            .onEach { info ->
                _uiState.update { it.copy(networkInfo = info) }
            }
            .launchIn(viewModelScope)
    }

    fun startSpeedTest() {
        if (_uiState.value.phase != TestPhase.IDLE &&
            _uiState.value.phase != TestPhase.FINISHED
        ) return

        val server = _uiState.value.selectedServer ?: AVAILABLE_SERVERS.first()

        testJob = viewModelScope.launch {
            _uiState.update {
                SpeedTestUiState(
                    phase = TestPhase.IDLE,
                    selectedServer = server,
                    networkInfo = it.networkInfo
                )
            }

            val jitterValue = runPingTest(server)
            runJitterPhase(jitterValue)
            runDownloadTest(server)
            runUploadTest(server)

            _uiState.update { current ->
                current.copy(
                    phase = TestPhase.FINISHED,
                    currentSpeed = 0.0,
                    progress = 1f
                )
            }

            saveResultToHistory()
        }
    }

    /** Membatalkan tes yang sedang berjalan dan mengembalikan UI ke kondisi awal. */
    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _uiState.update { current ->
            SpeedTestUiState(
                selectedServer = current.selectedServer,
                networkInfo = current.networkInfo
            )
        }
    }

    private suspend fun saveResultToHistory() {
        val state = _uiState.value
        if (state.pingResult >= 0 && state.downloadSpeed >= 0 && state.uploadSpeed >= 0) {
            val result = SpeedTestResult(
                ping = state.pingResult,
                jitter = state.jitterResult,
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

    fun resetTest() {
        _uiState.update { current ->
            SpeedTestUiState(
                selectedServer = current.selectedServer,
                networkInfo = current.networkInfo
            )
        }
    }

    fun selectServer(server: ServerInfo) {
        if (_uiState.value.phase != TestPhase.IDLE &&
            _uiState.value.phase != TestPhase.FINISHED
        ) return

        _uiState.update { it.copy(selectedServer = server) }
    }

    /**
     * Menghasilkan teks ringkasan untuk dibagikan.
     */
    fun getShareSummary(): String {
        val state = _uiState.value
        val server = state.selectedServer?.name ?: "Unknown"
        return """
            🚀 Hasil Internet Speed Test
            ---------------------------
            📍 Server: $server
            📶 Ping: ${state.pingResult.toInt()} ms
            〰️ Jitter: ${state.jitterResult.toInt()} ms
            ⬇️ Download: ${String.format("%.2f", state.downloadSpeed)} Mbps
            ⬆️ Upload: ${String.format("%.2f", state.uploadSpeed)} Mbps
            
            Dites menggunakan aplikasi Android Speed Test.
        """.trimIndent()
    }

    /**
     * Menghasilkan konten CSV dari seluruh histori.
     */
    fun getHistoryCsvContent(history: List<SpeedTestResult>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        
        // Header
        sb.append("ID,Waktu,Ping(ms),Jitter(ms),Download(Mbps),Upload(Mbps)\n")
        
        // Data
        history.forEach { res ->
            sb.append("${res.id},")
            sb.append("${sdf.format(Date(res.timestamp))},")
            sb.append("${res.ping},")
            sb.append("${res.jitter},")
            sb.append("${res.download},")
            sb.append("${res.upload}\n")
        }
        
        return sb.toString()
    }

    /** Menurunkan jarum gauge ke 0 dan menahannya sejenak agar terlihat oleh user. */
    private suspend fun settleGaugeToZero() {
        _uiState.update { it.copy(currentSpeed = 0.0) }
        delay(GAUGE_SETTLE_DELAY_MS)
    }

    private suspend fun runPingTest(server: ServerInfo): Double {
        _uiState.update { it.copy(phase = TestPhase.TESTING_PING, progress = 0f, currentSpeed = 0.0) }

        var jitterValue = 0.0

        speedTestManager.measurePing(server).collect { update ->
            when (update) {
                is PingUpdate.Started -> {}
                is PingUpdate.Progress -> {
                    _uiState.update { current ->
                        current.copy(
                            currentSpeed = update.rttMs,
                            progress = 0.1f
                        )
                    }
                }
                is PingUpdate.Completed -> {
                    jitterValue = update.jitterMs
                    _uiState.update { current ->
                        current.copy(
                            pingResult = update.avgRttMs,
                            currentSpeed = update.avgRttMs,
                            progress = 0.12f
                        )
                    }
                }
                is PingUpdate.Error -> {
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = "Ping: ${update.message}",
                            pingResult = -1.0
                        )
                    }
                }
            }
        }

        // Tahan hasil ping sejenak, lalu turunkan jarum ke 0
        // sebelum berpindah ke fase Jitter
        delay(JITTER_REVEAL_DELAY_MS)
        settleGaugeToZero()

        return jitterValue
    }

    private suspend fun runJitterPhase(jitterValue: Double) {
        _uiState.update { current ->
            current.copy(phase = TestPhase.TESTING_JITTER, progress = 0.15f, currentSpeed = 0.0)
        }

        // Jeda kecil agar transisi fase terlihat sebelum jarum bergerak
        delay(JITTER_REVEAL_DELAY_MS)

        _uiState.update { current ->
            current.copy(jitterResult = jitterValue, currentSpeed = jitterValue, progress = 0.2f)
        }

        // Tahan nilai jitter agar sempat terbaca oleh user
        delay(JITTER_HOLD_DELAY_MS)

        // Turunkan jarum ke 0 sebelum berpindah ke fase Download
        settleGaugeToZero()
    }

    private suspend fun runDownloadTest(server: ServerInfo) {
        _uiState.update { current ->
            current.copy(
                phase = TestPhase.TESTING_DOWNLOAD,
                currentSpeed = 0.0,
                progress = 0.2f
            )
        }

        speedTestManager.measureDownload(server).collect { update ->
            when (update) {
                is SpeedUpdate.Started -> {}
                is SpeedUpdate.Progress -> {
                    _uiState.update { current ->
                        current.copy(
                            currentSpeed = update.speedMbps,
                            progress = 0.2f + (0.5f * (update.totalBytes.toFloat() / 10_000_000f))
                                .coerceAtMost(0.5f)
                        )
                    }
                }
                is SpeedUpdate.Completed -> {
                    _uiState.update { current ->
                        current.copy(
                            downloadSpeed = update.finalSpeedMbps,
                            currentSpeed = update.finalSpeedMbps,
                            progress = 0.7f
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

        // Turunkan jarum ke 0 sebelum berpindah ke fase Upload
        settleGaugeToZero()
    }

    private suspend fun runUploadTest(server: ServerInfo) {
        _uiState.update { current ->
            current.copy(
                phase = TestPhase.TESTING_UPLOAD,
                currentSpeed = 0.0,
                progress = 0.7f
            )
        }

        speedTestManager.measureUpload(server).collect { update ->
            when (update) {
                is SpeedUpdate.Started -> {}
                is SpeedUpdate.Progress -> {
                    _uiState.update { current ->
                        current.copy(
                            currentSpeed = update.speedMbps,
                            progress = 0.7f + (0.25f * (update.totalBytes.toFloat() / 5_000_000f))
                                .coerceAtMost(0.25f)
                        )
                    }
                }
                is SpeedUpdate.Completed -> {
                    _uiState.update { current ->
                        current.copy(
                            uploadSpeed = update.finalSpeedMbps,
                            currentSpeed = update.finalSpeedMbps,
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

        // Turunkan jarum ke 0 setelah semua tes selesai
        settleGaugeToZero()
    }
}
