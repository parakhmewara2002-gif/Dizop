package com.dizop.print.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dizop.print.ui.theme.IosCardBorder
import com.dizop.print.ui.theme.IosCardLight

/**
 * iOS-styled card container with fluid spring scale press animations,
 * squircle corners, subtle hairline border, and zero-layout GPU graphicsLayer acceleration.
 */
@Composable
fun IosCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    backgroundColor: Color = IosCardLight,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // iOS Fluid Spring calibration: Damping 0.82f for soft settle, Stiffness 380f for organic mass-spring response
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "cardPressScale"
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundColor)
            .border(width = 0.5.dp, color = IosCardBorder, shape = shape)
            .then(clickableModifier)
            .padding(16.dp),
        content = content
    )
}
