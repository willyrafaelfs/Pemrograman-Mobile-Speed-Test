package com.example.speedtest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Komponen grafik garis kustom untuk menampilkan tren kecepatan.
 */
@Composable
fun TrendChart(
    data: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val spacing = size.width / (data.size - 1)
        val maxData = data.maxOrNull() ?: 1.0
        val minData = data.minOrNull() ?: 0.0
        val range = (maxData - minData).coerceAtLeast(1.0)
        
        // Helper to get Y coordinate (inverted because 0 is top)
        fun getY(value: Double): Float {
            val normalized = (value - minData) / range
            return (size.height - (normalized * size.height)).toFloat()
        }

        // Draw Grid Lines (Horizontal)
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

        // Create Path for the Line
        val path = Path().apply {
            moveTo(0f, getY(data[0]))
            for (i in 1 until data.size) {
                lineTo(i * spacing, getY(data[i]))
            }
        }

        // Create Path for the Gradient Fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        // Draw Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )

        // Draw the Line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Data Points
        data.forEachIndexed { i, value ->
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(i * spacing, getY(value))
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(i * spacing, getY(value))
            )
        }
    }
}
