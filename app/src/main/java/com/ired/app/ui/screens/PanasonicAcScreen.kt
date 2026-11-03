package com.ired.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ired.app.model.UserRemote
import com.ired.app.model.PanasonicMode
import com.ired.app.model.PanasonicFan
import com.ired.app.model.PanasonicSwing
import com.ired.app.ui.components.AddCustomButtonDialog
import com.ired.app.ui.components.RoundIconButton
import com.ired.app.ui.theme.AccentBlue
import com.ired.app.ui.theme.AccentGreen
import com.ired.app.ui.theme.AcLcdBackground
import com.ired.app.ui.theme.AcLcdBorder
import com.ired.app.ui.theme.AcLcdText
import com.ired.app.ui.theme.AcPowerOff
import com.ired.app.ui.theme.AcPowerOn
import com.ired.app.ui.theme.BorderColor
import com.ired.app.ui.theme.CardBackground
import com.ired.app.ui.theme.DarkSurfaceVariant
import com.ired.app.ui.theme.Shape10
import com.ired.app.ui.theme.Shape12
import com.ired.app.ui.theme.Shape20
import com.ired.app.ui.theme.Shape6
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary
import com.ired.app.ui.viewmodel.MainViewModel

@Composable
fun PanasonicAcScreen(
    remote: UserRemote,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val acState by viewModel.acState.collectAsState()
    val scrollState = rememberScrollState()
    var showAddBtnDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Digital AC Inverter LCD Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shape20)
                .background(AcLcdBackground)
                .border(1.5.dp, AcLcdBorder, Shape20)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "❄️ PANASONIC INVERTER (36.7kHz)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcLcdText
                    )

                    Box(
                        modifier = Modifier
                            .clip(Shape6)
                            .background(if (acState.powerOn) AccentGreen.copy(alpha = 0.2f) else DarkSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (acState.powerOn) "ON" else "OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (acState.powerOn) AccentGreen else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Temperature reading with + / - steppers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RoundIconButton(
                        icon = Icons.Default.Remove,
                        contentDescription = "Decrease Temp",
                        onClick = { viewModel.setAcTemp(acState.temperature - 1) },
                        size = 48.dp
                    )

                    Spacer(modifier = Modifier.size(24.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "${acState.temperature}",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (acState.powerOn) TextPrimary else TextMuted
                        )
                        Text(
                            text = "°C",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcLcdText,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.size(24.dp))

                    RoundIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Increase Temp",
                        onClick = { viewModel.setAcTemp(acState.temperature + 1) },
                        size = 48.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${acState.mode.label.uppercase()} | ${if (acState.mode == PanasonicMode.FAN) "—" else "${acState.temperature}°C"} | ${acState.fan.label} Fan | ${acState.swing.label} Swing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AcLcdText
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        AcOptionRow(title = "MODE", options = PanasonicMode.entries.map { it.label }, selected = acState.mode.label) { label ->
            viewModel.updateAcMode(PanasonicMode.entries.first { it.label == label })
        }
        Spacer(modifier = Modifier.height(10.dp))
        AcOptionRow(title = "FAN", options = PanasonicFan.entries.map { it.label }, selected = acState.fan.label) { label ->
            viewModel.updateAcFan(PanasonicFan.entries.first { it.label == label })
        }
        Spacer(modifier = Modifier.height(10.dp))
        AcOptionRow(title = "SWING", options = PanasonicSwing.entries.map { it.label }, selected = acState.swing.label) { label ->
            viewModel.updateAcSwing(PanasonicSwing.entries.first { it.label == label })
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Power Controls (AC ON / AC OFF) with tactile press feedback
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AcActionButton(
                label = "AC ON",
                icon = Icons.Default.PowerSettingsNew,
                backgroundColor = AcPowerOn,
                onClick = { viewModel.setAcDirect(acState.temperature, powerOn = true) },
                modifier = Modifier.weight(1f)
            )

            AcActionButton(
                label = "AC OFF",
                icon = Icons.Default.PowerSettingsNew,
                backgroundColor = AcPowerOff,
                onClick = { viewModel.setAcDirect(acState.temperature, powerOn = false) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Shortcut Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AcPresetButton(
                label = "24°C Eco",
                onClick = { viewModel.setAcDirect(24, powerOn = true) },
                modifier = Modifier.weight(1f)
            )

            AcPresetButton(
                label = "18°C Jet",
                onClick = { viewModel.setAcDirect(18, powerOn = true) },
                modifier = Modifier.weight(1f)
            )
        }

        // Custom Buttons
        if (remote.customButtons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "CUSTOM BUTTONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))

            remote.customButtons.chunked(3).forEach { rowBtns ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowBtns.forEach { btn ->
                        key(btn.id) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(Shape10)
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, BorderColor, Shape10)
                                    .clickable { viewModel.sendNecCode(btn.hexCode, btn.label) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = btn.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Add Custom Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(Shape12)
                .background(CardBackground)
                .border(1.dp, BorderColor, Shape12)
                .clickable { showAddBtnDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = "+ Add Custom Button", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showAddBtnDialog) {
        AddCustomButtonDialog(
            remoteId = remote.id,
            onDismiss = { showAddBtnDialog = false },
            onAdd = { btn ->
                viewModel.addCustomButton(remote.id, btn)
                showAddBtnDialog = false
            }
        )
    }
}

@Composable
private fun AcActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .clip(Shape12)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AcPresetButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "preset_scale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .clip(Shape12)
            .background(AcLcdBackground)
            .border(1.dp, AcLcdBorder, Shape12)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = AcLcdText.copy(alpha = 0.3f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = AcLcdText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun AcOptionRow(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val active = option == selected
                Box(Modifier.weight(1f).height(44.dp).clip(Shape10)
                    .background(if (active) AccentBlue else DarkSurfaceVariant)
                    .border(1.dp, if (active) AccentBlue else BorderColor, Shape10)
                    .clickable { onSelect(option) }, contentAlignment = Alignment.Center) {
                    Text(option, color = if (active) Color.White else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
