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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dizop.print.ui.theme.IosCardBorder
import com.dizop.print.ui.theme.IosCardLight

/**
 * iOS-styled card container with fluid spring scale press animations,
 * squircle corners, subtle hairline border, and hardware-accelerated soft depth.
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

    // Ultra-smooth spring scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
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
            .scale(scale)
            .shadow(elevation = 1.dp, shape = shape, spotColor = Color(0x0A000000), ambientColor = Color(0x03000000))
            .clip(shape)
            .background(backgroundColor)
            .border(width = 0.5.dp, color = IosCardBorder, shape = shape)
            .then(clickableModifier)
            .padding(16.dp),
        content = content
    )
}
