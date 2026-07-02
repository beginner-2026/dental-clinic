package com.dental.ui.odontogram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
private val DarkOutline = Color(0xFF555555)
private val PurpleFilling = Color(0xFFCE93D8)
private val PerioRed = Color(0xFFEF5350)
private val RootColor = Color(0xFFE8DCC8)
private val MissingGray = Color(0xFFE0E0E0)
private const val CanalStrokeWidth = 7.5f
private val DividerLineColor = Color(0xFF90A4AE).copy(alpha = 0.45f)
private val ShadowColor = Color(0xFF000000).copy(alpha = 0.06f)
private val CanalColor = Color(0xFF8D6E63).copy(alpha = 0.75f)

internal val CrownOptionColors = mapOf(
    CrownOption.METAL_CERAMIC to Color(0xFF4CAF50),
    CrownOption.CAST_SOLID to Color(0xFF424242),
    CrownOption.ZIRCONIUM_OXIDE to Color(0xFF03A9F4),
    CrownOption.FULL_CERAMIC to Color(0xFF8BC34A),
    CrownOption.IMPLANT_CROWN to Color(0xFF9C27B0),
    CrownOption.TEMPORARY to Color(0xFFFF9800),
    CrownOption.ARTIFICIAL_MC to Color(0xFF4CAF50),
    CrownOption.ARTIFICIAL_CAST to Color(0xFF424242),
    CrownOption.ARTIFICIAL_REMOVABLE to Color(0xFFFF80AB),
    CrownOption.PLOMBA to Color(0xFFCE93D8),
    CrownOption.MISSING to Color(0xFFE0E0E0)
)

internal val CrownOptionAbbreviations = mapOf(
    CrownOption.METAL_CERAMIC to "МК",
    CrownOption.CAST_SOLID to "ЦЛ",
    CrownOption.ZIRCONIUM_OXIDE to "ОЦ",
    CrownOption.FULL_CERAMIC to "ЦК",
    CrownOption.IMPLANT_CROWN to "КИМ",
    CrownOption.TEMPORARY to "ВК",
    CrownOption.ARTIFICIAL_MC to "ИМК",
    CrownOption.ARTIFICIAL_CAST to "ИЦЛ",
    CrownOption.ARTIFICIAL_REMOVABLE to "ИП",
    CrownOption.PLOMBA to "П",
    CrownOption.MISSING to ""
)

internal enum class LayerType { ANATOMICAL, CROWNS, RESTORATIONS, CONTOUR }

