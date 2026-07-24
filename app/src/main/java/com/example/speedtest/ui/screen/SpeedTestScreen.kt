package com.example.speedtest.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedtest.model.ServerInfo
import com.example.speedtest.model.SpeedTestUiState
import com.example.speedtest.model.TestPhase
import com.example.speedtest.ui.components.ResultCardsRow
import com.example.speedtest.ui.components.SpeedGauge
import com.example.speedtest.ui.theme.DownloadColor
import com.example.speedtest.ui.theme.JitterColor
import com.example.speedtest.ui.theme.PingColor
import com.example.speedtest.ui.theme.SpeedTestTheme
import com.example.speedtest.ui.theme.UploadColor
import com.example.speedtest.util.ShareUtils
import com.example.speedtest.viewmodel.SpeedTestViewModel

/**
 * ═══════════════════════════════════════════════════════════════
 *  SpeedTestScreen.kt — Layar utama Speed Test
 *
 *  Layout:
 *  ┌──────────────────────────────────┐
 *  │ 🌐 Internet Speed Test          │ TopAppBar
 *  ├──────────────────────────────────┤
 *  │                                  │
 *  │    ╭──────────────────────╮      │
 *  │    │   Circular Gauge     │      │ SpeedGauge
 *  │    │   42.5 Mbps          │      │
 *  │    │   Download           │      │
 *  │    ╰──────────────────────╯      │
 *  │                                  │
 *  │   ━━━━━━━━━━━━━━━━━━━━━━━━━━━   │ LinearProgress
 *  │   "Mengukur Download Speed..."   │ StatusText
 *  │                                  │
 *  │  ┌────┐  ┌────────┐  ┌──────┐   │
 *  │  │Ping│  │Download│  │Upload│   │ ResultCards
 *  │  │12ms│  │  45.8  │  │ 18.3 │   │
 *  │  └────┘  └────────┘  └──────┘   │
 *  │                                  │
 *  │  ┌────────────────────────────┐  │
 *  │  │      START TEST            │  │ Button
 *  │  └────────────────────────────┘  │
 *  └──────────────────────────────────┘
 *
 *  State-driven: seluruh UI bereaksi terhadap TestPhase
 *  yang dikelola oleh SpeedTestViewModel via StateFlow.
 * ═══════════════════════════════════════════════════════════════
 *  Author: Kukuh Yudhistiro, S.Kom., M.Kom.
 * ═══════════════════════════════════════════════════════════════
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showHistory by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showHistory) {
        HistoryScreen(
            viewModel = viewModel,
            onBack = { showHistory = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Internet Speed Test",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    actions = {
                        if (uiState.phase == TestPhase.FINISHED) {
                            IconButton(onClick = {
                                ShareUtils.shareText(context, viewModel.getShareSummary())
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Hasil")
                            }
                        }
                        IconButton(onClick = { showHistory = true }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            SpeedTestContent(
                uiState = uiState,
                availableServers = SpeedTestViewModel.AVAILABLE_SERVERS,
                onStartTest = { viewModel.startSpeedTest() },
                onResetTest = { viewModel.resetTest() },
                onCancelTest = { viewModel.cancelTest() },
                onServerSelected = { viewModel.selectServer(it) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun SpeedTestContent(
    uiState: SpeedTestUiState,
    availableServers: List<ServerInfo>,
    onStartTest: () -> Unit,
    onResetTest: () -> Unit,
    onCancelTest: () -> Unit,
    onServerSelected: (ServerInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTesting = uiState.phase != TestPhase.IDLE &&
            uiState.phase != TestPhase.FINISHED
    var showServerSheet by remember { mutableStateOf(false) }

    if (showServerSheet) {
        ServerSelectionSheet(
            servers = availableServers,
            selectedServer = uiState.selectedServer,
            onServerSelected = {
                onServerSelected(it)
                showServerSheet = false
            },
            onDismiss = { showServerSheet = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Server Selection ─────────────────────────────────
        ServerSelectionSection(
            selectedServer = uiState.selectedServer,
            enabled = !isTesting,
            onChangeServerClick = { showServerSheet = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Network Info ─────────────────────────────────────
        NetworkInfoSection(networkInfo = uiState.networkInfo)

        // ── Gauge Section ────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            SpeedGauge(
                currentSpeed = resolveGaugeSpeed(uiState),
                maxSpeed = resolveMaxSpeed(uiState),
                phaseLabel = resolvePhaseLabel(uiState),
                unitLabel = resolveUnitLabel(uiState),
                gaugeSize = 280.dp
            )
        }

        // ── Progress & Status ────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Linear progress bar (selama testing)
            AnimatedVisibility(visible = isTesting) {
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = 32.dp),
                    color = resolvePhaseColor(uiState),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teks status
            StatusText(uiState = uiState)

            // Error message
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Result Cards ─────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.phase != TestPhase.IDLE,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut()
        ) {
            ResultCardsRow(
                pingMs = uiState.pingResult,
                jitterMs = uiState.jitterResult,
                downloadMbps = uiState.downloadSpeed,
                uploadMbps = uiState.uploadSpeed,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // ── Ringkasan Kualitas Koneksi ────────────────────────
        AnimatedVisibility(
            visible = uiState.phase == TestPhase.FINISHED,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut()
        ) {
            ConnectionQualityCard(
                pingMs = uiState.pingResult,
                downloadMbps = uiState.downloadSpeed,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Action Button ────────────────────────────────────
        ActionButton(
            phase = uiState.phase,
            onStartTest = onStartTest,
            onResetTest = onResetTest,
            onCancelTest = onCancelTest,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

// ╔═══════════════════════════════════════════════════════════╗
// ║  Sub-Composables                                          ║
// ╚═══════════════════════════════════════════════════════════╝

@Composable
private fun StatusText(uiState: SpeedTestUiState) {
    val serverName = uiState.selectedServer?.name ?: "server"
    val (text, pulsate) = when (uiState.phase) {
        TestPhase.IDLE -> "Siap mengukur koneksi internet" to false
        TestPhase.TESTING_PING -> "Mengukur Ping ke $serverName..." to true
        TestPhase.TESTING_JITTER -> "Mengukur Jitter..." to true
        TestPhase.TESTING_DOWNLOAD -> "Mengukur Download Speed dari $serverName..." to true
        TestPhase.TESTING_UPLOAD -> "Mengukur Upload Speed ke $serverName..." to true
        TestPhase.FINISHED -> "Pengujian selesai" to false
    }

    // Animasi pulse saat testing
    val alpha = if (pulsate) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val animAlpha by transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        animAlpha
    } else 1f

    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.alpha(alpha)
    )
}

@Composable
private fun NetworkInfoSection(
    networkInfo: com.example.speedtest.model.NetworkInfo,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color) = if (networkInfo.isConnected) {
            when (networkInfo.type) {
                "WiFi" -> Icons.Default.Wifi to MaterialTheme.colorScheme.primary
                else -> Icons.Default.Public to MaterialTheme.colorScheme.primary
            }
        } else {
            Icons.Default.WifiOff to MaterialTheme.colorScheme.error
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = if (networkInfo.isConnected) {
                "${networkInfo.type} · ${networkInfo.ipAddress}"
            } else {
                "Terputus dari Internet"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (networkInfo.isConnected) 
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            else 
                MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Kartu ringkasan yang memberi tahu user apakah koneksinya
 * tergolong lambat, sedang, cepat, atau sangat cepat — berdasarkan
 * kecepatan download hasil test, plus catatan singkat soal latency.
 */
