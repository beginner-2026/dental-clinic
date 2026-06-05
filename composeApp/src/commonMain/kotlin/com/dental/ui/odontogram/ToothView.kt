package com.dental.ui.odontogram

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dental.model.*

object ToothColors {
    val intact = Color(0xFFFFFFFF)
    val missing = Color(0xFFE0E0E0)
    val implantRoot = Color(0xFF9E9E9E)
    val implantMark = Color(0xFF42A5F5)

    val crownMetal = Color(0xFFBDBDBD)
    val crownMCBg = Color(0xFFF5F5F5)
    val crownMCBorder = Color(0xFF757575)
    val crownZrBg = Color(0xFFF3E5F5)
    val crownZrBorder = Color(0xFF7B1FA2)
    val crownTemp = Color(0xFFFFF9C4)

    val bridgeArc = Color(0xFF66BB6A)
    val plannedBorder = Color(0xFF64B5F6)
    val inProgressMark = Color(0xFFFFA000)
    val completedMark = Color(0xFF4CAF50)
    val existingBorder = Color(0xFF424242)

    val selectedBorder = Color(0xFF1565C0)
}

@Composable
fun ToothView(
    number: Int,
    tooth: Tooth?,
    prostheticItems: List<ProstheticItem>,
    isSelected: Boolean,
    isBridgeEnd: Boolean,
    stage: ProstheticStage,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    small: Boolean = false
) {
    val toothStatus = tooth?.status ?: ToothStatus.PRESENT
    val prosthetic = prostheticItems.firstOrNull { number in it.toothIds }
    val bgColor = computeBgColor(toothStatus, prosthetic, stage)
    val borderC = if (isSelected) ToothColors.selectedBorder else computeBorderColor(prosthetic, stage)
    val borderW = if (isSelected) 2.dp else 1.dp
    val isDashed = stage == ProstheticStage.PLANNED

    val toothSize = if (small) 32.dp else size

    Box(
        modifier = Modifier
            .size(toothSize)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .customBorder(
                width = borderW,
                color = borderC,
                dashed = isDashed
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            toothStatus == ToothStatus.MISSING && prosthetic == null -> {
                Text("X", fontSize = if (small) 10.sp else 14.sp, color = Color.Gray)
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${number % 10}",
                        fontSize = if (small) 9.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (!small && prosthetic != null) {
                        when (prosthetic.type) {
                            ProstheticType.IMPLANT -> {
                                Text("\u2699", fontSize = 10.sp, color = ToothColors.implantRoot)
                                Text("Imp", fontSize = 7.sp, color = ToothColors.implantMark)
                            }
                            ProstheticType.TEMPORARY -> {
                                Text("Temp", fontSize = 7.sp, color = Color(0xFFF57F17))
                            }
                            else -> {
                                if (prosthetic.material == ProstheticMaterial.METAL) {
                                    Text("M", fontSize = 7.sp, color = Color(0xFF616161))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (stage == ProstheticStage.IN_PROGRESS) {
            Text("\u23F3", fontSize = if (small) 8.sp else 10.sp,
                modifier = Modifier.align(Alignment.TopEnd))
        }
        if (stage == ProstheticStage.COMPLETED) {
            Text("\u2713", fontSize = if (small) 8.sp else 10.sp,
                color = ToothColors.completedMark, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd))
        }
        if (isBridgeEnd) {
            Text("\u25CF", fontSize = 6.sp, color = ToothColors.bridgeArc,
                modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun computeBgColor(
    status: ToothStatus,
    prosthetic: ProstheticItem?,
    stage: ProstheticStage
): Color {
    if (status == ToothStatus.MISSING) return ToothColors.missing
    if (status == ToothStatus.IMPLANT) return Color(0xFFFFF3E0)

    val p = prosthetic ?: return ToothColors.intact
    if (p.type == ProstheticType.TEMPORARY) return ToothColors.crownTemp
    if (p.type == ProstheticType.IMPLANT) return Color(0xFFFFF3E0)

    return when (p.material) {
        ProstheticMaterial.METAL -> ToothColors.crownMetal
        ProstheticMaterial.METAL_CERAMIC -> ToothColors.crownMCBg
        ProstheticMaterial.ZIRCONIUM -> ToothColors.crownZrBg
        else -> ToothColors.intact
    }
}

private fun computeBorderColor(
    prosthetic: ProstheticItem?,
    stage: ProstheticStage
): Color {
    return when (stage) {
        ProstheticStage.PLANNED -> ToothColors.plannedBorder
        ProstheticStage.IN_PROGRESS -> ToothColors.inProgressMark
        ProstheticStage.COMPLETED -> ToothColors.completedMark
        ProstheticStage.EXISTING -> {
            val p = prosthetic
            if (p != null && p.material == ProstheticMaterial.METAL_CERAMIC) ToothColors.crownMCBorder
            else if (p != null && p.material == ProstheticMaterial.ZIRCONIUM) ToothColors.crownZrBorder
            else if (p?.type == ProstheticType.IMPLANT) ToothColors.implantMark
            else ToothColors.existingBorder
        }
    }
}