@Composable
fun OdontogramLayers(
    teeth: List<Tooth>,
    prostheticItems: List<ProstheticItem>,
    selectedTooth: Int?,
    selectedToothPart: ToothPart? = null,
    activeLayer: OdontogramLayer,
    onToothClick: (Int, ToothPart?) -> Unit,
    modifier: Modifier = Modifier,
    upperTeeth: List<Int> = (18 downTo 11).toList() + (21..28).toList(),
    lowerTeeth: List<Int> = (48 downTo 41).toList() + (31..38).toList(),
    crownSelections: Map<Int, CrownOption> = emptyMap(),
    rootSelections: Map<Int, RootOption> = emptyMap()
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
            selectedToothPart = selectedToothPart,
            layerType = LayerType.ANATOMICAL,
            toothNumbers = upperTeeth,
            onToothClick = onToothClick,
            crownSelections = crownSelections,
            rootSelections = rootSelections,
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        Spacer(Modifier.height(16.dp))

        ArchCanvas(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            selectedToothPart = selectedToothPart,
            layerType = LayerType.ANATOMICAL,
            toothNumbers = lowerTeeth,
            onToothClick = onToothClick,
            crownSelections = crownSelections,
            rootSelections = rootSelections,
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
    selectedToothPart: ToothPart? = null,
    layerType: LayerType,
    toothNumbers: List<Int>,
    onToothClick: (Int, ToothPart?) -> Unit,
    modifier: Modifier = Modifier,
    crownSelections: Map<Int, CrownOption> = emptyMap(),
    rootSelections: Map<Int, RootOption> = emptyMap()
) {
    var canvasPxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier.onSizeChanged { canvasPxSize = it }) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(toothNumbers, layerType) {
                    detectTapGestures { offset ->
                        val totalWidth = size.width.toFloat()
                        val clickedTooth = toothAtPosition(offset.x, totalWidth, toothNumbers)
                        if (clickedTooth != null) {
                            val part = if (layerType == LayerType.ANATOMICAL) {
                                if (offset.y < size.height * 0.45f) ToothPart.CROWN else ToothPart.ROOT
                            } else null
                            onToothClick(clickedTooth, part)
                        }
                    }
                }
        ) {
            drawRect(Color(0xFFF5F5F5).copy(alpha = 0.5f), topLeft = Offset.Zero, size = size)
            drawArch(
                teeth = teeth,
                prostheticItems = prostheticItems,
                selectedTooth = selectedTooth,
                selectedToothPart = selectedToothPart,
                toothNumbers = toothNumbers,
                layerType = layerType,
                y = 0f,
                height = size.height,
                totalWidth = size.width,
                crownSelections = crownSelections,
                rootSelections = rootSelections
            )
        }

        // Text overlays for crown abbreviations and root labels
        if (canvasPxSize.width > 0 && layerType == LayerType.ANATOMICAL) {
            val w = canvasPxSize.width.toFloat()
            val h = canvasPxSize.height.toFloat()
            val toothWidth = w / toothNumbers.size
            val padding = toothWidth * 0.1f
            val drawWidth = toothWidth - padding * 2
            val crownH = h * 0.45f
            val rootH = h - crownH

            toothNumbers.forEachIndexed { index, number ->
                val cx = index * toothWidth + padding + drawWidth / 2f

                // Crown abbreviation
                val crownOpt = crownSelections[number]
                val abbr = CrownOptionAbbreviations[crownOpt] ?: ""
                if (abbr.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (cx - 25f).toInt(),
                                    ((crownH / 2f - 8f).toInt())
                                )
                            }
                            .width(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = abbr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }
                }

                // Root label (КВ / АШ / РЕТ)
                val rootOpt = rootSelections[number]
                if (rootOpt == RootOption.POST_CORE || rootOpt == RootOption.ANCHOR_PIN || rootOpt == RootOption.RETAINED) {
                    val label = when (rootOpt) {
                        RootOption.POST_CORE -> "КВ"
                        RootOption.ANCHOR_PIN -> "АШ"
                        RootOption.RETAINED -> "РЕТ"
                        else -> ""
                    }
                    val labelColor = when (rootOpt) {
                        RootOption.POST_CORE -> Color(0xFF212121)
                        RootOption.ANCHOR_PIN -> Color(0xFF212121)
                        RootOption.RETAINED -> Color(0xFF212121)
                        else -> Color(0xFF212121)
                    }
                    val labelPos = getRootLabelPosition(cx, crownH, drawWidth, rootH, getToothType(number))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (labelPos.x - 12f).toInt(),
                                    (labelPos.y - 7f).toInt()
                                )
                            }
                    )
                }
            }
        }
    }
}

private fun toothAtPosition(x: Float, totalWidth: Float, toothNumbers: List<Int>): Int? {
    if (toothNumbers.isEmpty()) return null
    val toothWidth = totalWidth / toothNumbers.size
    val index = (x / toothWidth).toInt().coerceIn(0, toothNumbers.size - 1)
    return toothNumbers[index]
}

private fun getRootLabelPosition(cx: Float, crownH: Float, w: Float, rootH: Float, toothType: ToothType): Offset {
    val cervicalY = crownH
    return when (toothType) {
        ToothType.UPPER_MOLAR -> Offset(cx, cervicalY + rootH * 0.18f)
        ToothType.UPPER_PREMOLAR -> Offset(cx, cervicalY + rootH * 0.18f)
        ToothType.LOWER_MOLAR -> Offset(cx + w * 0.08f, cervicalY + rootH * 0.18f)
        else -> Offset(cx, cervicalY + rootH * 0.18f)
    }
}