@Composable
private fun ConnectionQualityCard(
    pingMs: Double,
    downloadMbps: Double,
    modifier: Modifier = Modifier
) {
    if (downloadMbps < 0) return

    val quality = resolveConnectionQuality(downloadMbps)
    val latencyNote = resolveLatencyNote(pingMs)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = quality.color.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = quality.icon,
                contentDescription = null,
                tint = quality.color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Koneksi Internet: ${quality.label}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = quality.color
                )
                Text(
                    text = quality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (latencyNote != null) {
                    Text(
                        text = latencyNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class ConnectionQuality(
    val label: String,
    val description: String,
    val color: Color,
    val icon: ImageVector
)

/** Klasifikasi kecepatan koneksi berdasarkan hasil download, mengikuti kebiasaan umum ISP. */
private fun resolveConnectionQuality(downloadMbps: Double): ConnectionQuality = when {
    downloadMbps < 4 -> ConnectionQuality(
        label = "Lambat",
        description = "Cukup untuk browsing dan chat ringan",
        color = Color(0xFFEF4444),
        icon = Icons.AutoMirrored.Filled.TrendingDown
    )
    downloadMbps < 20 -> ConnectionQuality(
        label = "Sedang",
        description = "Cocok untuk streaming SD/HD dan video call",
        color = Color(0xFFF59E0B),
        icon = Icons.AutoMirrored.Filled.TrendingFlat
    )
    downloadMbps < 75 -> ConnectionQuality(
        label = "Cepat",
        description = "Cocok untuk streaming 4K dan gaming online",
        color = Color(0xFF22C55E),
        icon = Icons.AutoMirrored.Filled.TrendingUp
    )
    else -> ConnectionQuality(
        label = "Sangat Cepat",
        description = "Ideal untuk banyak perangkat sekaligus & gaming kompetitif",
        color = Color(0xFF6366F1),
        icon = Icons.Default.Bolt
    )
}

/** Catatan tambahan soal latency, hanya ditampilkan jika cukup relevan untuk disebut. */
private fun resolveLatencyNote(pingMs: Double): String? = when {
    pingMs < 0 -> null
    pingMs <= 50 -> "Ping rendah, cocok untuk gaming online"
    pingMs <= 100 -> null
    else -> "Ping cukup tinggi, mungkin terasa delay saat gaming/video call"
}

@Composable
private fun ActionButton(
    phase: TestPhase,
    onStartTest: () -> Unit,
    onResetTest: () -> Unit,
    onCancelTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTesting = phase != TestPhase.IDLE && phase != TestPhase.FINISHED
    val isFinished = phase == TestPhase.FINISHED

    Button(
        onClick = {
            when {
                isTesting -> onCancelTest()
                isFinished -> onResetTest()
                else -> onStartTest()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = if (isTesting) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    ) {
        if (isTesting) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "  Batalkan Test",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            Text(
                text = if (isFinished) "Test Ulang" else "Mulai Test",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun ServerSelectionSection(
    selectedServer: ServerInfo?,
    enabled: Boolean,
    onChangeServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Server",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = selectedServer?.let { "${it.name} · ${it.location}" } ?: "Pilih server",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        TextButton(onClick = onChangeServerClick, enabled = enabled) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Ganti Server")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerSelectionSheet(
    servers: List<ServerInfo>,
    selectedServer: ServerInfo?,
    onServerSelected: (ServerInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Pilih Server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            servers.forEach { server ->
                ServerListItem(
                    server = server,
                    selected = server == selectedServer,
                    onClick = { onServerSelected(server) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ServerListItem(
    server: ServerInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = server.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Server terpilih",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ╔═══════════════════════════════════════════════════════════╗
// ║  Helper: Resolusi nilai berdasarkan fase                  ║
// ╚═══════════════════════════════════════════════════════════╝

/** Menentukan angka kecepatan yang ditampilkan di gauge */
private fun resolveGaugeSpeed(state: SpeedTestUiState): Float {
    return when (state.phase) {
        TestPhase.IDLE -> 0f
        TestPhase.TESTING_PING -> state.currentSpeed.toFloat()
        TestPhase.TESTING_JITTER -> state.currentSpeed.toFloat()
        TestPhase.TESTING_DOWNLOAD -> state.currentSpeed.toFloat()
        TestPhase.TESTING_UPLOAD -> state.currentSpeed.toFloat()
        TestPhase.FINISHED -> 0f
    }
}

/** Max speed berbeda untuk ping/jitter (ms) vs download/upload (Mbps) */
private fun resolveMaxSpeed(state: SpeedTestUiState): Float {
    return when (state.phase) {
        TestPhase.TESTING_PING -> 200f    // Max ping 200ms
        TestPhase.TESTING_JITTER -> 50f   // Max jitter 50ms
        else -> 150f                       // Max speed 150 Mbps
    }
}

/** Label fase untuk ditampilkan di bawah angka gauge */
private fun resolvePhaseLabel(state: SpeedTestUiState): String {
    return when (state.phase) {
        TestPhase.IDLE -> ""
        TestPhase.TESTING_PING -> "Ping"
        TestPhase.TESTING_JITTER -> "Jitter"
        TestPhase.TESTING_DOWNLOAD -> "Download"
        TestPhase.TESTING_UPLOAD -> "Upload"
        TestPhase.FINISHED -> ""
    }
}

/** Unit label untuk gauge */
private fun resolveUnitLabel(state: SpeedTestUiState): String {
    return when (state.phase) {
        TestPhase.TESTING_PING -> "ms"
        TestPhase.TESTING_JITTER -> "ms"
        else -> "Mbps"
    }
}

/** Warna aksen sesuai fase aktif */
@Composable
private fun resolvePhaseColor(state: SpeedTestUiState) = when (state.phase) {
    TestPhase.TESTING_PING -> PingColor
    TestPhase.TESTING_JITTER -> JitterColor
    TestPhase.TESTING_DOWNLOAD -> DownloadColor
    TestPhase.TESTING_UPLOAD -> UploadColor
    else -> MaterialTheme.colorScheme.primary
}

// ── Previews ─────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SpeedTestScreenPreviewDark() {
    SpeedTestTheme(darkTheme = true) {
        SpeedTestContent(
            uiState = SpeedTestUiState(
                phase = TestPhase.TESTING_DOWNLOAD,
                pingResult = 12.5,
                currentSpeed = 45.8,
                downloadSpeed = 0.0,
                uploadSpeed = 0.0,
                progress = 0.45f,
                selectedServer = SpeedTestViewModel.AVAILABLE_SERVERS.first()
            ),
            availableServers = SpeedTestViewModel.AVAILABLE_SERVERS,
            onStartTest = {},
            onResetTest = {},
            onCancelTest = {},
            onServerSelected = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SpeedTestScreenPreviewFinished() {
    SpeedTestTheme(darkTheme = false) {
        SpeedTestContent(
            uiState = SpeedTestUiState(
                phase = TestPhase.FINISHED,
                pingResult = 12.5,
                downloadSpeed = 45.8,
                uploadSpeed = 18.3,
                progress = 1f,
                selectedServer = SpeedTestViewModel.AVAILABLE_SERVERS.first()
            ),
            availableServers = SpeedTestViewModel.AVAILABLE_SERVERS,
            onStartTest = {},
            onResetTest = {},
            onCancelTest = {},
            onServerSelected = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SpeedTestScreenPreviewIdle() {
    SpeedTestTheme(darkTheme = true) {
        SpeedTestContent(
            uiState = SpeedTestUiState(
                selectedServer = SpeedTestViewModel.AVAILABLE_SERVERS.first()
            ),
            availableServers = SpeedTestViewModel.AVAILABLE_SERVERS,
            onStartTest = {},
            onResetTest = {},
            onCancelTest = {},
            onServerSelected = {}
        )
    }
}
