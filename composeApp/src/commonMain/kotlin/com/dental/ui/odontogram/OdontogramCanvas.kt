package com.dental.ui.odontogram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dental.model.*

private val CrownGreen = Color(0xFF4CAF50)
private val CrownBlack = Color(0xFF212121)
private val CrownLightBlue = Color(0xFF03A9F4)
private val CrownPink = Color(0xFFE91E63)
private val CrownOrange = Color(0xFFFF9800)
private val YellowEndo = Color(0xFFFFEB3B)
private val EnamelColor = Color(0xFFF5F0E8)
private val EnamelLight = Color(0xFFFFFAF0)
private val GumRed = Color(0xFFE53935)
private val BoneBeige = Color(0xFFD7CCC8)
private val ImplantGray = Color(0xFF757575)
private val DarkBlue = Color(0xFF1565C0)
private val OutlineGray = Color(0xFFBDBDBD)
private val DarkOutline = Color(0xFF555555)
private val PurpleFilling = Color(0xFFCE93D8)
private val PerioRed = Color(0xFFEF5350)
private val RootColor = Color(0xFFE8DCC8)
private val MissingGray = Color(0xFFE0E0E0)
private val DividerLineColor = Color(0xFF90A4AE).copy(alpha = 0.45f)
private val ShadowColor = Color(0xFF000000).copy(alpha = 0.06f)
private val CanalColor = Color(0xFF8D6E63).copy(alpha = 0.75f)

internal enum class LayerType { ANATOMICAL, CROWNS, RESTORATIONS, CONTOUR }