private fun DrawScope.drawArch(
    teeth: List<Tooth>,
    prostheticItems: List<ProstheticItem>,
    selectedTooth: Int?,
    selectedToothPart: ToothPart? = null,
    toothNumbers: List<Int>,
    layerType: LayerType,
    y: Float,
    height: Float,
    totalWidth: Float,
    crownSelections: Map<Int, CrownOption>,
    rootSelections: Map<Int, RootOption>
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
        val crownOpt = crownSelections[number]
        val rootOpt = rootSelections[number]
        val toothPart: ToothPart? = if (isSelected) selectedToothPart else null

        when (layerType) {
            LayerType.ANATOMICAL -> drawAnatomicalTooth(x, y, drawWidth, height, tooth, prosthetics, isSelected, crownOpt, rootOpt, toothPart)
            LayerType.CROWNS -> drawCrownTooth(x, y, drawWidth, height, tooth, prosthetics, isSelected)
            LayerType.RESTORATIONS -> drawRestorationTooth(x, y, drawWidth, height, tooth, prosthetics, isSelected)
            LayerType.CONTOUR -> drawContourTooth(x, y, drawWidth, height, tooth, prosthetics, isSelected)
        }
    }
}

private fun DrawScope.drawAnatomicalTooth(
    x: Float, y: Float, w: Float, h: Float,
    tooth: Tooth?, prosthetics: List<ProstheticItem>, isSelected: Boolean,
    crownOption: CrownOption?, rootOption: RootOption?,
    selectedPart: ToothPart? = null
) {
    val isMissing = tooth?.status == ToothStatus.MISSING
    val isImplant = tooth?.status == ToothStatus.IMPLANT
    val number = tooth?.number ?: return
    val toothType = getToothType(number)

    if (isMissing) {
        drawCircle(MissingGray, w * 0.3f, Offset(x + w / 2, y + h / 2))
        return
    }

    val crownMissing = crownOption == CrownOption.MISSING

    val cx = x + w / 2
    val crownH = h * 0.45f
    val rootH = h - crownH
    val cervicalY = y + crownH

    // --- ROOT RENDERING ---
    when (rootOption) {
        RootOption.MISSING -> { /* skip roots */ }

        RootOption.IMPLANT -> {
            val implantWidth = w * 0.48f
            val implantColor = Color(0xFF9C27B0)
            drawRect(implantColor, topLeft = Offset(cx - implantWidth / 2, cervicalY + 3), size = Size(implantWidth, rootH - 6), style = Stroke(width = 2f))
            for (i in 0..4) {
                val ty = cervicalY + 6 + i * (rootH - 12) / 5
                drawLine(implantColor, Offset(cx - implantWidth * 0.4f, ty), Offset(cx + implantWidth * 0.4f, ty), strokeWidth = 1f)
            }
        }

        RootOption.ENDO_TREATED -> {
            drawNormalRoots(cx, cervicalY, w, rootH, toothType, canalColor = Color(0xFF42A5F5), canalFraction = 1f)
        }

        RootOption.ENDO_PROBLEM -> {
            drawNormalRoots(cx, cervicalY, w, rootH, toothType, canalColor = Color(0xFFEF5350), canalFraction = 0.5f)
        }

        RootOption.RETAINED -> {
            drawNormalRoots(cx, cervicalY, w, rootH, toothType, rootFillColor = Color(0xFFEF9A9A))
        }

        RootOption.POST_CORE -> {
            drawNormalRoots(cx, cervicalY, w, rootH, toothType)
            drawPalatalRootFilled(cx, cervicalY, w, rootH, toothType, Color(0xFFBDBDBD))
        }

        RootOption.ANCHOR_PIN -> {
            drawNormalRoots(cx, cervicalY, w, rootH, toothType)
            drawPalatalRootFilled(cx, cervicalY, w, rootH, toothType, Color(0xFFFF9800))
        }

        else -> {
            if (isImplant) {
                drawRect(ImplantGray, topLeft = Offset(cx - w * 0.3f, cervicalY + 3), size = Size(w * 0.6f, rootH - 6), style = Stroke(width = 2f))
                for (i in 0..4) {
                    val ty = cervicalY + 6 + i * (rootH - 12) / 5
                    drawLine(ImplantGray, Offset(cx - w * 0.25f, ty), Offset(cx + w * 0.25f, ty), strokeWidth = 1f)
                }
            } else {
                drawNormalRoots(cx, cervicalY, w, rootH, toothType)
            }
        }
    }

    if (prosthetics.any { it.type == ProstheticType.POST_CORE }) {
        drawRect(Color(0xFF9E9E9E), topLeft = Offset(cx - w * 0.08f, cervicalY + 3), size = Size(w * 0.16f, rootH * 0.45f))
    }

    // --- CROWN RENDERING ---
    val (crownFill, crownOutline) = if (crownOption != null && crownOption != CrownOption.MISSING) {
        val col = CrownOptionColors[crownOption] ?: EnamelColor
        Pair(col.copy(alpha = 0.2f), col)
    } else if (crownOption == CrownOption.MISSING) {
        Pair(Color.Transparent, Color.Transparent)
    } else {
        val existingColor = when {
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
        val existingOutline = if (prosthetics.isEmpty() && existingColor == EnamelColor) OutlineGray else existingColor
        Pair(existingColor, existingOutline)
    }

    if (!crownMissing) {
        drawCrownFill(cx, y, w, crownH, cervicalY, toothType, number, crownFill)
        drawCrownOutline(cx, y, w, crownH, cervicalY, toothType, number, crownOutline)
    }

    // Dividing lines
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
        when (selectedPart) {
            ToothPart.CROWN -> drawRect(DarkBlue, topLeft = Offset(x, y), size = Size(w, crownH), style = Stroke(width = 2f))
            ToothPart.ROOT -> drawRect(DarkBlue, topLeft = Offset(x, cervicalY), size = Size(w, rootH), style = Stroke(width = 2f))
            null -> drawRect(DarkBlue, topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 2f))
        }
    }
}

