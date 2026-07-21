package com.example.speedtest.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.speedtest.data.local.entity.SpeedTestResult
import com.example.speedtest.ui.components.TrendChart
import com.example.speedtest.ui.theme.DownloadColor
import com.example.speedtest.ui.theme.PingColor
import com.example.speedtest.ui.theme.UploadColor
import com.example.speedtest.viewmodel.SpeedTestViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: SpeedTestViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.historyResults.collectAsState()
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Download

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Riwayat & Tren", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Semua")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (history.isEmpty()) {
                EmptyHistory()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TREND SECTION
                    item {
                        TrendSection(
                            history = history,
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    item {
                        Text(
                            "Log Aktivitas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // LIST SECTION
                    items(history) { result ->
                        HistoryItem(result)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistory() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Belum ada riwayat", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun TrendSection(
    history: List<SpeedTestResult>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val chartData = remember(history, selectedTab) {
        val reversed = history.reversed().takeLast(10) // Show last 10 tests
        when (selectedTab) {
            0 -> reversed.map { it.ping }
            1 -> reversed.map { it.download }
            else -> reversed.map { it.upload }
        }
    }

    val chartColor = when (selectedTab) {
        0 -> PingColor
        1 -> DownloadColor
        else -> UploadColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Tren Performa (10 Terakhir)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (history.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Butuh minimal 2 data untuk grafik",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                TrendChart(
                    data = chartData,
                    lineColor = chartColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = chartColor
                        )
                    }
                }
            ) {
                listOf("Ping", "Down", "Up").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTab == index) chartColor else MaterialTheme.colorScheme.outline
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(result: SpeedTestResult) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(result.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = dateString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultSmall(label = "PING", value = "${result.ping.toInt()} ms")
                ResultSmall(label = "JITTER", value = "${result.jitter.toInt()} ms")
                ResultSmall(label = "DOWN", value = String.format("%.1f Mbps", result.download))
                ResultSmall(label = "UP", value = String.format("%.1f Mbps", result.upload))
            }
        }
    }
}

@Composable
fun ResultSmall(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
