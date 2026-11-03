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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ired.app.ir.protocols.LgTvIrProtocol
import com.ired.app.model.UserRemote
import com.ired.app.ui.components.AddCustomButtonDialog
import com.ired.app.ui.components.PowerButton
import com.ired.app.ui.components.RockerSwitch
import com.ired.app.ui.components.RoundIconButton
import com.ired.app.ui.components.VirtualDpad
import com.ired.app.ui.theme.AccentBlue
import com.ired.app.ui.theme.BorderColor
import com.ired.app.ui.theme.CardBackground
import com.ired.app.ui.theme.DarkSurfaceVariant
import com.ired.app.ui.theme.Shape10
import com.ired.app.ui.theme.Shape12
import com.ired.app.ui.theme.Shape20
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary
import com.ired.app.ui.viewmodel.MainViewModel

@Composable
fun LgTvScreen(
    remote: UserRemote,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showAddBtnDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: TV Power, Input Source, Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PowerButton(
                isOn = true,
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_POWER, "TV Power") }
            )

            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.Input,
                contentDescription = "Input Source",
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_INPUT, "Input") }
            )

            RoundIconButton(
                icon = Icons.Default.Info,
                contentDescription = "Info",
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_INFO, "Info") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick webOS Toolbar: Home Dashboard, All Settings, Quick Menu
        Row(
            modifier = Modifier.fillMaxWidth(0.95f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LgQuickToolbarButton(
                label = "Home",
                icon = Icons.Default.Home,
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_HOME, "webOS Home") },
                modifier = Modifier.weight(1f)
            )

            LgQuickToolbarButton(
                label = "Settings",
                icon = Icons.Default.Settings,
                tint = AccentBlue,
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_SETTINGS, "LG Settings") },
                modifier = Modifier.weight(1f)
            )

            LgQuickToolbarButton(
                label = "Q.Menu",
                icon = Icons.Default.Tune,
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_Q_MENU, "Q.Menu") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Virtual D-Pad for LG webOS TV
        VirtualDpad(
            onUp = { viewModel.sendNecCode(LgTvIrProtocol.CMD_UP, "▲ Up") },
            onDown = { viewModel.sendNecCode(LgTvIrProtocol.CMD_DOWN, "▼ Down") },
            onLeft = { viewModel.sendNecCode(LgTvIrProtocol.CMD_LEFT, "◄ Left") },
            onRight = { viewModel.sendNecCode(LgTvIrProtocol.CMD_RIGHT, "► Right") },
            onCenter = { viewModel.sendNecCode(LgTvIrProtocol.CMD_OK, "● OK") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Row: Back, Mute, Exit
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_BACK, "Back") }
            )

            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.VolumeMute,
                contentDescription = "Mute",
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_MUTE, "Mute") }
            )

            RoundIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Exit",
                onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_EXIT, "Exit") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dual Rockers: VOL on Left, CH on Right, Play/Pause in Center
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Volume Rocker
            RockerSwitch(
                title = "VOL",
                onPlusClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_VOL_UP, "Vol +") },
                onMinusClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_VOL_DOWN, "Vol -") }
            )

            // Play & Pause
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RoundIconButton(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    size = 50.dp,
                    onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_PLAY, "Play") }
                )

                RoundIconButton(
                    icon = Icons.Default.Pause,
                    contentDescription = "Pause",
                    size = 50.dp,
                    onClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_PAUSE, "Pause") }
                )
            }

            // Channel Rocker
            RockerSwitch(
                title = "CH",
                plusIcon = Icons.Default.KeyboardArrowUp,
                minusIcon = Icons.Default.KeyboardArrowDown,
                onPlusClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_CH_UP, "CH +") },
                onMinusClick = { viewModel.sendNecCode(LgTvIrProtocol.CMD_CH_DOWN, "CH -") }
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
private fun LgQuickToolbarButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "tb_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(Shape20)
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderColor, Shape20)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}