private fun DrawScope.drawNormalRoots(
    cx: Float, cervicalY: Float, w: Float, rootH: Float, toothType: ToothType,
    canalColor: Color = CanalColor, canalFraction: Float = 1f,
    rootFillColor: Color = RootColor
) {
    when (toothType) {
        ToothType.UPPER_MOLAR -> drawUpperMolarRoots(cx, cervicalY, w, rootH, canalColor, canalFraction, rootFillColor)
        ToothType.LOWER_MOLAR -> drawLowerMolarRoots(cx, cervicalY, w, rootH, canalColor, canalFraction, rootFillColor)
        ToothType.UPPER_PREMOLAR -> drawPremolarRoots(cx, cervicalY, w, rootH, canalColor, canalFraction, rootFillColor)
        ToothType.LOWER_PREMOLAR -> drawSingleRoot(cx, cervicalY, w, rootH, canalColor, canalFraction, rootFillColor)
        else -> drawSingleRoot(cx, cervicalY, w, rootH, canalColor, canalFraction, rootFillColor)
    }
}

// ---- Root drawing helpers ----

private fun DrawScope.drawUpperMolarRoots(
    cx: Float, cervicalY: Float, w: Float, rootH: Float,
    canalColor: Color = CanalColor, canalFraction: Float = 1f,
    rootFillColor: Color = RootColor
) {
    val outline = DarkOutline
    val stroke = Stroke(width = 1.5f)

    val palatal = Path().apply {
        moveTo(cx - w * 0.16f, cervicalY)
        cubicTo(cx - w * 0.15f, cervicalY + rootH * 0.25f, cx - w * 0.08f, cervicalY + rootH * 0.55f, cx - w * 0.02f, cervicalY + rootH * 0.78f)
        cubicTo(cx + w * 0.03f, cervicalY + rootH * 0.93f, cx + w * 0.10f, cervicalY + rootH * 0.93f, cx + w * 0.08f, cervicalY + rootH * 0.78f)
        cubicTo(cx + w * 0.06f, cervicalY + rootH * 0.55f, cx + w * 0.13f, cervicalY + rootH * 0.25f, cx + w * 0.16f, cervicalY)
        close()
    }
    drawPath(palatal, rootFillColor); drawPath(palatal, outline, style = stroke)

    val mb = Path().apply {
        moveTo(cx - w * 0.28f, cervicalY)
        cubicTo(cx - w * 0.30f, cervicalY + rootH * 0.25f, cx - w * 0.34f, cervicalY + rootH * 0.50f, cx - w * 0.36f, cervicalY + rootH * 0.72f)
        cubicTo(cx - w * 0.37f, cervicalY + rootH * 0.88f, cx - w * 0.26f, cervicalY + rootH * 0.90f, cx - w * 0.18f, cervicalY + rootH * 0.72f)
        cubicTo(cx - w * 0.12f, cervicalY + rootH * 0.50f, cx - w * 0.10f, cervicalY + rootH * 0.25f, cx - w * 0.10f, cervicalY)
        close()
    }
    drawPath(mb, rootFillColor); drawPath(mb, outline, style = stroke)

    val db = Path().apply {
        moveTo(cx + w * 0.10f, cervicalY)
        cubicTo(cx + w * 0.12f, cervicalY + rootH * 0.25f, cx + w * 0.18f, cervicalY + rootH * 0.50f, cx + w * 0.22f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.26f, cervicalY + rootH * 0.90f, cx + w * 0.37f, cervicalY + rootH * 0.88f, cx + w * 0.36f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.34f, cervicalY + rootH * 0.50f, cx + w * 0.30f, cervicalY + rootH * 0.25f, cx + w * 0.28f, cervicalY)
        close()
    }
    drawPath(db, rootFillColor); drawPath(db, outline, style = stroke)

    val canalEnd = cervicalY + rootH * 0.78f * canalFraction
    drawLine(canalColor, Offset(cx, cervicalY + 4), Offset(cx + w * 0.03f, canalEnd), strokeWidth = CanalStrokeWidth)
    drawLine(canalColor, Offset(cx - w * 0.19f, cervicalY + 4), Offset(cx - w * 0.27f, canalEnd), strokeWidth = CanalStrokeWidth)
    drawLine(canalColor, Offset(cx + w * 0.19f, cervicalY + 4), Offset(cx + w * 0.27f, canalEnd), strokeWidth = CanalStrokeWidth)
}

