package com.example.speedtest.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ═══════════════════════════════════════════════════════════════
 *  Color.kt — Palet warna untuk Speed Test App
 *  Menggunakan skema "tech-forward" yang cocok untuk
 *  aplikasi pengukuran jaringan.
 * ═══════════════════════════════════════════════════════════════
 */

// ── Primary: Cyan/Blue ───────────────────────────────────────
val CyanPrimary = Color(0xFF00B4D8)
val CyanPrimaryDark = Color(0xFF0096C7)
val CyanLight = Color(0xFF90E0EF)

// ── Secondary: Indigo ────────────────────────────────────────
val IndigoPrimary = Color(0xFF6366F1)
val IndigoLight = Color(0xFF818CF8)
val IndigoDark = Color(0xFF4F46E5)

// ── Gauge Gradient Colors ────────────────────────────────────
val GaugeLow = Color(0xFF06B6D4)       // Cyan — kecepatan rendah
val GaugeMedium = Color(0xFF22C55E)    // Green — kecepatan sedang
val GaugeHigh = Color(0xFFF59E0B)      // Amber — kecepatan tinggi
val GaugeMax = Color(0xFFEF4444)       // Red — kecepatan sangat tinggi

// ── Background & Surface ─────────────────────────────────────
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)

val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)

// ── Text ─────────────────────────────────────────────────────
val TextOnDark = Color(0xFFF1F5F9)
val TextOnDarkSecondary = Color(0xFF94A3B8)
val TextOnLight = Color(0xFF0F172A)
val TextOnLightSecondary = Color(0xFF64748B)

// ── Status Colors ────────────────────────────────────────────
val PingColor = Color(0xFFF97316)      // Orange untuk Ping
val DownloadColor = Color(0xFF06B6D4)  // Cyan untuk Download
val UploadColor = Color(0xFF8B5CF6)    // Purple untuk Upload