@Composable
fun OdontogramLayers(
    teeth: List<Tooth>,
    prostheticItems: List<ProstheticItem>,
    selectedTooth: Int?,
    activeLayer: OdontogramLayer,
    onToothClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    upperTeeth: List<Int> = (18 downTo 11).toList() + (21..28).toList(),
    lowerTeeth: List<Int> = (48 downTo 41).toList() + (31..38).toList()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        ToothNumbering(
            toothNumbers = upperTeeth,
            modifier = Modifier.fillMaxWidth()
        )

        ArchCanvas(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            layerType = LayerType.ANATOMICAL,
            toothNumbers = upperTeeth,
            onToothClick = onToothClick,
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        Spacer(Modifier.height(16.dp))

        ArchCanvas(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            layerType = LayerType.ANATOMICAL,
            toothNumbers = lowerTeeth,
            onToothClick = onToothClick,
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        ToothNumbering(
            toothNumbers = lowerTeeth,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun ArchCanvas(
    teeth: List<Tooth>,
    prostheticItems: List<ProstheticItem>,
    selectedTooth: Int?,
    layerType: LayerType,
    toothNumbers: List<Int>,
    onToothClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(toothNumbers) {
            detectTapGestures { offset ->
                val totalWidth = size.width.toFloat()
                val clickedTooth = toothAtPosition(offset.x, totalWidth, toothNumbers)
                if (clickedTooth != null) {
                    onToothClick(clickedTooth)
                }
            }
        }
    ) {
        drawRect(Color(0xFFF5F5F5).copy(alpha = 0.5f), topLeft = Offset.Zero, size = size)
        drawArch(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            toothNumbers = toothNumbers,
            layerType = layerType,
            y = 0f,
            height = size.height,
            totalWidth = size.width
        )
    }
}

private fun toothAtPosition(x: Float, totalWidth: Float, toothNumbers: List<Int>): Int? {
    if (toothNumbers.isEmpty()) return null
    val toothWidth = totalWidth / toothNumbers.size
    val index = (x / toothWidth).toInt().coerceIn(0, toothNumbers.size - 1)
    return toothNumbers[index]
}

private fun DrawScope.drawArch(
    teeth: List<Tooth>,
    prostheticItems: List<ProstheticItem>,
    selectedTooth: Int?,
    toothNumbers: List<Int>,
    layerType: LayerType,
    y: Float,
    height: Float,
    totalWidth: Float
) {
    if (toothNumbers.isEmpty()) return
    val toothWidth = totalWidth / toothNumbers.size
    val padding = toothWidth * 0.1f
    val drawWidth = toothWidth - padding * 2

    toothNumbers.forEachIndexed { index, number ->
        val x = index * toothWidth + padding
        val tooth = teeth.find { it.number == number }
        val prosthetics = prostheticItems.filter { number in it.toothIds }
        val isSelected = selectedTooth == number

        when (layerType) {
            LayerType.ANATOMICAL -> drawAnatomicalTooth(x, y, drawWidth, height, tooth, prosthetics, isSelected)
            LayerType.CROWNS -> drawCrownTooth(x, y, drawWidth, height, tooth, prosthetics, isSelected)
            LayerType.RESTORATIONS -> drawRestorationTooth(x, y, drawWidth, height, tooth, prosthetics, isSelected)
            LayerType.CONTOUR -> drawContourTooth(x, y, drawWidth, height, tooth, prosthetics, isSelected)
        }
    }
}

private fun DrawScope.drawAnatomicalTooth(
    x: Float, y: Float, w: Float, h: Float,
    tooth: Tooth?, prosthetics: List<ProstheticItem>, isSelected: Boolean
) {
    val isMissing = tooth?.status == ToothStatus.MISSING
    val isImplant = tooth?.status == ToothStatus.IMPLANT
    val number = tooth?.number ?: return
    val toothType = getToothType(number)

    if (isMissing) {
        drawCircle(MissingGray, w * 0.3f, Offset(x + w / 2, y + h / 2))
        return
    }

    val cx = x + w / 2
    val crownH = h * 0.45f
    val rootH = h - crownH
    val cervicalY = y + crownH

    if (isImplant) {
        drawRect(ImplantGray, topLeft = Offset(cx - w * 0.3f, cervicalY + 3), size = Size(w * 0.6f, rootH - 6), style = Stroke(width = 2f))
        for (i in 0..4) {
            val ty = cervicalY + 6 + i * (rootH - 12) / 5
            drawLine(ImplantGray, Offset(cx - w * 0.25f, ty), Offset(cx + w * 0.25f, ty), strokeWidth = 1f)
        }
    } else {
        when (toothType) {
            ToothType.UPPER_MOLAR -> drawUpperMolarRoots(cx, cervicalY, w, rootH)
            ToothType.LOWER_MOLAR -> drawLowerMolarRoots(cx, cervicalY, w, rootH)
            ToothType.UPPER_PREMOLAR -> drawPremolarRoots(cx, cervicalY, w, rootH)
            ToothType.LOWER_PREMOLAR -> drawSingleRoot(cx, cervicalY, w, rootH)
            else -> drawSingleRoot(cx, cervicalY, w, rootH)
        }
    }

    if (prosthetics.any { it.type == ProstheticType.POST_CORE }) {
        drawRect(Color(0xFF9E9E9E), topLeft = Offset(cx - w * 0.08f, cervicalY + 3), size = Size(w * 0.16f, rootH * 0.45f))
    }

    val crownColor = when {
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.METAL_CERAMIC } -> CrownGreen
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.METAL } -> CrownBlack
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.ZIRCONIUM } -> CrownLightBlue
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.CERAMIC } -> CrownPink
        prosthetics.any { it.type == ProstheticType.TEMPORARY } -> CrownOrange
        prosthetics.any { it.type == ProstheticType.IMPLANT } -> ImplantGray.copy(alpha = 0.3f)
        prosthetics.any { it.type == ProstheticType.PONTIC && it.material == ProstheticMaterial.METAL_CERAMIC } -> CrownGreen
        prosthetics.any { it.type == ProstheticType.PONTIC && it.material == ProstheticMaterial.METAL } -> CrownBlack
        prosthetics.any { it.type == ProstheticType.PONTIC } -> CrownLightBlue
        else -> EnamelColor
    }

    drawCrownByType(cx, y, w, crownH, cervicalY, toothType, number, crownColor)

    val midCrownY = y + crownH * 0.5f
    val dividePath = Path().apply {
        moveTo(cx - w * 0.35f, midCrownY)
        cubicTo(cx - w * 0.15f, midCrownY - 1f, cx + w * 0.15f, midCrownY - 1f, cx + w * 0.35f, midCrownY)
    }
    drawPath(dividePath, DividerLineColor, style = Stroke(width = 1.2f))
    val dividePath2 = Path().apply {
        moveTo(cx - w * 0.3f, midCrownY + 2f)
        cubicTo(cx - w * 0.1f, midCrownY, cx + w * 0.1f, midCrownY, cx + w * 0.3f, midCrownY + 2f)
    }
    drawPath(dividePath2, DividerLineColor.copy(alpha = 0.25f), style = Stroke(width = 0.6f))

    drawLine(GumRed, start = Offset(x + 1, cervicalY), end = Offset(x + w - 1, cervicalY), strokeWidth = 1.5f)

    if (isSelected) {
        drawRect(DarkBlue, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 2f))
    }
}