private fun DrawScope.drawLowerMolarRoots(
    cx: Float, cervicalY: Float, w: Float, rootH: Float,
    canalColor: Color = CanalColor, canalFraction: Float = 1f,
    rootFillColor: Color = RootColor
) {
    val outline = DarkOutline
    val stroke = Stroke(width = 1.5f)

    val mesial1 = Path().apply {
        moveTo(cx - w * 0.28f, cervicalY)
        cubicTo(cx - w * 0.30f, cervicalY + rootH * 0.25f, cx - w * 0.28f, cervicalY + rootH * 0.50f, cx - w * 0.24f, cervicalY + rootH * 0.72f)
        cubicTo(cx - w * 0.22f, cervicalY + rootH * 0.90f, cx - w * 0.12f, cervicalY + rootH * 0.92f, cx - w * 0.08f, cervicalY + rootH * 0.72f)
        cubicTo(cx - w * 0.05f, cervicalY + rootH * 0.50f, cx - w * 0.08f, cervicalY + rootH * 0.25f, cx - w * 0.12f, cervicalY)
        close()
    }
    drawPath(mesial1, rootFillColor); drawPath(mesial1, outline, style = stroke)

    val mesial2 = Path().apply {
        moveTo(cx - w * 0.06f, cervicalY)
        cubicTo(cx - w * 0.04f, cervicalY + rootH * 0.25f, cx - w * 0.01f, cervicalY + rootH * 0.50f, cx + w * 0.02f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.05f, cervicalY + rootH * 0.90f, cx + w * 0.14f, cervicalY + rootH * 0.92f, cx + w * 0.16f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.18f, cervicalY + rootH * 0.50f, cx + w * 0.16f, cervicalY + rootH * 0.25f, cx + w * 0.10f, cervicalY)
        close()
    }
    drawPath(mesial2, rootFillColor); drawPath(mesial2, outline, style = stroke)

    val distal = Path().apply {
        moveTo(cx + w * 0.12f, cervicalY)
        cubicTo(cx + w * 0.15f, cervicalY + rootH * 0.25f, cx + w * 0.22f, cervicalY + rootH * 0.50f, cx + w * 0.26f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.29f, cervicalY + rootH * 0.90f, cx + w * 0.38f, cervicalY + rootH * 0.92f, cx + w * 0.36f, cervicalY + rootH * 0.72f)
        cubicTo(cx + w * 0.33f, cervicalY + rootH * 0.50f, cx + w * 0.28f, cervicalY + rootH * 0.25f, cx + w * 0.25f, cervicalY)
        close()
    }
    drawPath(distal, rootFillColor); drawPath(distal, outline, style = stroke)

    val canalEnd = cervicalY + rootH * 0.78f * canalFraction
    drawLine(canalColor, Offset(cx - w * 0.20f, cervicalY + 4), Offset(cx - w * 0.20f, canalEnd), strokeWidth = CanalStrokeWidth)
    drawLine(canalColor, Offset(cx + w * 0.02f, cervicalY + 4), Offset(cx + w * 0.05f, canalEnd), strokeWidth = CanalStrokeWidth)
    drawLine(canalColor, Offset(cx + w * 0.185f, cervicalY + 4), Offset(cx + w * 0.30f, canalEnd), strokeWidth = CanalStrokeWidth)
}

