package com.example.speedtest.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.speedtest.MainActivity
import com.example.speedtest.data.local.AppDatabase
import com.example.speedtest.data.local.entity.SpeedTestResult

class SpeedTestWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val latestResult = db.speedTestDao().getLatestResult()

        provideContent {
            GlanceTheme {
                WidgetContent(latestResult)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(result: SpeedTestResult?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speed Test",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.primary
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (result != null) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        Text(text = "Down", style = TextStyle(fontSize = 10.sp))
                        Text(
                            text = String.format("%.1f", result.download),
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        Text(text = "Up", style = TextStyle(fontSize = 10.sp))
                        Text(
                            text = String.format("%.1f", result.upload),
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        )
                    }
                }
            } else {
                Text(text = "Belum ada tes", style = TextStyle(fontSize = 12.sp))
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            val startTestParam = ActionParameters.Key<Boolean>("EXTRA_START_TEST")
            Button(
                text = "Mulai Tes",
                onClick = actionStartActivity<MainActivity>(
                    actionParametersOf(startTestParam to true)
                )
            )
        }
    }
}
