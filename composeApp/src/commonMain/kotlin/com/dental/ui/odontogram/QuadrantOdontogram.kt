package com.dental.ui.odontogram

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dental.model.*

private data class QuadrantDef(
    val label: String,
    val toothNumbers: List<Int>
)

@Composable
fun QuadrantOdontogram(
    teeth: List<Tooth>,
    prostheticItems: List<ProstheticItem>,
    selectedTooth: Int?,
    activeLayer: OdontogramLayer,
    onToothClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val quadrants = listOf(
        QuadrantDef("К1 — верхний правый", (18 downTo 11).toList()),
        QuadrantDef("К2 — верхний левый", (21..28).toList()),
        QuadrantDef("К3 — нижний левый", (31..38).toList()),
        QuadrantDef("К4 — нижний правый", (48 downTo 41).toList())
    )

    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        TabRow(selectedTabIndex = selectedIndex) {
            quadrants.forEachIndexed { index, q ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    text = { Text(q.label.take(2)) }
                )
            }
        }

        val q = quadrants[selectedIndex]
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = q.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArchCanvas(
                        teeth = teeth,
                        prostheticItems = prostheticItems,
                        selectedTooth = selectedTooth,
                        layerType = LayerType.ANATOMICAL,
                        toothNumbers = q.toothNumbers,
                        onToothClick = onToothClick,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                    ToothNumbering(
                        toothNumbers = q.toothNumbers,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
