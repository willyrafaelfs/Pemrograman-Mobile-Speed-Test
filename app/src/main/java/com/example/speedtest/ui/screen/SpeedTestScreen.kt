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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedtest.model.SpeedTestUiState
import com.example.speedtest.model.TestPhase
import com.example.speedtest.ui.components.ResultCardsRow
import com.example.speedtest.ui.components.SpeedGauge
import com.example.speedtest.ui.theme.DownloadColor
import com.example.speedtest.ui.theme.PingColor
import com.example.speedtest.ui.theme.SpeedTestTheme
import com.example.speedtest.ui.theme.UploadColor
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
                onStartTest = { viewModel.startSpeedTest() },
                onResetTest = { viewModel.resetTest() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun SpeedTestContent(
    uiState: SpeedTestUiState,
    onStartTest: () -> Unit,
    onResetTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTesting = uiState.phase != TestPhase.IDLE &&
            uiState.phase != TestPhase.FINISHED

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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
                downloadMbps = uiState.downloadSpeed,
                uploadMbps = uiState.uploadSpeed,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Action Button ────────────────────────────────────
        ActionButton(
            phase = uiState.phase,
            onStartTest = onStartTest,
            onResetTest = onResetTest,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

// ╔═══════════════════════════════════════════════════════════╗
// ║  Sub-Composables                                          ║
// ╚═══════════════════════════════════════════════════════════╝

@Composable
private fun StatusText(uiState: SpeedTestUiState) {
    val (text, pulsate) = when (uiState.phase) {
        TestPhase.IDLE -> "Siap mengukur koneksi internet" to false
        TestPhase.TESTING_PING -> "Mengukur Ping ke google.com..." to true
        TestPhase.TESTING_DOWNLOAD -> "Mengukur Download Speed..." to true
        TestPhase.TESTING_UPLOAD -> "Mengukur Upload Speed..." to true
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
private fun ActionButton(
    phase: TestPhase,
    onStartTest: () -> Unit,
    onResetTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTesting = phase != TestPhase.IDLE && phase != TestPhase.FINISHED
    val isFinished = phase == TestPhase.FINISHED

    Button(
        onClick = {
            if (isFinished) onResetTest()
            else onStartTest()
        },
        enabled = !isTesting,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (isTesting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                strokeWidth = 2.5.dp
            )
            Text(
                text = "  Testing...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

// ╔═══════════════════════════════════════════════════════════╗
// ║  Helper: Resolusi nilai berdasarkan fase                  ║
// ╚═══════════════════════════════════════════════════════════╝

/** Menentukan angka kecepatan yang ditampilkan di gauge */
private fun resolveGaugeSpeed(state: SpeedTestUiState): Float {
    return when (state.phase) {
        TestPhase.IDLE -> 0f
        TestPhase.TESTING_PING -> state.currentSpeed.toFloat()
        TestPhase.TESTING_DOWNLOAD -> state.currentSpeed.toFloat()
        TestPhase.TESTING_UPLOAD -> state.currentSpeed.toFloat()
        TestPhase.FINISHED -> 0f
    }
}

/** Max speed berbeda untuk ping (ms) vs download/upload (Mbps) */
private fun resolveMaxSpeed(state: SpeedTestUiState): Float {
    return when (state.phase) {
        TestPhase.TESTING_PING -> 200f   // Max ping 200ms
        else -> 150f                      // Max speed 150 Mbps
    }
}

/** Label fase untuk ditampilkan di bawah angka gauge */
private fun resolvePhaseLabel(state: SpeedTestUiState): String {
    return when (state.phase) {
        TestPhase.IDLE -> ""
        TestPhase.TESTING_PING -> "Ping"
        TestPhase.TESTING_DOWNLOAD -> "Download"
        TestPhase.TESTING_UPLOAD -> "Upload"
        TestPhase.FINISHED -> ""
    }
}

/** Unit label untuk gauge */
private fun resolveUnitLabel(state: SpeedTestUiState): String {
    return when (state.phase) {
        TestPhase.TESTING_PING -> "ms"
        else -> "Mbps"
    }
}

/** Warna aksen sesuai fase aktif */
@Composable
private fun resolvePhaseColor(state: SpeedTestUiState) = when (state.phase) {
    TestPhase.TESTING_PING -> PingColor
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
                progress = 0.45f
            ),
            onStartTest = {},
            onResetTest = {}
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
                progress = 1f
            ),
            onStartTest = {},
            onResetTest = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SpeedTestScreenPreviewIdle() {
    SpeedTestTheme(darkTheme = true) {
        SpeedTestContent(
            uiState = SpeedTestUiState(),
            onStartTest = {},
            onResetTest = {}
        )
    }
}
