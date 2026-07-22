package com.dental.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Burgundy = Color(0xFF7D0015)
val GraphiteGray = Color(0xFF2C2C2C)

@Composable
fun DentalLogoHeader(
    modifier: Modifier = Modifier,
    surfaceColor: Color = MaterialTheme.colorScheme.surface
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val side = minOf(size.width, size.height)
                val sw = side * 0.88f
                val sh = side * 0.92f
                val l = (size.width - sw) / 2f
                val t = (size.height - sh) / 2f + side * 0.02f
                val r = Rect(l, t, l + sw, t + sh)
                drawHeraldicSymbol(r)
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "КАБИНЕТ",
            color = GraphiteGray,
            style = MaterialTheme.typography.titleSmall.copy(
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp
            ),
            maxLines = 1
        )
        Text(
            text = "ДОКТОРА ОРЛОВА",
            color = GraphiteGray,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                fontSize = 25.sp,
                letterSpacing = 1.sp
            ),
            maxLines = 1
        )
    }
}

private fun DrawScope.drawHeraldicSymbol(rect: Rect) {
    val l = rect.left
    val t = rect.top
    val w = rect.width
    val h = rect.height

    fun xf(v: Float) = l + v * w
    fun yf(v: Float) = t + v * h

    val borderStroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val innerStroke = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    val shield = buildShieldPath(::xf, ::yf)
    drawPath(shield, Burgundy, style = Fill)
    drawPath(shield, GraphiteGray, style = borderStroke)
    drawPath(shield, Color.White, style = innerStroke)

    val tooth = buildMolarPath(::xf, ::yf)
    drawPath(tooth, Color.White, style = Fill)
    drawPath(tooth, GraphiteGray, style = innerStroke)

    val eagle = buildEaglePath(::xf, ::yf)
    drawPath(eagle, Burgundy, style = Fill)

    val eyeX = xf(0.475f)
    val eyeY = yf(0.105f)
    val eyeR = (xf(0.025f) - xf(0.0f)) * 0.4f
    drawCircle(Color.White, eyeR, Offset(eyeX, eyeY), style = Fill)
}

private fun buildShieldPath(
    px: (Float) -> Float,
    py: (Float) -> Float
): Path = Path().apply {
    moveTo(px(0.12f), py(0.02f))
    lineTo(px(0.88f), py(0.02f))
    cubicTo(px(0.95f), py(0.02f), px(0.97f), py(0.05f), px(0.97f), py(0.08f))
    cubicTo(px(0.97f), py(0.30f), px(0.93f), py(0.55f), px(0.86f), py(0.72f))
    cubicTo(px(0.78f), py(0.86f), px(0.65f), py(0.94f), px(0.52f), py(0.97f))
    cubicTo(px(0.48f), py(0.97f), px(0.45f), py(0.96f), px(0.42f), py(0.94f))
    cubicTo(px(0.32f), py(0.88f), px(0.20f), py(0.76f), px(0.14f), py(0.62f))
    cubicTo(px(0.07f), py(0.45f), px(0.03f), py(0.28f), px(0.03f), py(0.12f))
    cubicTo(px(0.03f), py(0.06f), px(0.05f), py(0.02f), px(0.12f), py(0.02f))
    close()
}

private fun buildMolarPath(
    px: (Float) -> Float,
    py: (Float) -> Float
): Path = Path().apply {
    moveTo(px(0.30f), py(0.28f))
    cubicTo(px(0.26f), py(0.18f), px(0.28f), py(0.06f), px(0.36f), py(0.03f))
    cubicTo(px(0.40f), py(0.02f), px(0.44f), py(0.05f), px(0.46f), py(0.09f))
    cubicTo(px(0.48f), py(0.12f), px(0.52f), py(0.10f), px(0.54f), py(0.06f))
    cubicTo(px(0.56f), py(0.04f), px(0.60f), py(0.03f), px(0.64f), py(0.06f))
    cubicTo(px(0.70f), py(0.10f), px(0.74f), py(0.18f), px(0.70f), py(0.28f))
    cubicTo(px(0.68f), py(0.34f), px(0.66f), py(0.38f), px(0.64f), py(0.42f))
    cubicTo(px(0.66f), py(0.50f), px(0.66f), py(0.62f), px(0.62f), py(0.72f))
    cubicTo(px(0.60f), py(0.80f), px(0.54f), py(0.84f), px(0.50f), py(0.80f))
    cubicTo(px(0.48f), py(0.74f), px(0.48f), py(0.62f), px(0.50f), py(0.52f))
    cubicTo(px(0.46f), py(0.60f), px(0.42f), py(0.72f), px(0.38f), py(0.80f))
    cubicTo(px(0.34f), py(0.84f), px(0.28f), py(0.80f), px(0.28f), py(0.72f))
    cubicTo(px(0.28f), py(0.62f), px(0.30f), py(0.50f), px(0.30f), py(0.28f))
    close()
}

private fun buildEaglePath(
    px: (Float) -> Float,
    py: (Float) -> Float
): Path = Path().apply {
    moveTo(px(0.49f), py(0.06f))
    cubicTo(px(0.50f), py(0.05f), px(0.52f), py(0.06f), px(0.52f), py(0.09f))
    lineTo(px(0.62f), py(0.10f))
    cubicTo(px(0.64f), py(0.10f), px(0.65f), py(0.13f), px(0.62f), py(0.15f))
    lineTo(px(0.55f), py(0.14f))
    cubicTo(px(0.59f), py(0.13f), px(0.64f), py(0.13f), px(0.68f), py(0.16f))
    cubicTo(px(0.70f), py(0.18f), px(0.69f), py(0.22f), px(0.65f), py(0.25f))
    cubicTo(px(0.62f), py(0.28f), px(0.58f), py(0.29f), px(0.55f), py(0.30f))
    lineTo(px(0.54f), py(0.40f))
    lineTo(px(0.56f), py(0.54f))
    cubicTo(px(0.58f), py(0.60f), px(0.56f), py(0.64f), px(0.52f), py(0.60f))
    lineTo(px(0.50f), py(0.56f))
    lineTo(px(0.48f), py(0.60f))
    cubicTo(px(0.44f), py(0.64f), px(0.42f), py(0.60f), px(0.44f), py(0.54f))
    lineTo(px(0.46f), py(0.40f))
    lineTo(px(0.45f), py(0.30f))
    cubicTo(px(0.42f), py(0.29f), px(0.38f), py(0.28f), px(0.35f), py(0.25f))
    cubicTo(px(0.31f), py(0.22f), px(0.30f), py(0.18f), px(0.32f), py(0.16f))
    cubicTo(px(0.36f), py(0.13f), px(0.41f), py(0.13f), px(0.45f), py(0.14f))
    lineTo(px(0.46f), py(0.11f))
    cubicTo(px(0.46f), py(0.08f), px(0.47f), py(0.06f), px(0.49f), py(0.06f))
    close()
}
