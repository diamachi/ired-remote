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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ired.app.ir.protocols.CherryLightsProtocol
import com.ired.app.model.UserRemote
import com.ired.app.ui.components.AddCustomButtonDialog
import com.ired.app.ui.theme.AccentBlue
import com.ired.app.ui.theme.AccentYellow
import com.ired.app.ui.theme.BorderColor
import com.ired.app.ui.theme.CardBackground
import com.ired.app.ui.theme.CherryCardBg
import com.ired.app.ui.theme.CherryCardBorder
import com.ired.app.ui.theme.CherryHeader
import com.ired.app.ui.theme.CherryPatternBg
import com.ired.app.ui.theme.CherryPatternBorder
import com.ired.app.ui.theme.CherryPatternText
import com.ired.app.ui.theme.DarkSurfaceVariant
import com.ired.app.ui.theme.Shape10
import com.ired.app.ui.theme.Shape12
import com.ired.app.ui.theme.Shape18
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary
import com.ired.app.ui.viewmodel.MainViewModel

// Pre-defined static mode list (avoids recreating objects on every recomposition)
private data class LightMode(val label: String, val code: Long, val desc: String)

private val CHERRY_MODES = listOf(
    LightMode("1: Comb", CherryLightsProtocol.CMD_MODE_1_COMB, "Combination"),
    LightMode("2: Wave", CherryLightsProtocol.CMD_MODE_2_WAVE, "In Wave"),
    LightMode("3: Seq", CherryLightsProtocol.CMD_MODE_3_SEQ, "Sequential"),
    LightMode("4: SloGlo", CherryLightsProtocol.CMD_MODE_4_SLOGLO, "Slo-Glo"),
    LightMode("5: Chase", CherryLightsProtocol.CMD_MODE_5_CHASE, "Chasing/Flash"),
    LightMode("6: Fade", CherryLightsProtocol.CMD_MODE_6_FADE, "Slow Fade"),
    LightMode("7: Twinkle", CherryLightsProtocol.CMD_MODE_7_TWINKLE, "Twinkle/Flash"),
    LightMode("8: Steady", CherryLightsProtocol.CMD_MODE_8_STEADY, "Steady On"),
    LightMode("8 (Alt)", CherryLightsProtocol.CMD_MODE_8_STEADY_ALT, "Steady Alt")
)

@Composable
fun CherryLightsScreen(
    remote: UserRemote,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var activeModeName by remember { mutableStateOf("Ready") }
    var showAddBtnDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shape18)
                .background(CherryCardBg)
                .border(1.dp, CherryCardBorder, Shape18)
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = "🌸 CHERRY BLOSSOM LIGHTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CherryHeader
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeModeName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Point at USB power plug / control box",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Power & Timer Row (Top of 13-Key Remote)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CherryActionButton(
                label = "ON",
                icon = Icons.Default.PowerSettingsNew,
                backgroundColor = Color(0xFF2E7D32),
                onClick = {
                    activeModeName = "Power ON"
                    viewModel.sendCherryCode(CherryLightsProtocol.CMD_ON, "Lights ON")
                },
                modifier = Modifier.weight(1f)
            )

            CherryActionButton(
                label = "TIMER",
                icon = Icons.Default.Schedule,
                backgroundColor = Color(0xFF1565C0),
                onClick = {
                    activeModeName = "Timer (6h)"
                    viewModel.sendCherryCode(CherryLightsProtocol.CMD_TIMER_6H, "Timer 6h")
                },
                modifier = Modifier.weight(1f)
            )

            CherryActionButton(
                label = "OFF",
                icon = Icons.Default.PowerSettingsNew,
                backgroundColor = Color(0xFFC62828),
                onClick = {
                    activeModeName = "Power OFF"
                    viewModel.sendCherryCode(CherryLightsProtocol.CMD_OFF, "Lights OFF")
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 8 Lighting Animation Patterns (3x3 Grid)
        Text(
            text = "LIGHTING PATTERNS (1 TO 8)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        CHERRY_MODES.chunked(3).forEach { rowModes ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowModes.forEach { mode ->
                    key(mode.code) {
                        CherryModeButton(
                            label = mode.label,
                            isSelected = activeModeName == mode.desc,
                            onClick = {
                                activeModeName = mode.desc
                                viewModel.sendCherryCode(mode.code, mode.desc)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dimming Controls
        Text(
            text = "DIMMING CONTROLS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(Shape12)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, BorderColor, Shape12)
                    .clickable {
                        activeModeName = "Dim -"
                        viewModel.sendCherryCode(CherryLightsProtocol.CMD_DIM_MINUS, "Dim -")
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BrightnessLow, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "DIM -", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(Shape12)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, BorderColor, Shape12)
                    .clickable {
                        activeModeName = "Dim + (A)"
                        viewModel.sendCherryCode(CherryLightsProtocol.CMD_DIM_PLUS_A, "Dim +")
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BrightnessHigh, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "DIM +", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(Shape12)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, BorderColor, Shape12)
                    .clickable {
                        activeModeName = "Dim + (B)"
                        viewModel.sendCherryCode(CherryLightsProtocol.CMD_DIM_PLUS_B, "Dim + Alt")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "DIM + (B)", color = TextMuted, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
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
private fun CherryActionButton(
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
            Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CherryModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "mode_scale"
    )

    Box(
        modifier = modifier
            .height(50.dp)
            .scale(scale)
            .clip(Shape12)
            .background(if (isSelected) CherryPatternBg.copy(alpha = 0.9f) else CherryPatternBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CherryHeader else CherryPatternBorder,
                shape = Shape12
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = CherryPatternText.copy(alpha = 0.3f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else CherryPatternText,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}