// ---- Root drawing helpers ----

private fun DrawScope.drawUpperMolarRoots(cx: Float, cervicalY: Float, w: Float, rootH: Float) {
    val rootColor = RootColor
    val outline = DarkOutline
    val stroke = Stroke(width = 1.5f)

    val palatal = Path().apply {
        moveTo(cx - w * 0.16f, cervicalY)
        cubicTo(cx - w * 0.15f, cervicalY + rootH * 0.25f, cx - w * 0.08f, cervicalY + rootH * 0.55f, cx - w * 0.02f, cervicalY + rootH * 0.78f)
        cubicTo(cx + w * 0.03f, cervicalY + rootH * 0.93f, cx + w * 0.10f, cervicalY + rootH * 0.93f, cx + w * 0.08f, cervicalY + rootH * 0.78f)
        cubicTo(cx + w * 0.06f, cervicalY + rootH * 0.55f, cx + w * 0.13f, cervicalY + rootH * 0.25f, cx + w * 0.16f, cervicalY)
        close()
    }
    drawPath(palatal, rootColor); drawPath(palatal, outline, style = stroke)

    val mb = Path().apply {
        moveTo(cx - w * 0.28f, cervicalY)
        cubicTo(cx - w * 0.30f, cervicalY + rootH * 0.25f, cx - w * 0.34f, cervicalY + rootH * 0.50f, cx - w * 0.36f, cervicalY + rootH * 0.72f)
        cubicTo(cx - w * 0.37f, cervicalY + rootH * 0.88f, cx - w * 0.26f, cervicalY + rootH * 0.90f, cx - w * 0.18f, cervicalY + rootH * 0.72f)
        cubicTo(cx - w * 0.12f, cervicalY + rootH * 0.50f, cx - w * 0.10f, cervicalY + rootH * 0.25f, cx - w * 0.10f, cervicalY)
        close()
    }
    drawPath(mb, rootColor); drawPath(mb, outline, style = stroke)

    val db = Path().apply {
        moveTo(cx + w * 0.10f, cervicalY)
        cubicTo(cx + w * 0.12f, cervicalY + rootH * 0.25f, cx + w * 0.18f, cervicalY + rootH * 0.50f, cx + w * 0.22f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.26f, cervicalY + rootH * 0.90f, cx + w * 0.37f, cervicalY + rootH * 0.88f, cx + w * 0.36f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.34f, cervicalY + rootH * 0.50f, cx + w * 0.30f, cervicalY + rootH * 0.25f, cx + w * 0.28f, cervicalY)
        close()
    }
    drawPath(db, rootColor); drawPath(db, outline, style = stroke)
    drawLine(CanalColor, Offset(cx, cervicalY + 4), Offset(cx + w * 0.03f, cervicalY + rootH * 0.78f), strokeWidth = 2.5f)
    drawLine(CanalColor, Offset(cx - w * 0.19f, cervicalY + 4), Offset(cx - w * 0.27f, cervicalY + rootH * 0.78f), strokeWidth = 2.5f)
    drawLine(CanalColor, Offset(cx + w * 0.19f, cervicalY + 4), Offset(cx + w * 0.27f, cervicalY + rootH * 0.78f), strokeWidth = 2.5f)
}

