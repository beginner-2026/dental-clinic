package com.dental.ui.odontogram

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.customBorder(
    width: Dp = 1.dp,
    color: Color = Color.Gray,
    dashed: Boolean = false,
    cornerRadius: Dp = 4.dp
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f) else null
    )
    val r = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(r, r),
        style = stroke,
        size = size
    )
}
