package com.covildev.pulso.feature_alarm.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.covildev.pulso.feature_alarm.model.AlarmPayload
import com.covildev.pulso.ui.theme.PrimaryGreen
import com.covildev.pulso.ui.theme.PureWhite
import com.covildev.pulso.ui.theme.SecondaryBlue

@Composable
fun AlarmAlertScreen(
    alarmPayload: AlarmPayload,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryGreen)
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
                    text = alarmPayload.formattedTime,
                    color = PureWhite,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = alarmPayload.dayLabel,
                    color = PureWhite.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = alarmPayload.title,
                    color = PureWhite,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )

                alarmPayload.normalizedNote?.let { note ->
                    Text(
                        text = note,
                        color = PureWhite.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(1.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryBlue,
                        contentColor = PureWhite,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    onClick = onDismiss,
                ) {
                    Text(
                        text = "Entendido",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
