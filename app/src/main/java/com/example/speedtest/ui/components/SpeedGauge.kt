package com.example.speedtest.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.speedtest.ui.theme.GaugeHigh
import com.example.speedtest.ui.theme.GaugeLow
import com.example.speedtest.ui.theme.GaugeMax
import com.example.speedtest.ui.theme.GaugeMedium
import com.example.speedtest.ui.theme.SpeedTestTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * ═══════════════════════════════════════════════════════════════
 *  SpeedGauge.kt — Custom Circular Speedometer Gauge
 *
 *  Menampilkan kecepatan saat ini dalam bentuk gauge melingkar
 *  (setengah lingkaran atas) menggunakan Canvas Compose.
 *
 *  Komponen utama:
 *  1. Background arc (track abu-abu)
 *  2. Progress arc (gradient warna berdasarkan kecepatan)
 *  3. Tick marks (penanda skala kecepatan)
 *  4. Needle indicator (jarum penunjuk)
 *  5. Center text (angka kecepatan + unit)
 *
 *  Geometri gauge:
 *  ┌──────────────────────────────────┐
 *  │        startAngle = 135°          │
 *  │       ╱                   ╲       │
 *  │     ╱   sweepAngle = 270°   ╲     │
 *  │   ╱          ┃               ╲    │
 *  │  │     42.5 Mbps              │   │
 *  │  │     Download               │   │
 *  │   ╲                          ╱    │
 *  │     ╲                      ╱      │
 *  │       endAngle = 405° (45°)       │
 *  └──────────────────────────────────┘
 *
 *  Arah sudut Canvas Android: 0° = kanan, searah jarum jam
 *  Start: 135° (kiri bawah)  →  End: 405° / 45° (kanan bawah)
 * ═══════════════════════════════════════════════════════════════
 */