private fun DrawScope.drawLowerMolarRoots(cx: Float, cervicalY: Float, w: Float, rootH: Float) {
    val rootColor = RootColor
    val outline = DarkOutline
    val stroke = Stroke(width = 1.5f)

    val mesial1 = Path().apply {
        moveTo(cx - w * 0.28f, cervicalY)
        cubicTo(cx - w * 0.30f, cervicalY + rootH * 0.25f, cx - w * 0.28f, cervicalY + rootH * 0.50f, cx - w * 0.24f, cervicalY + rootH * 0.72f)
        cubicTo(cx - w * 0.22f, cervicalY + rootH * 0.90f, cx - w * 0.12f, cervicalY + rootH * 0.92f, cx - w * 0.08f, cervicalY + rootH * 0.72f)
        cubicTo(cx - w * 0.05f, cervicalY + rootH * 0.50f, cx - w * 0.08f, cervicalY + rootH * 0.25f, cx - w * 0.12f, cervicalY)
        close()
    }
    drawPath(mesial1, rootColor); drawPath(mesial1, outline, style = stroke)

    val mesial2 = Path().apply {
        moveTo(cx - w * 0.06f, cervicalY)
        cubicTo(cx - w * 0.04f, cervicalY + rootH * 0.25f, cx - w * 0.01f, cervicalY + rootH * 0.50f, cx + w * 0.02f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.05f, cervicalY + rootH * 0.90f, cx + w * 0.14f, cervicalY + rootH * 0.92f, cx + w * 0.16f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.18f, cervicalY + rootH * 0.50f, cx + w * 0.16f, cervicalY + rootH * 0.25f, cx + w * 0.10f, cervicalY)
        close()
    }
    drawPath(mesial2, rootColor); drawPath(mesial2, outline, style = stroke)

    val distal = Path().apply {
        moveTo(cx + w * 0.12f, cervicalY)
        cubicTo(cx + w * 0.15f, cervicalY + rootH * 0.25f, cx + w * 0.22f, cervicalY + rootH * 0.50f, cx + w * 0.26f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.29f, cervicalY + rootH * 0.90f, cx + w * 0.38f, cervicalY + rootH * 0.92f, cx + w * 0.36f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.33f, cervicalY + rootH * 0.50f, cx + w * 0.28f, cervicalY + rootH * 0.25f, cx + w * 0.25f, cervicalY)
        close()
    }
    drawPath(distal, rootColor); drawPath(distal, outline, style = stroke)
    drawLine(CanalColor, Offset(cx - w * 0.20f, cervicalY + 4), Offset(cx - w * 0.20f, cervicalY + rootH * 0.78f), strokeWidth = 2.5f)
    drawLine(CanalColor, Offset(cx + w * 0.02f, cervicalY + 4), Offset(cx + w * 0.05f, cervicalY + rootH * 0.78f), strokeWidth = 2.5f)
    drawLine(CanalColor, Offset(cx + w * 0.185f, cervicalY + 4), Offset(cx + w * 0.30f, cervicalY + rootH * 0.78f), strokeWidth = 2.5f)
}

private fun DrawScope.drawPremolarRoots(cx: Float, cervicalY: Float, w: Float, rootH: Float) {
    val rootColor = RootColor
    val outline = DarkOutline
    val stroke = Stroke(width = 1.5f)

    val mesial = Path().apply {
        moveTo(cx - w * 0.22f, cervicalY)
        cubicTo(cx - w * 0.24f, cervicalY + rootH * 0.20f, cx - w * 0.28f, cervicalY + rootH * 0.45f, cx - w * 0.30f, cervicalY + rootH * 0.65f)
        cubicTo(cx - w * 0.32f, cervicalY + rootH * 0.84f, cx - w * 0.20f, cervicalY + rootH * 0.88f, cx - w * 0.14f, cervicalY + rootH * 0.65f)
        cubicTo(cx - w * 0.10f, cervicalY + rootH * 0.45f, cx - w * 0.08f, cervicalY + rootH * 0.20f, cx - w * 0.08f, cervicalY)
        close()
    }
    drawPath(mesial, rootColor); drawPath(mesial, outline, style = stroke)

    val distal = Path().apply {
        moveTo(cx + w * 0.08f, cervicalY)
        cubicTo(cx + w * 0.10f, cervicalY + rootH * 0.20f, cx + w * 0.15f, cervicalY + rootH * 0.45f, cx + w * 0.20f, cervicalY + rootH * 0.65f)
        cubicTo(cx + w * 0.24f, cervicalY + rootH * 0.84f, cx + w * 0.34f, cervicalY + rootH * 0.88f, cx + w * 0.30f, cervicalY + rootH * 0.65f)
        cubicTo(cx + w * 0.26f, cervicalY + rootH * 0.45f, cx + w * 0.22f, cervicalY + rootH * 0.20f, cx + w * 0.22f, cervicalY)
        close()
    }
    drawPath(distal, rootColor); drawPath(distal, outline, style = stroke)
    drawLine(CanalColor, Offset(cx - w * 0.15f, cervicalY + 4), Offset(cx - w * 0.22f, cervicalY + rootH * 0.72f), strokeWidth = 2.5f)
    drawLine(CanalColor, Offset(cx + w * 0.15f, cervicalY + 4), Offset(cx + w * 0.24f, cervicalY + rootH * 0.72f), strokeWidth = 2.5f)
}

