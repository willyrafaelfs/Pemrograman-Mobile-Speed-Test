package com.example.speedtest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.speedtest.data.local.entity.SpeedTestResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Satu titik data pada grafik, dikaitkan dengan hasil riwayat tes aslinya
 * agar saat titik ditekan, info tes yang bersangkutan bisa ditampilkan.
 */
data class ChartPoint(
    val value: Double,
    val result: SpeedTestResult
)

/**
 * Komponen grafik garis kustom untuk menampilkan tren kecepatan.
 * Titik-titik pada grafik bisa ditekan untuk menampilkan info
 * riwayat tes (tanggal & seluruh metrik) dari titik tersebut.
 */
@Composable
fun TrendChart(
    points: List<ChartPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    Column {
        Box(modifier = modifier) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            if (points.size < 2) return@detectTapGestures
                            val spacing = size.width / (points.size - 1).toFloat()
                            val index = (tapOffset.x / spacing).roundToInt()
                                .coerceIn(0, points.size - 1)
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    }
            ) {
                if (points.size < 2) return@Canvas

                val values = points.map { it.value }
                val spacing = size.width / (points.size - 1)
                val maxData = values.maxOrNull() ?: 1.0
                val minData = values.minOrNull() ?: 0.0
                val range = (maxData - minData).coerceAtLeast(1.0)

                fun getY(value: Double): Float {
                    val normalized = (value - minData) / range
                    return (size.height - (normalized * size.height)).toFloat()
                }

                // Grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = (size.height / gridLines) * i
                    drawLine(
                        color = onSurface,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val path = Path().apply {
                    moveTo(0f, getY(values[0]))
                    for (i in 1 until values.size) {
                        lineTo(i * spacing, getY(values[i]))
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                        startY = 0f,
                        endY = size.height
                    )
                )

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                values.forEachIndexed { i, value ->
                    val isSelected = i == selectedIndex
                    val center = Offset(i * spacing, getY(value))

                    if (isSelected) {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.3f),
                            radius = 9.dp.toPx(),
                            center = center
                        )
                    }
                    drawCircle(
                        color = lineColor,
                        radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 2.5.dp.toPx() else 2.dp.toPx(),
                        center = center
                    )
                }
            }
        }

        // ── Info panel: detail riwayat tes dari titik yang dipilih ──
        val selected = selectedIndex?.let { points.getOrNull(it) }
        if (selected != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectedPointInfo(point = selected, accentColor = lineColor)
        }
    }
}

@Composable
private fun SelectedPointInfo(point: ChartPoint, accentColor: Color) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val result = point.result

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = sdf.format(Date(result.timestamp)),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoMetric(label = "PING", value = "${result.ping.toInt()} ms")
                InfoMetric(label = "JITTER", value = "${result.jitter.toInt()} ms")
                InfoMetric(label = "DOWN", value = String.format("%.1f", result.download))
                InfoMetric(label = "UP", value = String.format("%.1f", result.upload))
            }
        }
    }
}

@Composable
private fun InfoMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
