package com.covildev.pulso.feature_alarm.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun AlarmAlertScreen(
    hour: Int,
    minute: Int,
    dayLabel: String,
    note: String?,
    onDismiss: () -> Unit,
) {
    val shouldPlay = androidx.compose.runtime.remember { AtomicBoolean(true) }
    DisposableEffect(Unit) {
        onDispose { shouldPlay.set(false) }
    }

    LaunchedEffect(Unit) {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        try {
            while (shouldPlay.get()) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
                delay(1000)
            }
        } finally {
            toneGenerator.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF590000), Color(0xFF9C1010), Color(0xFF1C0000)),
                ),
            )
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = String.format(Locale.forLanguageTag("pt-BR"), "%02d:%02dh", hour, minute),
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = dayLabel,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Você deve medir a sua pressão!",
                    color = Color.White,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Observação:",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Text(
                        text = " ",
                        color = Color.White.copy(alpha = 0.35f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF8F0E0E),
                    ),
                    shape = RoundedCornerShape(16.dp),
                    onClick = onDismiss,
                ) {
                    Text(
                        text = "Dispensar alarme",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