private fun DrawScope.drawPremolarRoots(
    cx: Float, cervicalY: Float, w: Float, rootH: Float,
    canalColor: Color = CanalColor, canalFraction: Float = 1f,
    rootFillColor: Color = RootColor
) {
    val outline = DarkOutline
    val stroke = Stroke(width = 1.5f)
    val rootW = w * 0.28f
    val gap = w * 0.16f

    val mesial = Path().apply {
        moveTo(cx - gap / 2 - rootW, cervicalY)
        lineTo(cx - gap / 2, cervicalY)
        cubicTo(cx - gap / 2, cervicalY + rootH * 0.80f, cx - gap / 2, cervicalY + rootH * 0.92f, cx - gap / 2 - rootW / 2, cervicalY + rootH * 0.88f)
        cubicTo(cx - gap / 2 - rootW, cervicalY + rootH * 0.92f, cx - gap / 2 - rootW, cervicalY + rootH * 0.80f, cx - gap / 2 - rootW, cervicalY)
        close()
    }
    drawPath(mesial, rootFillColor); drawPath(mesial, outline, style = stroke)

    val distal = Path().apply {
        moveTo(cx + gap / 2, cervicalY)
        lineTo(cx + gap / 2 + rootW, cervicalY)
        cubicTo(cx + gap / 2 + rootW, cervicalY + rootH * 0.80f, cx + gap / 2 + rootW, cervicalY + rootH * 0.92f, cx + gap / 2 + rootW / 2, cervicalY + rootH * 0.88f)
        cubicTo(cx + gap / 2, cervicalY + rootH * 0.92f, cx + gap / 2, cervicalY + rootH * 0.80f, cx + gap / 2, cervicalY)
        close()
    }
    drawPath(distal, rootFillColor); drawPath(distal, outline, style = stroke)

    val canalEnd = cervicalY + rootH * 0.82f * canalFraction
    drawLine(canalColor, Offset(cx - gap / 2 - rootW / 2, cervicalY + 4), Offset(cx - gap / 2 - rootW / 2, canalEnd), strokeWidth = CanalStrokeWidth)
    drawLine(canalColor, Offset(cx + gap / 2 + rootW / 2, cervicalY + 4), Offset(cx + gap / 2 + rootW / 2, canalEnd), strokeWidth = CanalStrokeWidth)
}

private fun DrawScope.drawSingleRoot(
    cx: Float, cervicalY: Float, w: Float, rootH: Float,
    canalColor: Color = CanalColor, canalFraction: Float = 1f,
    rootFillColor: Color = RootColor
) {
    val rootW = w * 0.35f
    val rootPath = Path().apply {
        moveTo(cx - rootW, cervicalY)
        cubicTo(cx - rootW * 0.95f, cervicalY + rootH * 0.15f, cx - rootW * 0.8f, cervicalY + rootH * 0.4f, cx - rootW * 0.5f, cervicalY + rootH * 0.7f)
        cubicTo(cx - rootW * 0.3f, cervicalY + rootH * 0.9f, cx, cervicalY + rootH * 0.95f, cx + rootW * 0.3f, cervicalY + rootH * 0.7f)
        cubicTo(cx + rootW * 0.8f, cervicalY + rootH * 0.4f, cx + rootW * 0.95f, cervicalY + rootH * 0.15f, cx + rootW, cervicalY)
        close()
    }
    drawPath(rootPath, rootFillColor)
    drawPath(rootPath, DarkOutline, style = Stroke(width = 1.5f))

    val canalEnd = cervicalY + rootH * 0.82f * canalFraction
    drawLine(canalColor, Offset(cx, cervicalY + 4), Offset(cx, canalEnd), strokeWidth = CanalStrokeWidth)
}

