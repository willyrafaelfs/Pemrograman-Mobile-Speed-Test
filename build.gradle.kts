// ═══════════════════════════════════════════════════════════════
// Project-level build.gradle.kts
// Mendefinisikan plugin yang digunakan oleh seluruh module.
// ═══════════════════════════════════════════════════════════════
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
