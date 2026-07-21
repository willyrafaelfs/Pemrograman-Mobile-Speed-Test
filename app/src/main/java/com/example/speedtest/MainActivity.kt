package com.example.speedtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.speedtest.data.local.AppDatabase
import com.example.speedtest.ui.screen.SpeedTestScreen
import com.example.speedtest.ui.theme.SpeedTestTheme
import com.example.speedtest.viewmodel.SpeedTestViewModel

/**
 * ═══════════════════════════════════════════════════════════════
 *  MainActivity.kt — Entry point aplikasi Speed Test
 *
 *  Menggunakan Jetpack Compose sebagai UI framework.
 *  enableEdgeToEdge() mengaktifkan tampilan edge-to-edge
 *  untuk pengalaman visual yang lebih immersive.
 * ═══════════════════════════════════════════════════════════════
 *  Author: Kukuh Yudhistiro, S.Kom., M.Kom.
 * ═══════════════════════════════════════════════════════════════
 */
class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: SpeedTestViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SpeedTestViewModel(application, db) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeedTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpeedTestScreen(viewModel = viewModel)
                }
            }
        }
    }
}