private fun DrawScope.drawSingleRoot(cx: Float, cervicalY: Float, w: Float, rootH: Float) {
    val rootW = w * 0.35f
    val rootPath = Path().apply {
        moveTo(cx - rootW, cervicalY)
        cubicTo(cx - rootW * 0.95f, cervicalY + rootH * 0.15f, cx - rootW * 0.8f, cervicalY + rootH * 0.4f, cx - rootW * 0.5f, cervicalY + rootH * 0.7f)
        cubicTo(cx - rootW * 0.3f, cervicalY + rootH * 0.9f, cx, cervicalY + rootH * 0.95f, cx + rootW * 0.3f, cervicalY + rootH * 0.7f)
        cubicTo(cx + rootW * 0.8f, cervicalY + rootH * 0.4f, cx + rootW * 0.95f, cervicalY + rootH * 0.15f, cx + rootW, cervicalY)
        close()
    }
    drawPath(rootPath, RootColor)
    drawPath(rootPath, DarkOutline, style = Stroke(width = 1.5f))
    drawLine(CanalColor, Offset(cx, cervicalY + 4), Offset(cx, cervicalY + rootH * 0.82f), strokeWidth = 2.5f)
}

private fun DrawScope.drawCrownByType(cx: Float, y: Float, w: Float, crownH: Float, cervicalY: Float, toothType: ToothType, number: Int, crownColor: Color) {
    val isCanine = number == 13 || number == 23 || number == 33 || number == 43
    val isCentralIncisor = number == 11 || number == 21 || number == 31 || number == 41
    val isLateralIncisor = number == 12 || number == 22 || number == 32 || number == 42

    val maxW: Float
    val bottomW: Float
    val midW: Float
    val topW: Float

    when (toothType) {
        ToothType.UPPER_MOLAR, ToothType.LOWER_MOLAR -> {
            maxW = w * 0.88f
            bottomW = w * 0.58f
            midW = w * 0.84f
            topW = w * 0.50f
        }
        ToothType.UPPER_PREMOLAR, ToothType.LOWER_PREMOLAR -> {
            if (toothType == ToothType.LOWER_PREMOLAR) {
                maxW = w * 0.88f
                bottomW = w * 0.70f
                midW = w * 0.84f
                topW = w * 0.38f
            } else {
                maxW = w * 0.74f
                bottomW = w * 0.50f
                midW = w * 0.66f
                topW = w * 0.38f
            }
        }
        ToothType.UPPER_ANTERIOR, ToothType.LOWER_ANTERIOR -> {
            if (isCanine) {
                maxW = w * 0.76f
                bottomW = w * 0.70f
                midW = w * 0.74f
                topW = w * 0.14f
            } else if (isCentralIncisor) {
                if (toothType == ToothType.UPPER_ANTERIOR) {
                    maxW = w * 0.92f
                    bottomW = w * 0.70f
                    midW = w * 0.86f
                    topW = w * 0.90f
                } else {
                    maxW = w * 0.86f
                    bottomW = w * 0.70f
                    midW = w * 0.76f
                    topW = w * 0.84f
                }
            } else if (isLateralIncisor) {
                maxW = w * 0.86f
                bottomW = w * 0.70f
                midW = w * 0.76f
                topW = w * 0.84f
            } else {
                maxW = w * 0.55f
                bottomW = w * 0.34f
                midW = w * 0.46f
                topW = maxW * 0.70f
            }
        }
        else -> {
            maxW = w * 0.6f
            bottomW = w * 0.45f
            midW = w * 0.52f
            topW = w * 0.30f
        }
    }

    val isMolar = toothType == ToothType.UPPER_MOLAR || toothType == ToothType.LOWER_MOLAR
    val isPremolar = toothType == ToothType.UPPER_PREMOLAR || toothType == ToothType.LOWER_PREMOLAR
    val isIncisor = isCentralIncisor || isLateralIncisor
    val hasSharpCusp = number == 34

    val crownPath = Path().apply {
        moveTo(cx - bottomW / 2, cervicalY)
        cubicTo(
            cx - midW * 0.5f, cervicalY - crownH * 0.15f,
            cx - midW * 0.5f, y + crownH * 0.35f,
            cx - topW * 0.5f, y
        )
        if (isMolar) {
            cubicTo(
                cx - topW * 0.38f, y - crownH * 0.01f,
                cx - topW * 0.30f, y - crownH * 0.05f,
                cx - topW * 0.20f, y - crownH * 0.06f
            )
            cubicTo(
                cx - topW * 0.14f, y - crownH * 0.02f,
                cx - topW * 0.06f, y + crownH * 0.03f,
                cx, y + crownH * 0.04f
            )
            cubicTo(
                cx + topW * 0.06f, y + crownH * 0.03f,
                cx + topW * 0.14f, y - crownH * 0.02f,
                cx + topW * 0.20f, y - crownH * 0.06f
            )
            cubicTo(
                cx + topW * 0.30f, y - crownH * 0.05f,
                cx + topW * 0.38f, y - crownH * 0.01f,
                cx + topW * 0.50f, y
            )
        } else if (isPremolar) {
            if (hasSharpCusp) {
                cubicTo(
                    cx - topW * 0.38f, y + crownH * 0.04f,
                    cx - topW * 0.15f, y - crownH * 0.10f,
                    cx, y - crownH * 0.12f
                )
                cubicTo(
                    cx + topW * 0.15f, y - crownH * 0.10f,
                    cx + topW * 0.38f, y + crownH * 0.04f,
                    cx + topW * 0.50f, y
                )
            } else {
                cubicTo(
                    cx - topW * 0.38f, y + crownH * 0.02f,
                    cx - topW * 0.18f, y - crownH * 0.06f,
                    cx, y - crownH * 0.07f
                )
                cubicTo(
                    cx + topW * 0.18f, y - crownH * 0.06f,
                    cx + topW * 0.38f, y + crownH * 0.02f,
                    cx + topW * 0.50f, y
                )
            }
        } else if (isIncisor) {
            cubicTo(
                cx - topW * 0.48f, y + crownH * 0.08f,
                cx - topW * 0.28f, y - crownH * 0.05f,
                cx - topW * 0.12f, y
            )
            lineTo(cx + topW * 0.12f, y)
            cubicTo(
                cx + topW * 0.28f, y - crownH * 0.05f,
                cx + topW * 0.48f, y + crownH * 0.08f,
                cx + topW * 0.50f, y
            )
        } else {
            lineTo(cx + topW * 0.5f, y)
        }
        cubicTo(
            cx + midW * 0.5f, y + crownH * 0.35f,
            cx + midW * 0.5f, cervicalY - crownH * 0.15f,
            cx + bottomW * 0.5f, cervicalY
        )
        close()
    }

    drawPath(crownPath, crownColor)
    drawPath(crownPath, DarkOutline, style = Stroke(width = 1.5f))

    val shadowPath = Path().apply {
        moveTo(cx + topW * 0.3f, y + crownH * 0.1f)
        cubicTo(cx + midW * 0.45f, y + crownH * 0.3f,
                cx + midW * 0.45f, cervicalY - crownH * 0.3f,
                cx + bottomW * 0.3f, cervicalY)
        lineTo(cx + bottomW * 0.5f, cervicalY)
        cubicTo(cx + midW * 0.55f, cervicalY - crownH * 0.3f,
                cx + midW * 0.55f, y + crownH * 0.3f,
                cx + topW * 0.45f, y + crownH * 0.1f)
        close()
    }
    drawPath(shadowPath, ShadowColor)
}

