package com.dizop.print.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dizop.print.ui.theme.IosBlue
import com.dizop.print.ui.theme.IosGreen

data class PrintActivityState(
    val isPrinting: Boolean = false,
    val isComplete: Boolean = false,
    val statusMessage: String = "",
    val documentTitle: String = "",
    val printerName: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

/**
 * iOS Dynamic Island / Live Activity Pill Component
 * Floats at the top of the display with fluid spring animations.
 */
@Composable
fun DynamicIslandPill(
    state: PrintActivityState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isPrinting || state.isComplete,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + fadeOut()
    ) {
        val pillHeight by animateDpAsState(
            targetValue = if (state.isPrinting || state.isComplete) 46.dp else 0.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "pillHeight"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = Color(0x33000000))
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E)) // iOS Dark Glass
                    .height(pillHeight)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state.isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = IosBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Printing ${state.documentTitle}...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (state.isComplete) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Done",
                            tint = IosGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.statusMessage.isNotBlank()) state.statusMessage else "Print Complete",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
