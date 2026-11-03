package com.ired.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ired.app.gtv.GtvDiscoveredDevice
import com.ired.app.gtv.GtvState
import com.ired.app.gtv.GtvWire
import com.ired.app.model.UserRemote
import com.ired.app.ui.theme.DarkSurfaceVariant
import com.ired.app.ui.theme.Shape10
import com.ired.app.ui.theme.Shape12
import com.ired.app.ui.theme.Shape16
import com.ired.app.ui.theme.Shape20
import com.ired.app.ui.theme.ShapeCircle
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary
import com.ired.app.ui.viewmodel.MainViewModel
import kotlin.math.abs

// Google TV Brand Colors
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC04)
private val GoogleGreen = Color(0xFF34A853)

private val GtvCardBg = Color(0xFF161A22)
private val GtvCardBorder = Color(0xFF262E3D)
private val GtvTouchpadBg = Color(0xFF12151C)
private val GtvTouchpadBorder = Color(0xFF2E3747)

private enum class NavMode {
    TOUCHPAD,
    DPAD
}

/**
 * Google TV Streamer Controller Screen.
 * Pure Native Android TV Remote v2 Protocol over Wi-Fi (TLS Ports 6467 / 6466).
 * Zero external apps required. Zero IR Blaster involvement.
 */
