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
private val PurpleFilling = Color(0xFFCE93D8)
private val PerioRed = Color(0xFFEF5350)
private val RootColor = Color(0xFFE8DCC8)
private val MissingGray = Color(0xFFE0E0E0)
private val DividerLineColor = Color(0xFF90A4AE).copy(alpha = 0.45f)
private val ShadowColor = Color(0xFF000000).copy(alpha = 0.06f)

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
        Text(
            text = "Анатомический",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
        )
        ToothLayer(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            layerType = LayerType.ANATOMICAL,
            upperTeeth = upperTeeth,
            lowerTeeth = lowerTeeth,
            onToothClick = onToothClick,
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Text(
            text = "Коронки",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp)
        )
        ToothLayer(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            layerType = LayerType.CROWNS,
            upperTeeth = upperTeeth,
            lowerTeeth = lowerTeeth,
            onToothClick = onToothClick,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )

        Text(
            text = "Реставрации",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp)
        )
        ToothLayer(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            layerType = LayerType.RESTORATIONS,
            upperTeeth = upperTeeth,
            lowerTeeth = lowerTeeth,
            onToothClick = onToothClick,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )

        Text(
            text = "Контур",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp)
        )
        ToothLayer(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            layerType = LayerType.CONTOUR,
            upperTeeth = upperTeeth,
            lowerTeeth = lowerTeeth,
            onToothClick = onToothClick,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        )

        ToothNumbering(
            upperTeeth = upperTeeth,
            lowerTeeth = lowerTeeth,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

@Composable
private fun ToothLayer(
    teeth: List<Tooth>,
    prostheticItems: List<ProstheticItem>,
    selectedTooth: Int?,
    layerType: LayerType,
    upperTeeth: List<Int>,
    lowerTeeth: List<Int>,
    onToothClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(upperTeeth, lowerTeeth) {
            detectTapGestures { offset ->
                val totalWidth = size.width.toFloat()
                val archGap = size.height * 0.08f
                val upperHeight = (size.height - archGap) / 2f
                val lowerY = upperHeight + archGap
                val lowerHeight = size.height - lowerY

                var clickedTooth: Int? = null
                if (offset.y <= upperHeight) {
                    clickedTooth = toothAtPosition(offset.x, totalWidth, upperTeeth)
                } else if (offset.y >= lowerY) {
                    clickedTooth = toothAtPosition(offset.x, totalWidth, lowerTeeth)
                }
                if (clickedTooth != null) {
                    onToothClick(clickedTooth)
                }
            }
        }
    ) {
        // Neutral background for the dental chart
        drawRect(Color(0xFFF5F5F5).copy(alpha = 0.5f), topLeft = Offset.Zero, size = size)

        val totalWidth = size.width
        val archGap = size.height * 0.08f
        val upperHeight = (size.height - archGap) / 2f
        val lowerY = upperHeight + archGap

        drawArch(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            toothNumbers = upperTeeth,
            layerType = layerType,
            y = 0f,
            height = upperHeight,
            totalWidth = totalWidth
        )
        drawArch(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            toothNumbers = lowerTeeth,
            layerType = layerType,
            y = lowerY,
            height = upperHeight,
            totalWidth = totalWidth
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

    if (isMissing) {
        drawCircle(MissingGray, w * 0.3f, Offset(x + w / 2, y + h / 2))
        return
    }

    val cx = x + w / 2
    val crownH = h * 0.18f
    val rootH = h - crownH
    val crownW = w * 0.72f
    val rootW = w * 0.52f
    val cervicalY = y + crownH

    // Bone background
    drawRect(BoneBeige.copy(alpha = 0.3f), topLeft = Offset(x, cervicalY), size = Size(w, rootH))

    // Root path — увеличен на 20%, с плавным переходом от коронки
    val rootPath = Path().apply {
        moveTo(cx - rootW / 2, cervicalY)
        cubicTo(cx - rootW * 0.48f, cervicalY + rootH * 0.1f,
                cx - rootW * 0.4f, cervicalY + rootH * 0.4f,
                cx - rootW * 0.28f, cervicalY + rootH * 0.7f)
        cubicTo(cx - rootW * 0.18f, cervicalY + rootH * 0.9f,
                cx, cervicalY + rootH * 0.95f,
                cx + rootW * 0.18f, cervicalY + rootH * 0.7f)
        cubicTo(cx + rootW * 0.4f, cervicalY + rootH * 0.4f,
                cx + rootW * 0.48f, cervicalY + rootH * 0.1f,
                cx + rootW / 2, cervicalY)
        close()
    }

    if (isImplant) {
        drawRect(ImplantGray, topLeft = Offset(cx - rootW * 0.35f, cervicalY + 3), size = Size(rootW * 0.7f, rootH - 6), style = Stroke(width = 2f))
        for (i in 0..4) {
            val ty = cervicalY + 6 + i * (rootH - 12) / 5
            drawLine(ImplantGray, Offset(cx - rootW * 0.3f, ty), Offset(cx + rootW * 0.3f, ty), strokeWidth = 1f)
        }
    } else {
        drawPath(rootPath, RootColor)
        drawPath(rootPath, Color(0xFFD7CCC8), style = Stroke(width = 1f))
    }

    // Crown shape — сглаженные углы
    val crownPath = Path().apply {
        moveTo(cx - crownW / 2, cervicalY)
        cubicTo(cx - crownW * 0.48f, cervicalY - crownH * 0.15f,
                cx - crownW * 0.45f, y + 2f,
                cx - crownW * 0.3f, y + 0.5f)
        cubicTo(cx - crownW * 0.15f, y,
                cx + crownW * 0.15f, y,
                cx + crownW * 0.3f, y + 0.5f)
        cubicTo(cx + crownW * 0.45f, y + 2f,
                cx + crownW * 0.48f, cervicalY - crownH * 0.15f,
                cx + crownW / 2, cervicalY)
        close()
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

    drawPath(crownPath, crownColor)
    drawPath(crownPath, color = OutlineGray, style = Stroke(width = 0.8f))

    // Subtle shadow on distal side for volume
    val shadowPath = Path().apply {
        moveTo(cx + crownW * 0.2f, y + 1f)
        cubicTo(cx + crownW * 0.4f, y + 2f,
                cx + crownW / 2 - 1f, cervicalY - crownH * 0.2f,
                cx + crownW / 2 - 1f, cervicalY)
        lineTo(cx + crownW / 2, cervicalY)
        cubicTo(cx + crownW / 2 + 1f, cervicalY - crownH * 0.2f,
                cx + crownW * 0.45f, y + 2f,
                cx + crownW * 0.25f, y + 1f)
        close()
    }
    drawPath(shadowPath, ShadowColor)

    // ---- HORIZONTAL DIVIDING LINE (crown/root separation) ----
    val divideY = cervicalY - 2f
    val dividePath = Path().apply {
        moveTo(cx - crownW * 0.45f, divideY + 1f)
        cubicTo(cx - crownW * 0.2f, divideY - 0.5f,
                cx + crownW * 0.2f, divideY - 0.5f,
                cx + crownW * 0.45f, divideY + 1f)
    }
    drawPath(dividePath, DividerLineColor, style = Stroke(width = 1.2f))
    val dividePath2 = Path().apply {
        moveTo(cx - crownW * 0.4f, divideY + 3f)
        cubicTo(cx - crownW * 0.15f, divideY + 1f,
                cx + crownW * 0.15f, divideY + 1f,
                cx + crownW * 0.4f, divideY + 3f)
    }
    drawPath(dividePath2, DividerLineColor.copy(alpha = 0.25f), style = Stroke(width = 0.6f))

    // Post/core marker in root
    if (prosthetics.any { it.type == ProstheticType.POST_CORE }) {
        drawRect(Color(0xFF9E9E9E), topLeft = Offset(cx - rootW * 0.1f, cervicalY + 3), size = Size(rootW * 0.2f, rootH * 0.45f))
    }

    // Gum line
    drawLine(GumRed, start = Offset(x + 1, cervicalY), end = Offset(x + w - 1, cervicalY), strokeWidth = 1.5f)

    if (isSelected) {
        drawRect(DarkBlue, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 2f))
    }
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
    drawLine(DividerLineColor, Offset(cx - r * 0.7f, divideY), Offset(cx + r * 0.7f, divideY), strokeWidth = 1.2f)

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
private fun ToothNumbering(
    upperTeeth: List<Int>,
    lowerTeeth: List<Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            upperTeeth.forEach { number ->
                Text(
                    text = "$number",
                    fontSize = 8.sp,
                    color = Color(0xFF212121),
                    modifier = Modifier.width(24.dp),
                    maxLines = 1
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            lowerTeeth.forEach { number ->
                Text(
                    text = "$number",
                    fontSize = 8.sp,
                    color = Color(0xFF212121),
                    modifier = Modifier.width(24.dp),
                    maxLines = 1
                )
            }
        }
    }
}