private fun DrawScope.drawPalatalRootFilled(
    cx: Float, cervicalY: Float, w: Float, rootH: Float,
    toothType: ToothType, fillColor: Color
) {
    val outline = DarkOutline
    val stroke = Stroke(width = 1.5f)
    when (toothType) {
        ToothType.UPPER_MOLAR -> {
            val palatal = Path().apply {
                moveTo(cx - w * 0.16f, cervicalY)
                cubicTo(cx - w * 0.15f, cervicalY + rootH * 0.25f, cx - w * 0.08f, cervicalY + rootH * 0.55f, cx - w * 0.02f, cervicalY + rootH * 0.78f)
                cubicTo(cx + w * 0.03f, cervicalY + rootH * 0.93f, cx + w * 0.10f, cervicalY + rootH * 0.93f, cx + w * 0.08f, cervicalY + rootH * 0.78f)
                cubicTo(cx + w * 0.06f, cervicalY + rootH * 0.55f, cx + w * 0.13f, cervicalY + rootH * 0.25f, cx + w * 0.16f, cervicalY)
                close()
            }
            drawPath(palatal, fillColor); drawPath(palatal, outline, style = stroke)
        }
        ToothType.UPPER_PREMOLAR -> {
            val distal = Path().apply {
                moveTo(cx + w * 0.08f, cervicalY)
                cubicTo(cx + w * 0.10f, cervicalY + rootH * 0.20f, cx + w * 0.15f, cervicalY + rootH * 0.45f, cx + w * 0.20f, cervicalY + rootH * 0.65f)
                cubicTo(cx + w * 0.24f, cervicalY + rootH * 0.84f, cx + w * 0.34f, cervicalY + rootH * 0.88f, cx + w * 0.30f, cervicalY + rootH * 0.65f)
                cubicTo(cx + w * 0.26f, cervicalY + rootH * 0.45f, cx + w * 0.22f, cervicalY + rootH * 0.20f, cx + w * 0.22f, cervicalY)
                close()
            }
            drawPath(distal, fillColor); drawPath(distal, outline, style = stroke)
        }
        ToothType.LOWER_MOLAR -> {
            val mesial = Path().apply {
                moveTo(cx - w * 0.28f, cervicalY)
                cubicTo(cx - w * 0.30f, cervicalY + rootH * 0.25f, cx - w * 0.28f, cervicalY + rootH * 0.50f, cx - w * 0.24f, cervicalY + rootH * 0.72f)
                cubicTo(cx - w * 0.22f, cervicalY + rootH * 0.90f, cx - w * 0.12f, cervicalY + rootH * 0.92f, cx - w * 0.08f, cervicalY + rootH * 0.72f)
                cubicTo(cx - w * 0.05f, cervicalY + rootH * 0.50f, cx - w * 0.08f, cervicalY + rootH * 0.25f, cx - w * 0.12f, cervicalY)
                close()
            }
            drawPath(mesial, fillColor); drawPath(mesial, outline, style = stroke)
        }
        else -> {
            val rootW = w * 0.35f
            val single = Path().apply {
                moveTo(cx - rootW, cervicalY)
                cubicTo(cx - rootW * 0.95f, cervicalY + rootH * 0.15f, cx - rootW * 0.8f, cervicalY + rootH * 0.4f, cx - rootW * 0.5f, cervicalY + rootH * 0.7f)
                cubicTo(cx - rootW * 0.3f, cervicalY + rootH * 0.9f, cx, cervicalY + rootH * 0.95f, cx + rootW * 0.3f, cervicalY + rootH * 0.7f)
                cubicTo(cx + rootW * 0.8f, cervicalY + rootH * 0.4f, cx + rootW * 0.95f, cervicalY + rootH * 0.15f, cx + rootW, cervicalY)
                close()
            }
            drawPath(single, fillColor); drawPath(single, outline, style = stroke)
        }
    }
}

