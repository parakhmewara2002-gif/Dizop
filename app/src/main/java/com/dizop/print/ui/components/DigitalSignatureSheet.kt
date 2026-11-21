package com.dizop.print.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dizop.print.ui.theme.IosBlue
import com.dizop.print.ui.theme.IosTextPrimary
import com.dizop.print.ui.theme.IosTextSecondary

/**
 * iOS-style Digital Signature Drawing Sheet
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DigitalSignatureSheet(
    onSignatureCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val points = remember { mutableStateListOf<Offset?>() }

    IosCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Draw,
                        contentDescription = null,
                        tint = IosBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Your Document",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = IosTextPrimary
                    )
                }

                Text(
                    text = "Clear",
                    color = IosBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Signature Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF9F9FB))
                    .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(16.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    points.add(Offset(event.x, event.y))
                                    true
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    points.add(Offset(event.x, event.y))
                                    true
                                }
                                MotionEvent.ACTION_UP -> {
                                    points.add(null)
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    val path = Path()
                    var isDrawing = false
                    points.forEach { point ->
                        if (point == null) {
                            isDrawing = false
                        } else if (!isDrawing) {
                            path.moveTo(point.x, point.y)
                            isDrawing = true
                        } else {
                            path.lineTo(point.x, point.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF003366), // Signature Navy Blue
                        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                if (points.isEmpty()) {
                    Text(
                        text = "Sign here with finger or stylus",
                        fontSize = 13.sp,
                        color = IosTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = IosTextPrimary)
                }

                Button(
                    onClick = {
                        val bitmap = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.parseColor("#003366")
                            strokeWidth = 6f
                            style = AndroidPaint.Style.STROKE
                            strokeCap = AndroidPaint.Cap.ROUND
                        }
                        var lastPoint: Offset? = null
                        points.forEach { pt ->
                            if (pt != null && lastPoint != null) {
                                canvas.drawLine(lastPoint!!.x, lastPoint!!.y, pt.x, pt.y, paint)
                            }
                            lastPoint = pt
                        }
                        onSignatureCaptured(bitmap)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                ) {
                    Text("Apply & Print", color = Color.White)
                }
            }
        }
    }
}
