package com.dental.ui.odontogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dental.model.*

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
    var step by remember { mutableStateOf(0) }
    var selectedType by remember { mutableStateOf<ProstheticType?>(null) }
    var selectedMaterial by remember { mutableStateOf<ProstheticMaterial?>(null) }
    var selectedStage by remember { mutableStateOf(ProstheticStage.PLANNED) }

    val existing = existingProsthetics.firstOrNull { it.toothIds.contains(toothNumber) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Tooth #${toothNumber}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (existing != null) {
                Text(
                    text = "Current: ${existing.type.name} (${existing.stage.name})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Status: ${toothStatus.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Tooth status bar
            Text("Tooth status:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToothStatus.entries.forEach { status ->
                    FilterChip(
                        selected = toothStatus == status,
                        onClick = { onChangeToothStatus(status) },
                        label = { Text(status.name, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Prosthetics:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            when (step) {
                0 -> TypeSelectionStep(
                    selectedType = selectedType,
                    isBridgeMode = isBridgeMode,
                    onSelect = { type ->
                        selectedType = type
                        if (type == ProstheticType.REMOVAL) {
                            onSelectProsthetic(ProstheticType.REMOVAL, ProstheticMaterial.COMPOSITE, ProstheticStage.COMPLETED)
                            onDismiss()
                        } else if (type == ProstheticType.BRIDGE && !isBridgeMode) {
                            onToggleBridgeMode()
                            onDismiss()
                        } else {
                            step = 1
                        }
                    }
                )
                1 -> MaterialSelectionStep(
                    onSelect = { material ->
                        selectedMaterial = material
                        step = 2
                    },
                    onBack = { step = 0 }
                )
                2 -> StageSelectionStep(
                    onSelect = { stage ->
                        selectedStage = stage
                        selectedType?.let { type ->
                            selectedMaterial?.let { material ->
                                onSelectProsthetic(type, material, stage)
                                onDismiss()
                            }
                        }
                    },
                    onBack = { step = 1 }
                )
            }
        }
    }
}

@Composable
private fun TypeSelectionStep(
    selectedType: ProstheticType?,
    isBridgeMode: Boolean,
    onSelect: (ProstheticType) -> Unit
) {
    Text("Select type:", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))

    val types = listOf(
        TypeOption(ProstheticType.CROWN, "Crown", "\u2B24"),
        TypeOption(ProstheticType.BRIDGE, "Bridge", "\u2550"),
        TypeOption(ProstheticType.IMPLANT, "Implant", "\u2699"),
        TypeOption(ProstheticType.POST_CORE, "Post/Core", "\u29B6"),
        TypeOption(ProstheticType.PONTIC, "Pontic", "\u25A3"),
        TypeOption(ProstheticType.TEMPORARY, "Temporary", "\u23F1"),
        TypeOption(ProstheticType.REMOVAL, "Remove", "\u2716"),
    )

    types.forEach { option ->
        ElevatedButton(
            onClick = { onSelect(option.type) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = if (selectedType == option.type) {
                ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            } else {
                ButtonDefaults.elevatedButtonColors()
            }
        ) {
            Text(option.icon, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Text(option.label, modifier = Modifier.weight(1f))
        }
    }

    if (isBridgeMode) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap first and last abutment tooth",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MaterialSelectionStep(
    onSelect: (ProstheticMaterial) -> Unit,
    onBack: () -> Unit
) {
    Text("Select material:", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))

    val materials = listOf(
        MaterialOption(ProstheticMaterial.METAL_CERAMIC, "MC", Color(0xFF757575)),
        MaterialOption(ProstheticMaterial.ZIRCONIUM, "Zr", Color(0xFF7B1FA2)),
        MaterialOption(ProstheticMaterial.METAL, "Metal", Color(0xFFBDBDBD)),
        MaterialOption(ProstheticMaterial.COMPOSITE, "Composite", Color(0xFFFFF9C4)),
        MaterialOption(ProstheticMaterial.CERAMIC, "Ceramic", Color(0xFFF3E5F5))
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        materials.forEach { option ->
            ElevatedButton(
                onClick = { onSelect(option.material) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = option.color.copy(alpha = 0.2f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(option.color, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(12.dp))
                Text(option.label, modifier = Modifier.weight(1f))
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onBack) { Text("Back") }
}

@Composable
private fun StageSelectionStep(
    onSelect: (ProstheticStage) -> Unit,
    onBack: () -> Unit
) {
    Text("Select stage:", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))

    val stages = listOf(
        StageOption(ProstheticStage.EXISTING, "Existing", "\u25CB"),
        StageOption(ProstheticStage.PLANNED, "Planned", "\u25CC"),
        StageOption(ProstheticStage.IN_PROGRESS, "In progress", "\u23F3"),
        StageOption(ProstheticStage.COMPLETED, "Completed", "\u2713")
    )

    stages.forEach { option ->
        ElevatedButton(
            onClick = { onSelect(option.stage) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(option.icon, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Text(option.label, modifier = Modifier.weight(1f))
        }
    }

    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onBack) { Text("Back") }
}

private data class TypeOption(val type: ProstheticType, val label: String, val icon: String)
private data class MaterialOption(val material: ProstheticMaterial, val label: String, val color: Color)
private data class StageOption(val stage: ProstheticStage, val label: String, val icon: String)
