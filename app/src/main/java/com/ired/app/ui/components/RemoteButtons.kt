package com.ired.app.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ired.app.ui.theme.AccentRed
import com.ired.app.ui.theme.BorderColor
import com.ired.app.ui.theme.DarkSurfaceVariant
import com.ired.app.ui.theme.Shape28
import com.ired.app.ui.theme.ShapeCircle
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    tint: Color = TextPrimary,
    backgroundColor: Color = DarkSurfaceVariant
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(ShapeCircle)
            .background(backgroundColor)
            .border(1.dp, BorderColor, ShapeCircle)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = tint.copy(alpha = 0.3f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.44f)
        )
    }
}

@Composable
fun PowerButton(
    isOn: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    RoundIconButton(
        icon = Icons.Default.PowerSettingsNew,
        contentDescription = "Power",
        onClick = onClick,
        modifier = modifier,
        size = size,
        tint = if (isOn) Color.White else AccentRed,
        backgroundColor = if (isOn) AccentRed else DarkSurfaceVariant
    )
}

@Composable
fun RockerSwitch(
    title: String,
    onPlusClick: () -> Unit,
    onMinusClick: () -> Unit,
    modifier: Modifier = Modifier,
    plusIcon: ImageVector = Icons.Default.Add,
    minusIcon: ImageVector = Icons.Default.Remove
) {
    val plusInteraction = remember { MutableInteractionSource() }
    val minusInteraction = remember { MutableInteractionSource() }
    val isPlusPressed by plusInteraction.collectIsPressedAsState()
    val isMinusPressed by minusInteraction.collectIsPressedAsState()

    val plusScale by animateFloatAsState(
        targetValue = if (isPlusPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "plus_scale"
    )
    val minusScale by animateFloatAsState(
        targetValue = if (isMinusPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "minus_scale"
    )

    Column(
        modifier = modifier
            .clip(Shape28)
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderColor, Shape28)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .scale(plusScale)
                .clip(ShapeCircle)
                .clickable(
                    interactionSource = plusInteraction,
                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
                    onClick = onPlusClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = plusIcon,
                contentDescription = "$title Plus",
                tint = TextPrimary
            )
        }

        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Box(
            modifier = Modifier
                .size(46.dp)
                .scale(minusScale)
                .clip(ShapeCircle)
                .clickable(
                    interactionSource = minusInteraction,
                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
                    onClick = onMinusClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = minusIcon,
                contentDescription = "$title Minus",
                tint = TextPrimary
            )
        }
    }
}
