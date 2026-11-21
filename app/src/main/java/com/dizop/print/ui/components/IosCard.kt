package com.dizop.print.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dizop.print.ui.theme.IosCardBorder
import com.dizop.print.ui.theme.IosCardLight

/**
 * iOS-styled card container with rounded squircle corners, subtle hairline border,
 * and soft depth shadow.
 */
@Composable
fun IosCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    backgroundColor: Color = IosCardLight,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = shape, spotColor = Color(0x0F000000), ambientColor = Color(0x05000000))
            .clip(shape)
            .background(backgroundColor)
            .border(width = 0.5.dp, color = IosCardBorder, shape = shape)
            .then(clickableModifier)
            .padding(16.dp),
        content = content
    )
}
