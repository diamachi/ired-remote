package com.ired.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.ired.app.ui.theme.BorderColor
import com.ired.app.ui.theme.DpadCenter
import com.ired.app.ui.theme.DpadRing
import com.ired.app.ui.theme.ShapeCircle
import com.ired.app.ui.theme.TextPrimary

@Composable
fun VirtualDpad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(ShapeCircle)
            .background(DpadRing)
            .border(2.dp, BorderColor, ShapeCircle),
        contentAlignment = Alignment.Center
    ) {
        // UP
        DpadDirectionButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            onClick = onUp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
        )

        // DOWN
        DpadDirectionButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            onClick = onDown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-8).dp)
        )

        // LEFT
        DpadDirectionButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Left",
            onClick = onLeft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 8.dp)
        )

        // RIGHT
        DpadDirectionButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Right",
            onClick = onRight,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-8).dp)
        )

        // CENTER (OK)
        val centerInteraction = remember { MutableInteractionSource() }
        val isCenterPressed by centerInteraction.collectIsPressedAsState()
        val centerScale by animateFloatAsState(
            targetValue = if (isCenterPressed) 0.88f else 1.0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
            label = "center_scale"
        )

        Box(
            modifier = Modifier
                .size(size * 0.38f)
                .scale(centerScale)
                .clip(ShapeCircle)
                .background(DpadCenter)
                .border(1.5.dp, BorderColor, ShapeCircle)
                .clickable(
                    interactionSource = centerInteraction,
                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
                    onClick = onCenter
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun DpadDirectionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "dir_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(ShapeCircle)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(34.dp)
        )
    }
}
