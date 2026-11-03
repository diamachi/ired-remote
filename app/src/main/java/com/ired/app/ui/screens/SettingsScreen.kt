package com.ired.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ired.app.ui.theme.AccentBlue
import com.ired.app.ui.theme.AccentGreen
import com.ired.app.ui.theme.AccentRed
import com.ired.app.ui.theme.BorderColor
import com.ired.app.ui.theme.CardBackground
import com.ired.app.ui.theme.Shape16
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary
import com.ired.app.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val hasIrHardware = viewModel.isIrHardwareAvailable
    val irRanges = viewModel.irCarrierRanges

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "HARDWARE IR BLASTER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shape16)
                .background(CardBackground)
                .border(1.dp, BorderColor, Shape16)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Oppo Built-in IR Emitter", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasIrHardware) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasIrHardware) AccentGreen else AccentRed
                        )
                        Text(
                            text = if (hasIrHardware) "Ready" else "Not Detected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasIrHardware) AccentGreen else AccentRed,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                if (irRanges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Carrier Frequencies: ${irRanges.joinToString { "${it.first / 1000}kHz - ${it.second / 1000}kHz" }}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.testIrHardware() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = null)
                    Text(text = "Fire Test IR Signal (38kHz)", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hardware Tips
        Text(
            text = "LINE OF SIGHT TIPS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shape16)
                .background(CardBackground)
                .border(1.dp, BorderColor, Shape16)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.padding(top = 2.dp))
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(text = "Cherry Blossom Lights", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        Text(
                            text = "The IR receiver is inside the translucent USB power plug or adapter box (not in the branches). Point the top of your phone directly at the USB plug within 2–3 meters.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.padding(top = 2.dp))
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(text = "Google TV Streamer (HDMI-CEC)", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        Text(
                            text = "Make sure Simplink (HDMI-CEC) is enabled on your LG TV (Settings > General > Devices > HDMI Settings > SIMPLINK). Blasting the LG TV IR codes forwards D-Pad, OK, and Back straight to your Google TV Streamer.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