// Crown fill (lighter version for interior)
private fun DrawScope.drawCrownFill(
    cx: Float, y: Float, w: Float, crownH: Float, cervicalY: Float,
    toothType: ToothType, number: Int, fillColor: Color
) {
    val dims = crownDimensions(w, crownH, toothType, number)
    val crownPath = buildCrownPath(cx, y, w, crownH, cervicalY, toothType, number, dims)
    drawPath(crownPath, fillColor)
}

// Crown outline only
private fun DrawScope.drawCrownOutline(
    cx: Float, y: Float, w: Float, crownH: Float, cervicalY: Float,
    toothType: ToothType, number: Int, outlineColor: Color
) {
    val dims = crownDimensions(w, crownH, toothType, number)
    val crownPath = buildCrownPath(cx, y, w, crownH, cervicalY, toothType, number, dims)
    drawPath(crownPath, outlineColor, style = Stroke(width = 2f))
}

private data class CrownDims(
    val maxW: Float, val bottomW: Float, val midW: Float, val topW: Float
)

private fun crownDimensions(w: Float, crownH: Float, toothType: ToothType, number: Int): CrownDims {
    val isCanine = number == 13 || number == 23 || number == 33 || number == 43
    val isCentralIncisor = number == 11 || number == 21 || number == 31 || number == 41
    val isLateralIncisor = number == 12 || number == 22 || number == 32 || number == 42

    return when (toothType) {
        ToothType.UPPER_MOLAR, ToothType.LOWER_MOLAR ->
            CrownDims(w * 0.88f, w * 0.58f, w * 0.84f, w * 0.50f)
        ToothType.UPPER_PREMOLAR, ToothType.LOWER_PREMOLAR -> {
            if (toothType == ToothType.LOWER_PREMOLAR)
                CrownDims(w * 0.88f, w * 0.70f, w * 0.84f, w * 0.38f)
            else
                CrownDims(w * 0.88f, w * 0.72f, w * 0.84f, w * 0.50f)
        }
        ToothType.UPPER_ANTERIOR, ToothType.LOWER_ANTERIOR -> {
            when {
                isCanine -> CrownDims(w * 0.76f, w * 0.70f, w * 0.74f, w * 0.14f)
                isCentralIncisor -> {
                    if (toothType == ToothType.UPPER_ANTERIOR)
                        CrownDims(w * 0.92f, w * 0.70f, w * 0.86f, w * 0.90f)
                    else
                        CrownDims(w * 0.86f, w * 0.70f, w * 0.76f, w * 0.84f)
                }
                isLateralIncisor -> CrownDims(w * 0.86f, w * 0.70f, w * 0.76f, w * 0.84f)
                else -> CrownDims(w * 0.55f, w * 0.34f, w * 0.46f, w * 0.55f * 0.70f)
            }
        }
        else -> CrownDims(w * 0.6f, w * 0.45f, w * 0.52f, w * 0.30f)
    }
}

private fun buildCrownPath(
    cx: Float, y: Float, w: Float, crownH: Float, cervicalY: Float,
    toothType: ToothType, number: Int, dims: CrownDims
): Path {
    val isMolar = toothType == ToothType.UPPER_MOLAR || toothType == ToothType.LOWER_MOLAR
    val isPremolar = toothType == ToothType.UPPER_PREMOLAR || toothType == ToothType.LOWER_PREMOLAR
    val isCentralIncisor = number == 11 || number == 21 || number == 31 || number == 41
    val isLateralIncisor = number == 12 || number == 22 || number == 32 || number == 42
    val isIncisor = isCentralIncisor || isLateralIncisor
    val hasSharpCusp = number == 34
    val topW = dims.topW
    val midW = dims.midW
    val bottomW = dims.bottomW

    return Path().apply {
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
}

// ---- Existing drawing functions for non-ANATOMICAL layers ----

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