private fun DrawScope.drawCrownTooth(
    x: Float, y: Float, w: Float, h: Float,
    tooth: Tooth?, prosthetics: List<ProstheticItem>, isSelected: Boolean
) {
    val isMissing = tooth?.status == ToothStatus.MISSING
    if (isMissing) {
        drawRect(MissingGray, topLeft = Offset(x + 1, y + 1), size = Size(w - 2, h - 2))
        return
    }

    val cx = x + w / 2
    val cy = y + h / 2
    val halfW = w * 0.35f
    val halfH = h * 0.35f

    val bodyPath = Path().apply {
        moveTo(cx - halfW, cy + halfH)
        cubicTo(cx - halfW, cy + halfH * 0.3f,
                cx - halfW * 1.1f, cy - halfH * 0.2f,
                cx - halfW * 0.7f, cy - halfH * 0.7f)
        cubicTo(cx - halfW * 0.4f, cy - halfH * 1.1f,
                cx + halfW * 0.4f, cy - halfH * 1.1f,
                cx + halfW * 0.7f, cy - halfH * 0.7f)
        cubicTo(cx + halfW * 1.1f, cy - halfH * 0.2f,
                cx + halfW, cy + halfH * 0.3f,
                cx + halfW, cy + halfH)
        close()
    }

    val crownColor = when {
        prosthetics.any { it.type == ProstheticType.IMPLANT } -> ImplantGray.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.METAL_CERAMIC } -> CrownGreen
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.METAL } -> CrownBlack
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.ZIRCONIUM } -> CrownLightBlue
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.CERAMIC } -> CrownPink
        prosthetics.any { it.type == ProstheticType.TEMPORARY } -> CrownOrange
        prosthetics.any { it.type == ProstheticType.PONTIC && it.material == ProstheticMaterial.METAL_CERAMIC } -> CrownGreen
        prosthetics.any { it.type == ProstheticType.PONTIC && it.material == ProstheticMaterial.METAL } -> CrownBlack
        prosthetics.any { it.type == ProstheticType.PONTIC } -> CrownLightBlue
        prosthetics.any { it.type == ProstheticType.POST_CORE } -> Color(0xFFBCAAA4)
        else -> EnamelColor
    }

    drawPath(bodyPath, crownColor)
    drawPath(bodyPath, color = OutlineGray, style = Stroke(width = 0.8f))

    // Horizontal dividing line
    val divideY = cy - halfH * 0.15f
    val dividePath = Path().apply {
        moveTo(cx - halfW * 0.8f, divideY)
        cubicTo(cx - halfW * 0.3f, divideY - 1.5f,
                cx + halfW * 0.3f, divideY - 1.5f,
                cx + halfW * 0.8f, divideY)
    }
    drawPath(dividePath, DividerLineColor, style = Stroke(width = 1.2f))

    if (prosthetics.any { it.type == ProstheticType.CROWN && it.stage == ProstheticStage.COMPLETED }) {
        drawCircle(YellowEndo, halfW * 0.15f, Offset(cx, cy))
    }

    if (prosthetics.any { it.type == ProstheticType.BRIDGE } || prosthetics.any { it.type == ProstheticType.PONTIC }) {
        drawLine(Color(0xFF66BB6A), Offset(x, cy), Offset(x + w, cy), strokeWidth = 3f)
    }
    if (prosthetics.any { it.type == ProstheticType.POST_CORE }) {
        drawRect(Color(0xFF8D6E63), topLeft = Offset(cx - halfW * 0.15f, cy - halfH * 0.3f), size = Size(halfW * 0.3f, halfH * 0.6f))
    }

    if (isSelected) {
        drawRect(DarkBlue, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawRestorationTooth(
    x: Float, y: Float, w: Float, h: Float,
    tooth: Tooth?, prosthetics: List<ProstheticItem>, isSelected: Boolean
) {
    val isMissing = tooth?.status == ToothStatus.MISSING
    val cx = x + w / 2
    val cy = y + h / 2
    val r = minOf(w, h) * 0.35f

    if (isMissing) {
        drawCircle(MissingGray, r, Offset(cx, cy))
        drawLine(Color(0xFF9E9E9E), Offset(cx - r * 0.7f, cy - r * 0.7f), Offset(cx + r * 0.7f, cy + r * 0.7f), strokeWidth = 2f)
        return
    }

    val fillColor = when {
        prosthetics.any { it.type == ProstheticType.REMOVAL } -> PerioRed.copy(alpha = 0.4f)
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.METAL_CERAMIC } -> CrownGreen.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.METAL } -> CrownBlack.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.ZIRCONIUM } -> CrownLightBlue.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.CERAMIC } -> CrownPink.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.TEMPORARY } -> CrownOrange.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.PONTIC && it.material == ProstheticMaterial.METAL_CERAMIC } -> CrownGreen.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.PONTIC && it.material == ProstheticMaterial.METAL } -> CrownBlack.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.PONTIC } -> CrownLightBlue.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.IMPLANT || it.type == ProstheticType.BRIDGE } -> ImplantGray.copy(alpha = 0.5f)
        prosthetics.any { it.type == ProstheticType.POST_CORE } -> Color(0xFFBCAAA4).copy(alpha = 0.5f)
        else -> EnamelColor
    }

    drawOval(fillColor, topLeft = Offset(cx - r, cy - r * 0.7f), size = Size(r * 2, r * 1.4f))
    drawOval(OutlineGray, topLeft = Offset(cx - r, cy - r * 0.7f), size = Size(r * 2, r * 1.4f), style = Stroke(width = 0.8f))

    // Horizontal dividing line
    val divideY = cy - r * 0.1f
    drawLine(DividerLineColor, Offset(cx - r * 0.7f, divideY), Offset(cx + r * 0.7f, divideY), strokeWidth = 2.5f)

    if (prosthetics.isNotEmpty() && prosthetics.none { it.type == ProstheticType.CROWN || it.type == ProstheticType.IMPLANT || it.type == ProstheticType.BRIDGE || it.type == ProstheticType.PONTIC }) {
        val dotX = cx - r * 0.3f
        val dotY = cy - r * 0.15f
        drawCircle(PurpleFilling, r * 0.2f, Offset(dotX, dotY))
        drawCircle(Color.White, r * 0.08f, Offset(dotX - r * 0.05f, dotY - r * 0.05f))
    }

    if (isSelected) {
        drawRect(DarkBlue, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawContourTooth(
    x: Float, y: Float, w: Float, h: Float,
    tooth: Tooth?, prosthetics: List<ProstheticItem>, isSelected: Boolean
) {
    val isMissing = tooth?.status == ToothStatus.MISSING
    val cx = x + w / 2
    val cy = y + h / 2
    val r = minOf(w, h) * 0.3f

    val dashedStroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2f), 0f))
    val solidStroke = Stroke(width = 1.5f)

    if (isMissing) {
        drawCircle(MissingGray, r, Offset(cx, cy), style = dashedStroke)
        return
    }

    val hasProsthetic = prosthetics.isNotEmpty()
    val isImplant = tooth?.status == ToothStatus.IMPLANT || prosthetics.any { it.type == ProstheticType.IMPLANT }

    val contourFillColor = when {
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.METAL_CERAMIC } -> CrownGreen.copy(alpha = 0.3f)
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.METAL } -> CrownBlack.copy(alpha = 0.3f)
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.ZIRCONIUM } -> CrownLightBlue.copy(alpha = 0.3f)
        prosthetics.any { it.type == ProstheticType.CROWN && it.material == ProstheticMaterial.CERAMIC } -> CrownPink.copy(alpha = 0.3f)
        prosthetics.any { it.type == ProstheticType.TEMPORARY } -> CrownOrange.copy(alpha = 0.3f)
        prosthetics.any { it.type == ProstheticType.PONTIC && it.material == ProstheticMaterial.METAL_CERAMIC } -> CrownGreen.copy(alpha = 0.3f)
        prosthetics.any { it.type == ProstheticType.PONTIC && it.material == ProstheticMaterial.METAL } -> CrownBlack.copy(alpha = 0.3f)
        prosthetics.any { it.type == ProstheticType.PONTIC } -> CrownLightBlue.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val outlinePath = Path().apply {
        moveTo(cx - r, cy)
        cubicTo(cx - r, cy - r * 0.8f, cx, cy - r, cx + r, cy - r * 0.5f)
        cubicTo(cx + r, cy, cx + r * 0.5f, cy + r * 0.5f, cx, cy + r * 0.3f)
        cubicTo(cx - r * 0.5f, cy + r * 0.5f, cx - r, cy, cx - r, cy)
        close()
    }

    drawPath(outlinePath, color = contourFillColor)
    drawPath(outlinePath, color = OutlineGray, style = if (hasProsthetic && !isImplant) dashedStroke else solidStroke)

    // Horizontal dividing line
    val divideY = cy - r * 0.05f
    drawLine(DividerLineColor, Offset(cx - r * 0.7f, divideY), Offset(cx + r * 0.7f, divideY), strokeWidth = 1f)

    drawLine(GumRed, Offset(cx - r * 0.8f, cy - r * 0.3f), Offset(cx + r * 0.8f, cy - r * 0.3f), strokeWidth = 1.5f)

    if (isImplant) {
        drawRect(ImplantGray, topLeft = Offset(cx - r * 0.2f, cy - r * 0.6f), size = Size(r * 0.4f, r * 0.4f))
        drawCircle(ImplantGray, r * 0.15f, Offset(cx, cy - r * 0.7f))
    }

    if (prosthetics.any { it.type == ProstheticType.POST_CORE }) {
        drawRect(Color(0xFF8D6E63), topLeft = Offset(cx - r * 0.1f, cy - r * 0.4f), size = Size(r * 0.2f, r * 0.5f))
    }

    if (isSelected) {
        drawRect(DarkBlue, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 2f))
    }
}

@Composable
internal fun ToothNumbering(
    toothNumbers: List<Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        toothNumbers.forEach { number ->
            Text(
                text = "$number",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
                modifier = Modifier.width(28.dp),
                maxLines = 1
            )
        }
    }
}
