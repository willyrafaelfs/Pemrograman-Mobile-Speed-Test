package com.example.speedtest.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Utilitas untuk membagikan teks atau file ke aplikasi lain.
 */
object ShareUtils {

    /**
     * Membagikan teks sederhana (Ringkasan hasil tes).
     */
    fun shareText(context: Context, text: String, title: String = "Bagikan Hasil Tes") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    /**
     * Membagikan file CSV hasil ekspor histori.
     */
    fun shareCsvFile(context: Context, csvContent: String, fileName: String = "speedtest_history.csv") {
        try {
            // Simpan ke file sementara di cache internal
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { 
                it.write(csvContent.toByteArray())
            }

            // Dapatkan URI menggunakan FileProvider (keamanan Android)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Ekspor Histori CSV"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
