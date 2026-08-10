package com.dental.ui.odontogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

internal data class CrownOptionInfo(
    val option: CrownOption,
    val label: String,
    val abbreviation: String,
    val color: Color
)

internal data class RootOptionInfo(
    val option: RootOption,
    val label: String,
    val abbreviation: String,
    val color: Color
)

internal val crownOptionsList = listOf(
    CrownOptionInfo(CrownOption.MISSING, "Зуб отсутствует", "", Color(0xFFE0E0E0)),
    CrownOptionInfo(CrownOption.METAL_CERAMIC, "Металлокерамическая коронка", "МК", Color(0xFF4CAF50)),
    CrownOptionInfo(CrownOption.CAST_SOLID, "Цельнолитая коронка", "ЦЛ", Color(0xFF424242)),
    CrownOptionInfo(CrownOption.ZIRCONIUM_OXIDE, "Оксид циркониевая коронка", "ОЦ", Color(0xFF03A9F4)),
    CrownOptionInfo(CrownOption.FULL_CERAMIC, "Цельнокерамическая коронка", "ЦК", Color(0xFF8BC34A)),
    CrownOptionInfo(CrownOption.IMPLANT_CROWN, "Коронка на имплантате", "КИМ", Color(0xFF9C27B0)),
    CrownOptionInfo(CrownOption.TEMPORARY, "Временная коронка", "ВК", Color(0xFFFF9800)),
    CrownOptionInfo(CrownOption.ARTIFICIAL_REMOVABLE, "Зуб в Съёмном протезе", "И", Color(0xFFFF80AB)),
    CrownOptionInfo(CrownOption.PLOMBA, "Пломба", "П", Color(0xFFCE93D8))
)

internal val rootOptionsList = listOf(
    RootOptionInfo(RootOption.MISSING, "Корень отсутствует", "", Color(0xFFE0E0E0)),
    RootOptionInfo(RootOption.POST_CORE, "Культевая вкладка", "КВ", Color(0xFF616161)),
    RootOptionInfo(RootOption.ANCHOR_PIN, "Анкерный штифт", "АШ", Color(0xFFFF9800)),
    RootOptionInfo(RootOption.ENDO_TREATED, "Эндодонтически пролеченный зуб", "", Color(0xFF42A5F5)),
    RootOptionInfo(RootOption.ENDO_PROBLEM, "Эндодонтическая проблема", "", Color(0xFFEF5350)),
    RootOptionInfo(RootOption.PERIO_PROBLEM, "Пародонтологическая проблема", "ПП", Color(0xFFE53935)),
    RootOptionInfo(RootOption.IMPLANT, "Имплантат", "", Color(0xFF9C27B0)),
    RootOptionInfo(RootOption.RETAINED, "Ретинированный зуб", "РЕТ", Color(0xFFEF9A9A))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToothPartSheet(
    toothNumber: Int,
    part: ToothPart,
    onDismiss: () -> Unit,
    onApplyCrown: (CrownOption) -> Unit,
    onApplyRoot: (RootOption) -> Unit,
    onApplyIntact: () -> Unit = {}
) {
    var selectedCrown by remember { mutableStateOf<CrownOption?>(null) }
    var selectedRoot by remember { mutableStateOf<RootOption?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Зуб №${toothNumber} — ${if (part == ToothPart.CROWN) "Коронка" else "Корень"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Выберите позицию:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (part == ToothPart.CROWN) selectedCrown?.let { onApplyCrown(it) }
                        else selectedRoot?.let { onApplyRoot(it) }
                    },
                    enabled = if (part == ToothPart.CROWN) selectedCrown != null else selectedRoot != null,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Подтвердить", fontSize = 12.sp, color = Color.White)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                ) {
                    Text("Отмена", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = onApplyIntact,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF2E7D32)
                )
            ) {
                Text(
                    text = if (part == ToothPart.CROWN) "Интактный зуб (без коронки и пломб)" else "Интактный корень",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (part == ToothPart.CROWN) {
                    crownOptionsList.forEach { item ->
                        CompactOption(
                            label = item.label,
                            abbreviation = item.abbreviation,
                            color = item.color,
                            isSelected = selectedCrown == item.option,
                            onSelect = {
                                selectedCrown = if (selectedCrown == item.option) null else item.option
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    rootOptionsList.forEach { item ->
                        CompactOption(
                            label = item.label,
                            abbreviation = item.abbreviation,
                            color = item.color,
                            isSelected = selectedRoot == item.option,
                            onSelect = {
                                selectedRoot = if (selectedRoot == item.option) null else item.option
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOption(
    label: String,
    abbreviation: String,
    color: Color,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        modifier = modifier.padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = color,
                    uncheckedColor = color.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
