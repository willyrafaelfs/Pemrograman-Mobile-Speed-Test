package com.example.speedtest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.speedtest.ui.theme.DownloadColor
import com.example.speedtest.ui.theme.PingColor
import com.example.speedtest.ui.theme.SpeedTestTheme
import com.example.speedtest.ui.theme.UploadColor

/**
 * ═══════════════════════════════════════════════════════════════
 *  ResultCard.kt — Kartu hasil pengujian per parameter
 *
 *  Menampilkan:
 *  - Icon + label (Ping / Download / Upload)
 *  - Nilai hasil tes (angka besar)
 *  - Unit (ms / Mbps)
 *
 *  Setiap kartu memiliki warna aksen yang berbeda untuk
 *  memudahkan identifikasi visual:
 *  - Ping: Orange 🟠
 *  - Download: Cyan 🔵
 *  - Upload: Purple 🟣
 * ═══════════════════════════════════════════════════════════════
 */

@Composable
fun ResultCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon + Label (horizontal)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nilai hasil tes
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                textAlign = TextAlign.Center
            )

            // Unit
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Row berisi 3 ResultCard untuk Ping, Download, Upload.
 * Digunakan setelah semua tes selesai.
 */
@Composable
fun ResultCardsRow(
    pingMs: Double,
    jitterMs: Double,
    downloadMbps: Double,
    uploadMbps: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultCard(
                label = "PING",
                value = if (pingMs < 0) "—" else String.format("%.0f", pingMs),
                unit = "ms",
                icon = Icons.Rounded.NetworkPing,
                accentColor = PingColor,
                modifier = Modifier.weight(1f)
            )
            ResultCard(
                label = "JITTER",
                value = if (jitterMs < 0) "—" else String.format("%.0f", jitterMs),
                unit = "ms",
                icon = Icons.Rounded.SyncAlt,
                accentColor = PingColor.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultCard(
                label = "DOWNLOAD",
                value = if (downloadMbps < 0) "—" else String.format("%.1f", downloadMbps),
                unit = "Mbps",
                icon = Icons.Rounded.ArrowDownward,
                accentColor = DownloadColor,
                modifier = Modifier.weight(1f)
            )
            ResultCard(
                label = "UPLOAD",
                value = if (uploadMbps < 0) "—" else String.format("%.1f", uploadMbps),
                unit = "Mbps",
                icon = Icons.Rounded.ArrowUpward,
                accentColor = UploadColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun ResultCardsPreviewDark() {
    SpeedTestTheme(darkTheme = true) {
        ResultCardsRow(
            pingMs = 12.5,
            jitterMs = 2.4,
            downloadMbps = 45.8,
            uploadMbps = 18.3,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultCardsPreviewLight() {
    SpeedTestTheme(darkTheme = false) {
        ResultCardsRow(
            pingMs = 8.0,
            jitterMs = 1.2,
            downloadMbps = 92.4,
            uploadMbps = 35.7,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SingleCardPreview() {
    SpeedTestTheme(darkTheme = true) {
        ResultCard(
            label = "DOWNLOAD",
            value = "45.8",
            unit = "Mbps",
            icon = Icons.Rounded.ArrowDownward,
            accentColor = DownloadColor,
            modifier = Modifier
                .width(120.dp)
                .padding(8.dp)
        )
    }
}
