package com.dental.ui.odontogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dental.model.*

private val GreenColor = Color(0xFF4CAF50)
private val BlackColor = Color(0xFF212121)
private val LightBlueColor = Color(0xFF03A9F4)
private val PinkColor = Color(0xFFE91E63)
private val OrangeColor = Color(0xFFFF9800)

private val constructionTypes = listOf(
    ConstructionType("МКК", "Металлокерамическая коронка", GreenColor, ProstheticType.CROWN, ProstheticMaterial.METAL_CERAMIC),
    ConstructionType("ЦЛК", "Цельнолитая коронка", BlackColor, ProstheticType.CROWN, ProstheticMaterial.METAL),
    ConstructionType("ОЦК", "Коронка из диоксида циркония", LightBlueColor, ProstheticType.CROWN, ProstheticMaterial.ZIRCONIUM),
    ConstructionType("ЦКК", "Цельнокерамическая коронка", PinkColor, ProstheticType.CROWN, ProstheticMaterial.CERAMIC),
    ConstructionType("Искусственный зуб МК", "Металлокерамический искусственный зуб", GreenColor, ProstheticType.PONTIC, ProstheticMaterial.METAL_CERAMIC),
    ConstructionType("Искусственный зуб ЦЛ", "Цельнолитой искусственный зуб", BlackColor, ProstheticType.PONTIC, ProstheticMaterial.METAL),
    ConstructionType("Временная коронка", "Временная коронка", OrangeColor, ProstheticType.TEMPORARY, ProstheticMaterial.COMPOSITE)
)

private data class ConstructionType(
    val label: String,
    val description: String,
    val color: Color,
    val prostheticType: ProstheticType,
    val prostheticMaterial: ProstheticMaterial
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToothActionSheet(
    toothNumber: Int,
    toothStatus: ToothStatus,
    existingProsthetics: List<ProstheticItem>,
    isBridgeMode: Boolean,
    onDismiss: () -> Unit,
    onSelectProsthetic: (ProstheticType, ProstheticMaterial, ProstheticStage) -> Unit,
    onToggleBridgeMode: () -> Unit,
    onChangeToothStatus: (ToothStatus) -> Unit
) {
    val existing = existingProsthetics.firstOrNull { it.toothIds.contains(toothNumber) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Зуб №${toothNumber}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (existing != null) {
                val name = constructionTypes.find {
                    it.prostheticType == existing.type && it.prostheticMaterial == existing.material
                }?.label ?: existing.type.name
                Text(
                    text = "Текущее: $name (${stageName(existing.stage)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))

            Text("Выберите конструкцию:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            constructionTypes.forEach { item ->
                ElevatedButton(
                    onClick = {
                        val stage = ProstheticStage.COMPLETED
                        if (item.prostheticType == ProstheticType.PONTIC) {
                            onChangeToothStatus(ToothStatus.MISSING)
                        } else {
                            onChangeToothStatus(ToothStatus.PRESENT)
                        }
                        onSelectProsthetic(item.prostheticType, item.prostheticMaterial, stage)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = item.color.copy(alpha = 0.12f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(item.color, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Remove existing prosthetic
            if (existing != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onSelectProsthetic(ProstheticType.REMOVAL, ProstheticMaterial.COMPOSITE, ProstheticStage.COMPLETED)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить конструкцию")
                }
            }
        }
    }
}

private fun stageName(stage: ProstheticStage): String = when (stage) {
    ProstheticStage.EXISTING -> "существующая"
    ProstheticStage.PLANNED -> "запланировано"
    ProstheticStage.IN_PROGRESS -> "в работе"
    ProstheticStage.COMPLETED -> "выполнено"
}
