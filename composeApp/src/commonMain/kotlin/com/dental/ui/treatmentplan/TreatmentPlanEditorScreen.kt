package com.dental.ui.treatmentplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dental.model.PREDEFINED_PROCEDURES
import com.dental.model.TreatmentPlanItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentPlanEditorScreen(
    patientId: Long,
    existingItems: List<TreatmentPlanItem>,
    onSave: (List<TreatmentPlanItem>) -> Unit,
    onBack: () -> Unit
) {
    val initialChecked = remember(existingItems) {
        existingItems.map { it.procedure }.toSet()
    }
    val initialToothNumbers = remember(existingItems) {
        existingItems.associate { it.procedure to it.toothNumbers }
    }

    var checkedItems by remember { mutableStateOf(initialChecked) }
    var toothNumbers by remember { mutableStateOf(initialToothNumbers) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор плана лечения") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val items = PREDEFINED_PROCEDURES
                                .filter { it.text in checkedItems }
                                .map { pred ->
                                    TreatmentPlanItem(
                                        patientId = patientId,
                                        toothNumbers = toothNumbers[pred.text] ?: "",
                                        procedure = pred.text
                                    )
                                }
                            onSave(items)
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Номер зуба",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(90.dp)
                    )
                    Text(
                        text = "Процесс",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            itemsIndexed(PREDEFINED_PROCEDURES) { _, pred ->
                val isChecked = pred.text in checkedItems

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = toothNumbers[pred.text] ?: "",
                        onValueChange = { value ->
                            toothNumbers = toothNumbers + (pred.text to value)
                        },
                        modifier = Modifier.width(90.dp),
                        singleLine = true,
                        enabled = isChecked,
                        placeholder = { Text("№", fontSize = 12.sp) },
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.width(4.dp))

                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            checkedItems = if (checked) {
                                checkedItems + pred.text
                            } else {
                                checkedItems - pred.text
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    )

                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            text = pred.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
