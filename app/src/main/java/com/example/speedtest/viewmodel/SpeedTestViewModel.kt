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
    private val networkMonitor = NetworkMonitor(application)

    // ── State Management ─────────────────────────────────────
    private val _uiState = MutableStateFlow(
        SpeedTestUiState(selectedServer = AVAILABLE_SERVERS.first())
    )
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

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

        viewModelScope.launch {
            _uiState.update {
                SpeedTestUiState(
                    phase = TestPhase.IDLE, 
                    selectedServer = server,
                    networkInfo = it.networkInfo
                )
            }

            runPingTest(server)
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

    private suspend fun runPingTest(server: ServerInfo) {
        _uiState.update { it.copy(phase = TestPhase.TESTING_PING, progress = 0f) }

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
                    _uiState.update { current ->
                        current.copy(
                            pingResult = update.avgRttMs,
                            jitterResult = update.jitterMs,
                            currentSpeed = update.avgRttMs,
                            progress = 0.15f
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
                is SpeedUpdate.Started -> {}
                is SpeedUpdate.Progress -> {
                    _uiState.update { current ->
                        current.copy(
                            currentSpeed = update.speedMbps,
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
                is SpeedUpdate.Started -> {}
                is SpeedUpdate.Progress -> {
                    _uiState.update { current ->
                        current.copy(
                            currentSpeed = update.speedMbps,
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