@Composable
fun SpeedGauge(
    currentSpeed: Float,
    maxSpeed: Float = 100f,
    phaseLabel: String = "",
    unitLabel: String = "Mbps",
    gaugeSize: Dp = 260.dp,
    modifier: Modifier = Modifier
) {
    // Animasi smooth untuk pergerakan gauge
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.coerceIn(0f, maxSpeed),
        animationSpec = tween(durationMillis = 600),
        label = "speed_animation"
    )

    // Fraksi progress (0f..1f)
    val fraction = (animatedSpeed / maxSpeed).coerceIn(0f, 1f)

    // Warna gauge berdasarkan kecepatan
    val gaugeColor = lerpGaugeColor(fraction)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(gaugeSize)
    ) {
        // ── Canvas: Arc, Ticks, Needle ──────────────────────
        Canvas(modifier = Modifier.size(gaugeSize)) {
            val strokeWidth = 16.dp.toPx()
            val thinStroke = 3.dp.toPx()
            val padding = strokeWidth + 8.dp.toPx()

            val arcSize = Size(
                width = size.width - padding * 2,
                height = size.height - padding * 2
            )
            val arcTopLeft = Offset(padding, padding)

            // ── 1. Background Track ─────────────────────────
            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // ── 2. Progress Arc (gradient) ──────────────────
            if (fraction > 0.005f) {
                val progressSweep = fraction * SWEEP_ANGLE
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(GaugeLow, GaugeMedium, GaugeHigh, GaugeMax),
                        center = Offset(size.width / 2, size.height / 2)
                    ),
                    startAngle = START_ANGLE,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // ── 3. Tick Marks ───────────────────────────────
            drawTickMarks(
                center = Offset(size.width / 2, size.height / 2),
                radius = arcSize.width / 2 + 4.dp.toPx(),
                tickColor = tickColor,
                maxSpeed = maxSpeed
            )

            // ── 4. Needle / Indicator ───────────────────────
            drawNeedle(
                center = Offset(size.width / 2, size.height / 2),
                radius = arcSize.width / 2 - 12.dp.toPx(),
                fraction = fraction,
                color = gaugeColor
            )
        }

        // ── Teks Tengah: Angka + Unit + Label ───────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Angka kecepatan
            Text(
                text = if (currentSpeed < 0.1f && phaseLabel.isEmpty()) "0"
                else String.format("%.1f", animatedSpeed),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            // Unit (Mbps / ms)
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            // Label fase (Download / Upload / Ping)
            if (phaseLabel.isNotEmpty()) {
                Text(
                    text = phaseLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = gaugeColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Constants ────────────────────────────────────────────────

// Start angle: 135° (kiri bawah dari pusat)
// Sweep angle: 270° (berputar searah jarum jam sampai 405° = 45°)
private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f

// ── Helper Functions ─────────────────────────────────────────

/**
 * Interpolasi warna gauge berdasarkan fraksi kecepatan.
 * 0.0 = Cyan (rendah)
 * 0.5 = Green → Amber (sedang-tinggi)
 * 1.0 = Red (maksimum)
 */
private fun lerpGaugeColor(fraction: Float): Color {
    return when {
        fraction < 0.33f -> lerp(GaugeLow, GaugeMedium, fraction / 0.33f)
        fraction < 0.66f -> lerp(GaugeMedium, GaugeHigh, (fraction - 0.33f) / 0.33f)
        else -> lerp(GaugeHigh, GaugeMax, (fraction - 0.66f) / 0.34f)
    }
}

/** Interpolasi linear antara dua warna */
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = 1f
    )
}

/**
 * Menggambar tick marks (garis-garis skala) di sekeliling arc.
 * Tick besar setiap kelipatan 20, tick kecil setiap kelipatan 10.
 */
private fun DrawScope.drawTickMarks(
    center: Offset,
    radius: Float,
    tickColor: Color,
    maxSpeed: Float
) {
    val tickCount = 10  // Jumlah segmen utama
    val anglePerTick = SWEEP_ANGLE / tickCount

    for (i in 0..tickCount) {
        val angle = START_ANGLE + i * anglePerTick
        val angleRad = Math.toRadians(angle.toDouble())

        val isMajor = i % 2 == 0
        val tickLength = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
        val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

        val outerX = center.x + (radius + 2.dp.toPx()) * cos(angleRad).toFloat()
        val outerY = center.y + (radius + 2.dp.toPx()) * sin(angleRad).toFloat()
        val innerX = center.x + (radius - tickLength) * cos(angleRad).toFloat()
        val innerY = center.y + (radius - tickLength) * sin(angleRad).toFloat()

        drawLine(
            color = tickColor,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Menggambar needle (jarum penunjuk) pada posisi fraksi tertentu.
 * Jarum berupa garis dari pusat ke tepi arc.
 */
private fun DrawScope.drawNeedle(
    center: Offset,
    radius: Float,
    fraction: Float,
    color: Color
) {
    val needleAngle = START_ANGLE + fraction * SWEEP_ANGLE
    val angleRad = Math.toRadians(needleAngle.toDouble())

    // Titik ujung needle
    val needleX = center.x + radius * cos(angleRad).toFloat()
    val needleY = center.y + radius * sin(angleRad).toFloat()

    // Gambar garis needle
    drawLine(
        color = color,
        start = center,
        end = Offset(needleX, needleY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Lingkaran kecil di pusat needle
    drawCircle(
        color = color,
        radius = 8.dp.toPx(),
        center = center
    )

    // Lingkaran kecil di ujung needle
    drawCircle(
        color = color,
        radius = 5.dp.toPx(),
        center = Offset(needleX, needleY)
    )
}

// ── Previews ─────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SpeedGaugePreviewDark() {
    SpeedTestTheme(darkTheme = true) {
        SpeedGauge(
            currentSpeed = 42.5f,
            phaseLabel = "Download",
            unitLabel = "Mbps"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpeedGaugePreviewLight() {
    SpeedTestTheme(darkTheme = false) {
        SpeedGauge(
            currentSpeed = 75f,
            phaseLabel = "Upload",
            unitLabel = "Mbps"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SpeedGaugePreviewIdle() {
    SpeedTestTheme(darkTheme = true) {
        SpeedGauge(
            currentSpeed = 0f,
            phaseLabel = "",
            unitLabel = "Mbps"
        )
    }
}
