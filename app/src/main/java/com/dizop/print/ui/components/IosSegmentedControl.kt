package com.dizop.print.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dizop.print.ui.theme.IosCardLight
import com.dizop.print.ui.theme.IosTextPrimary
import com.dizop.print.ui.theme.IosTextSecondary

/**
 * True iOS-styled Segmented Switcher with sliding physical thumb capsule
 * and calibrated mass-spring physics.
 */
@Composable
fun IosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(10.dp)
    val thumbShape = RoundedCornerShape(8.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(outerShape)
            .background(Color(0xFFEAEAEE))
            .padding(3.dp)
    ) {
        val segmentCount = items.size.coerceAtLeast(1)
        val segmentWidth = maxWidth / segmentCount

        // Sliding indicator offset with organic iOS spring response
        val thumbOffsetMultiplier by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
            label = "segmentedThumbSlide"
        )

        // Physical Sliding White Pill
        Box(
            modifier = Modifier
                .offset(x = segmentWidth * thumbOffsetMultiplier)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(elevation = 2.dp, shape = thumbShape, spotColor = Color(0x18000000), ambientColor = Color(0x0A000000))
                .clip(thumbShape)
                .background(IosCardLight)
        )

        // Clickable Labels
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) IosTextPrimary else IosTextSecondary
                    )
                }
            }
        }
    }
}