@Composable
fun GoogleTvScreen(
    remote: UserRemote,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var navMode by remember(remote.id) { mutableStateOf(NavMode.DPAD) }

    val gtvState by viewModel.gtvState(remote.id).collectAsState()
    val discoveredDevices by viewModel.gtvDiscoveredDevices(remote.id).collectAsState()

    var showDiscoveryDialog by remember { mutableStateOf(false) }
    var showManualIpDialog by remember { mutableStateOf(false) }
    var manualIpInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showConnectionActions by remember(remote.id) { mutableStateOf(false) }
    var useLgIrVolume by remember(remote.id) { mutableStateOf(viewModel.usesLgIrVolume(remote.id)) }
    var renameInput by remember(remote.id, remote.name) { mutableStateOf(remote.name) }
    if (showRenameDialog) AlertDialog(
        onDismissRequest = { showRenameDialog = false },
        title = { Text("Rename remote", color = TextPrimary) },
        text = { OutlinedTextField(value = renameInput, onValueChange = { renameInput = it.take(40) }, singleLine = true, label = { Text("Remote name") }) },
        confirmButton = { TextButton(onClick = { if (renameInput.isNotBlank()) viewModel.renameRemote(remote.id, renameInput.trim()); showRenameDialog = false }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } },
        containerColor = GtvCardBg
    )

    // Dialog: PIN Pairing Prompt (when TV displays 6-character code)
    if (gtvState is GtvState.PairingPrompt) {
        val pairing = gtvState as GtvState.PairingPrompt
        AlertDialog(
            onDismissRequest = { viewModel.disconnectGtv(remote.id) },
            title = {
                Text(
                    text = "Pair Google TV Streamer",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the 6-character code displayed on your TV screen (${pairing.tvName}):",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.uppercase().filter { char -> char in "0123456789ABCDEF" }.take(6) },
                        label = { Text("6-Character PIN") },
                        placeholder = { Text("e.g. 3F89A1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (pinInput.length == 6) {
                                    viewModel.submitGtvPin(remote.id, pinInput)
                                    pinInput = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoogleBlue,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 6) {
                            viewModel.submitGtvPin(remote.id, pinInput)
                            pinInput = ""
                        } else {
                            Toast.makeText(context, "Code must be 6 characters", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = pinInput.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                ) {
                    Text("Pair & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.disconnectGtv(remote.id) }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = GtvCardBg
        )
    }

    // Dialog: mDNS Device Discovery
    if (showDiscoveryDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscoveryDialog = false
                viewModel.stopGtvDiscovery(remote.id)
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Google TV", fontWeight = FontWeight.Bold, color = TextPrimary)
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = GoogleBlue,
                        strokeWidth = 2.dp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Scanning local Wi-Fi for Android TV & Google TV devices...",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (discoveredDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No devices found yet. Searching...", fontSize = 12.sp, color = TextMuted)
                        }
                    } else {
                        discoveredDevices.forEach { dev ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(Shape10)
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, GtvCardBorder, Shape10)
                                    .clickable {
                                        showDiscoveryDialog = false
                                        viewModel.stopGtvDiscovery(remote.id)
                                        viewModel.pairGtv(remote.id, dev.host, dev.name)
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(dev.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(dev.host, fontSize = 11.sp, color = GoogleBlue)
                                    }
                                    Text("Pair", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoogleGreen)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showDiscoveryDialog = false
                            viewModel.stopGtvDiscovery(remote.id)
                            showManualIpDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter IP Manually", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showDiscoveryDialog = false
                    viewModel.stopGtvDiscovery(remote.id)
                }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = GtvCardBg
        )
    }

    // Dialog: Manual IP Entry
    if (showManualIpDialog) {
        AlertDialog(
            onDismissRequest = { showManualIpDialog = false },
            title = {
                Text("Enter Google TV IP", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Enter the local Wi-Fi IP address of your Google TV Streamer (from TV Settings > Network & Internet):",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { manualIpInput = it.trim() },
                        label = { Text("TV IP Address") },
                        placeholder = { Text("192.168.1.50") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (manualIpInput.isNotBlank()) {
                                    showManualIpDialog = false
                                    viewModel.pairGtv(remote.id, manualIpInput, remote.name)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoogleBlue,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (manualIpInput.isNotBlank()) {
                                showManualIpDialog = false
                                viewModel.connectGtv(remote.id, manualIpInput, remote.name)
                            }
                        }
                    ) {
                        Text("Direct Connect", fontSize = 12.sp, color = TextPrimary)
                    }
                    Button(
                        onClick = {
                            if (manualIpInput.isNotBlank()) {
                                showManualIpDialog = false
                                viewModel.pairGtv(remote.id, manualIpInput, remote.name)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                    ) {
                        Text("Pair with PIN", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualIpDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = GtvCardBg
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Device Status Header
        GtvDeviceHeaderCard(
            gtvState = gtvState,
            onPowerClick = {
                triggerHaptic(context)
                viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_POWER)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Direct Wi-Fi Connection Card (Pairing, Status, and Error diagnostics)
        GtvWifiConnectionCard(
            gtvState = gtvState,
            onRenameClick = { showRenameDialog = true },
            showConnectionActions = showConnectionActions,
            onConnectionActionsClick = { showConnectionActions = !showConnectionActions },
            onScanClick = {
                triggerHaptic(context)
                showDiscoveryDialog = true
                viewModel.startGtvDiscovery(remote.id)
            },
            onManualIpClick = {
                triggerHaptic(context)
                showManualIpDialog = true
            },
            onRetryPairClick = { host ->
                triggerHaptic(context)
                viewModel.pairGtv(remote.id, host, remote.name)
            },
            onDirectConnectClick = { host ->
                triggerHaptic(context)
                viewModel.connectGtv(remote.id, host, remote.name)
            },
            onDisconnectClick = {
                triggerHaptic(context)
                viewModel.disconnectGtv(remote.id)
            },
            onCancelClick = {
                triggerHaptic(context)
                viewModel.cancelGtvConnection(remote.id)
            },
            onUnpairClick = {
                triggerHaptic(context)
                viewModel.unpairGtv(remote.id)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Switcher: Swipe Touchpad vs D-Pad Buttons
        GtvModeSwitcher(
            currentMode = navMode,
            onModeSelect = {
                navMode = it
                triggerHaptic(context)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Controller: Touchpad Canvas or D-Pad Ring
        Crossfade(
            targetState = navMode,
            animationSpec = tween(150),
            label = "gtv_nav_crossfade"
        ) { mode ->
            when (mode) {
                NavMode.TOUCHPAD -> {
                    GtvTouchpadCanvas(
                        onSwipe = { directionKey ->
                            triggerHaptic(context)
                            viewModel.sendGtvKey(remote.id, directionKey)
                        },
                        onCenterClick = {
                            triggerHaptic(context)
                            viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_DPAD_CENTER)
                        }
                    )
                }
                NavMode.DPAD -> {
                    GtvVirtualDpadWireless(
                        onDirectionClick = { keyCode ->
                            triggerHaptic(context)
                            viewModel.sendGtvKey(remote.id, keyCode)
                        },
                        onCenterClick = {
                            triggerHaptic(context)
                            viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_DPAD_CENTER)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Core System Navigation Row: Back, Home, Google Assistant
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GtvSystemRoundButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Back",
                accentColor = Color(0xFFCBD5E1),
                onClick = {
                    triggerHaptic(context)
                    viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_BACK)
                }
            )

            GtvSystemRoundButton(
                icon = Icons.Default.Home,
                label = "Home",
                accentColor = GoogleBlue,
                onClick = {
                    triggerHaptic(context)
                    viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_HOME)
                }
            )

            GtvAssistantRoundButton(
                onClick = {
                    triggerHaptic(context)
                    viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_SEARCH)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("TV volume via LG IR", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Use when HDMI-CEC is enabled and the TV is an LG", color = TextMuted, fontSize = 11.sp)
            }
            Switch(checked = useLgIrVolume, enabled = viewModel.isIrHardwareAvailable,
                onCheckedChange = { useLgIrVolume = it; viewModel.setUsesLgIrVolume(remote.id, it) })
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Volume & Quick Control Row
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GtvVolumePill(
                label = "VOL -",
                onClick = {
                    triggerHaptic(context)
                    if (useLgIrVolume) viewModel.sendNecCode(com.ired.app.ir.protocols.LgTvIrProtocol.CMD_VOL_DOWN, "TV volume down") else viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_VOLUME_DOWN)
                }
            )

            GtvVolumePill(
                label = "MUTE",
                icon = Icons.AutoMirrored.Filled.VolumeMute,
                onClick = {
                    triggerHaptic(context)
                    if (useLgIrVolume) viewModel.sendNecCode(com.ired.app.ir.protocols.LgTvIrProtocol.CMD_MUTE, "TV mute") else viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_VOLUME_MUTE)
                }
            )

            GtvVolumePill(
                label = "VOL +",
                onClick = {
                    triggerHaptic(context)
                    if (useLgIrVolume) viewModel.sendNecCode(com.ired.app.ir.protocols.LgTvIrProtocol.CMD_VOL_UP, "TV volume up") else viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_VOLUME_UP)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bottom Controls: Settings & Media Play/Pause
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GtvVolumePill(
                label = "SETTINGS",
                icon = Icons.Default.Settings,
                onClick = {
                    triggerHaptic(context)
                    viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_SETTINGS)
                }
            )

            GtvVolumePill(
                label = "PLAY / PAUSE",
                icon = Icons.Default.PlayArrow,
                onClick = {
                    triggerHaptic(context)
                    viewModel.sendGtvKey(remote.id, GtvWire.KEYCODE_MEDIA_PLAY_PAUSE)
                }
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

/**
 * Top Device Header Card showing Google TV Streamer connection state.
 */
@Composable
private fun GtvDeviceHeaderCard(
    gtvState: GtvState,
    onPowerClick: () -> Unit
) {
    val isConnected = gtvState is GtvState.Connected
    val isConnecting = gtvState is GtvState.Connecting || gtvState is GtvState.Discovering

    val statusDotColor = when {
        isConnected -> GoogleGreen
        isConnecting -> GoogleYellow
        gtvState is GtvState.Error -> GoogleRed
        else -> Color(0xFF64748B)
    }

    val statusSubtitle = when (gtvState) {
        is GtvState.Connected -> "Wi-Fi Connected (${gtvState.host})"
        is GtvState.Connecting -> "Connecting to ${gtvState.host}..."
        is GtvState.Discovering -> "Scanning Wi-Fi network..."
        is GtvState.PairingPrompt -> "Pairing code required on TV..."
        is GtvState.Error -> "Connection failed (Tap info below)"
        GtvState.Disconnected -> "Direct Wi-Fi Remote (No IR Blaster)"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shape16)
            .background(GtvCardBg)
            .border(1.dp, GtvCardBorder, Shape16)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(Shape12)
                        .background(GoogleBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = GoogleBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (gtvState is GtvState.Connected) gtvState.tvName else "Google TV Streamer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(ShapeCircle)
                                .background(statusDotColor)
                        )
                    }
                    Text(
                        text = statusSubtitle,
                        fontSize = 11.sp,
                        color = when {
                            isConnected -> GoogleGreen
                            gtvState is GtvState.Error -> GoogleRed
                            else -> TextMuted
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Power Icon Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(ShapeCircle)
                    .background(Color(0xFF2E1A1E))
                    .border(1.dp, GoogleRed.copy(alpha = 0.5f), ShapeCircle)
                    .clickable(onClick = onPowerClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = GoogleRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Direct Wi-Fi Connection Card with scan, manual IP, error diagnostics, and connection controls.
 */
@Composable
private fun GtvWifiConnectionCard(
    gtvState: GtvState,
    onRenameClick: () -> Unit,
    showConnectionActions: Boolean,
    onConnectionActionsClick: () -> Unit,
    onScanClick: () -> Unit,
    onManualIpClick: () -> Unit,
    onRetryPairClick: (String) -> Unit,
    onDirectConnectClick: (String) -> Unit,
    onDisconnectClick: () -> Unit,
    onCancelClick: () -> Unit,
    onUnpairClick: () -> Unit
) {
    val isConnected = gtvState is GtvState.Connected

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shape16)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E2838), Color(0xFF151C28))
                )
            )
            .border(1.dp, Color(0xFF2F3C54), Shape16)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(ShapeCircle).background(GoogleBlue))
                        Box(modifier = Modifier.size(7.dp).clip(ShapeCircle).background(GoogleRed))
                        Box(modifier = Modifier.size(7.dp).clip(ShapeCircle).background(GoogleYellow))
                        Box(modifier = Modifier.size(7.dp).clip(ShapeCircle).background(GoogleGreen))
                    }
                    TextButton(onClick = onRenameClick) { Text("Rename", fontSize = 12.sp) }
                }

                Text(
                    text = when {
                        isConnected -> "WI-FI CONNECTED"
                        gtvState is GtvState.Error -> "CONNECTION ERROR"
                        else -> "DIRECT WI-FI REMOTE"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = when {
                        isConnected -> GoogleGreen
                        gtvState is GtvState.Error -> GoogleRed
                        else -> GoogleBlue
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (gtvState) {
                GtvState.Disconnected -> {
                    Text(
                        "Connect to your TV to enable remote buttons. Scan your Wi-Fi network or enter the Streamer's IP address.",
                        fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onScanClick,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan Wi-Fi")
                        }
                        OutlinedButton(
                            onClick = onManualIpClick,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Enter IP")
                        }
                    }
                }
                is GtvState.Connecting -> Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoogleBlue)
                        Text(" Connecting…", color = TextPrimary, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Cancel connection")
                    }
                }
                is GtvState.Connected -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Connected to ${gtvState.tvName}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    }
                    Text(
                        text = "Controlling over local network at ${gtvState.host}. Direct encrypted TLS session on port 6466.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onConnectionActionsClick, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showConnectionActions) "Hide connection options" else "Connection options", fontSize = 12.sp)
                    }
                    if (showConnectionActions) Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(Shape10)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, GtvCardBorder, Shape10)
                                .clickable(onClick = onDisconnectClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Disconnect", fontSize = 12.sp, color = TextPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(Shape10)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, GtvCardBorder, Shape10)
                                .clickable(onClick = onUnpairClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Unpair Device", fontSize = 12.sp, color = GoogleRed)
                        }
                    }
                }
                is GtvState.Error -> {
                    // Prominent Error Diagnostic Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shape12)
                            .background(Color(0xFF2E181D))
                            .border(1.dp, GoogleRed.copy(alpha = 0.5f), Shape12)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = GoogleRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Connection Failed",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleRed
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = gtvState.message,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val errorHost = gtvState.host
                    if (!errorHost.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(Shape10)
                                    .background(GoogleBlue)
                                    .clickable { onRetryPairClick(errorHost) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Retry Pair (PIN)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(Shape10)
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, GtvCardBorder, Shape10)
                                    .clickable { onDirectConnectClick(errorHost) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Direct Connect (6466)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(Shape10)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, GtvCardBorder, Shape10)
                                .clickable(onClick = onScanClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = GoogleBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Wi-Fi", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(Shape10)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, GtvCardBorder, Shape10)
                                .clickable(onClick = onManualIpClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, null, tint = GoogleYellow, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change IP", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = "Connect Google TV Streamer",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Controls Google TV Streamer over local Wi-Fi without IR blasters. Pairs once with the PIN shown on your TV.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Scan mDNS button
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(42.dp)
                                .clip(Shape10)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF3872E0), Color(0xFF2563EB))
                                    )
                                )
                                .clickable(onClick = onScanClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan Wi-Fi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Manual IP button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(Shape10)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, GtvCardBorder, Shape10)
                                .clickable(onClick = onManualIpClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = GoogleYellow,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Enter IP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mode Switcher: Touchpad (Swipe) vs D-Pad (Directional Buttons).
 */
@Composable
private fun GtvModeSwitcher(
    currentMode: NavMode,
    onModeSelect: (NavMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(38.dp)
            .clip(Shape20)
            .background(Color(0xFF151922))
            .border(1.dp, GtvCardBorder, Shape20)
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Touchpad Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(Shape20)
                .background(if (currentMode == NavMode.TOUCHPAD) GoogleBlue else Color.Transparent)
                .clickable { onModeSelect(NavMode.TOUCHPAD) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Touchpad (Swipe)",
                fontSize = 11.sp,
                fontWeight = if (currentMode == NavMode.TOUCHPAD) FontWeight.Bold else FontWeight.Medium,
                color = if (currentMode == NavMode.TOUCHPAD) Color.White else TextMuted
            )
        }

        // D-Pad Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(Shape20)
                .background(if (currentMode == NavMode.DPAD) GoogleBlue else Color.Transparent)
                .clickable { onModeSelect(NavMode.DPAD) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "D-Pad (Buttons)",
                fontSize = 11.sp,
                fontWeight = if (currentMode == NavMode.DPAD) FontWeight.Bold else FontWeight.Medium,
                color = if (currentMode == NavMode.DPAD) Color.White else TextMuted
            )
        }
    }
}

/**
 * Swipe Touchpad Canvas: Drag in 4 directions to inject D-Pad keys, tap to select (OK).
 */
@Composable
private fun GtvTouchpadCanvas(
    onSwipe: (Int) -> Unit,
    onCenterClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "tp_scale"
    )

    var totalDx by remember { mutableFloatStateOf(0f) }
    var totalDy by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .height(230.dp)
            .scale(scale)
            .clip(Shape20)
            .background(GtvTouchpadBg)
            .border(1.5.dp, GtvTouchpadBorder, Shape20)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = GoogleBlue.copy(alpha = 0.25f)),
                onClick = onCenterClick
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDx = 0f
                        totalDy = 0f
                    },
                    onDragEnd = {
                        val threshold = with(density) { 40.dp.toPx() }
                        if (abs(totalDx) > threshold || abs(totalDy) > threshold) {
                            if (abs(totalDx) > abs(totalDy)) {
                                if (totalDx > 0) onSwipe(GtvWire.KEYCODE_DPAD_RIGHT)
                                else onSwipe(GtvWire.KEYCODE_DPAD_LEFT)
                            } else {
                                if (totalDy > 0) onSwipe(GtvWire.KEYCODE_DPAD_DOWN)
                                else onSwipe(GtvWire.KEYCODE_DPAD_UP)
                            }
                        }
                    },
                    onDrag = { _, dragAmount ->
                        totalDx += dragAmount.x
                        totalDy += dragAmount.y
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Center OK Disc
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(ShapeCircle)
                .background(Color(0xFF1E2532))
                .border(1.dp, Color(0xFF333F54), ShapeCircle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Direction Indicators
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Swipe to navigate • Tap to select",
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Tactile D-Pad Controller Ring for button navigation.
 */
@Composable
private fun GtvVirtualDpadWireless(
    onDirectionClick: (Int) -> Unit,
    onCenterClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer D-Pad Base Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ShapeCircle)
                .background(GtvTouchpadBg)
                .border(1.5.dp, GtvTouchpadBorder, ShapeCircle)
        )

        // UP
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(52.dp)
                .clip(ShapeCircle)
                .clickable { onDirectionClick(GtvWire.KEYCODE_DPAD_UP) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = TextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        // DOWN
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(52.dp)
                .clip(ShapeCircle)
                .clickable { onDirectionClick(GtvWire.KEYCODE_DPAD_DOWN) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = TextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        // LEFT
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .size(52.dp)
                .clip(ShapeCircle)
                .clickable { onDirectionClick(GtvWire.KEYCODE_DPAD_LEFT) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = TextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        // RIGHT
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(52.dp)
                .clip(ShapeCircle)
                .clickable { onDirectionClick(GtvWire.KEYCODE_DPAD_RIGHT) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Right",
                tint = TextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        // CENTER OK
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(ShapeCircle)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2E384C), Color(0xFF1A212E))
                    )
                )
                .border(1.5.dp, Color(0xFF42526F), ShapeCircle)
                .clickable(onClick = onCenterClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

/**
 * Circular System Button (Back, Home, etc.).
 */
@Composable
private fun GtvSystemRoundButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "sys_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(ShapeCircle)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF222B3A), Color(0xFF161C26))
                    )
                )
                .border(1.2.dp, GtvCardBorder, ShapeCircle)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 27.dp),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
    }
}

/**
 * Voice Search Button with Google Brand Dots.
 */
@Composable
private fun GtvAssistantRoundButton(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "asst_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(ShapeCircle)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF242F42), Color(0xFF18202D))
                    )
                )
                .border(1.5.dp, GoogleBlue.copy(alpha = 0.4f), ShapeCircle)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 27.dp),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Open voice assistant",
                    tint = GoogleBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.size(3.dp).clip(ShapeCircle).background(GoogleBlue))
                    Box(modifier = Modifier.size(3.dp).clip(ShapeCircle).background(GoogleRed))
                    Box(modifier = Modifier.size(3.dp).clip(ShapeCircle).background(GoogleYellow))
                    Box(modifier = Modifier.size(3.dp).clip(ShapeCircle).background(GoogleGreen))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Voice Assist",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
    }
}

/**
 * Tactile pill button for Volume controls.
 */
@Composable
private fun GtvVolumePill(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "vol_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .height(48.dp)
            .clip(Shape12)
            .background(DarkSurfaceVariant)
            .border(1.dp, GtvCardBorder, Shape12)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextPrimary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

private fun triggerHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(18)
        }
    } catch (_: Exception) {}
}
