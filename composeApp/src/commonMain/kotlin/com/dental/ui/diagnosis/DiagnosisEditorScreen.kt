package com.dental.ui.diagnosis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dental.model.Diagnosis
import com.dental.model.PREDEFINED_DIAGNOSES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisEditorScreen(
    patientId: Long,
    existingDiagnoses: List<Diagnosis>,
    onSave: (List<Diagnosis>) -> Unit,
    onBack: () -> Unit
) {
    // Инициализируем состояние: чеки и номера зубов из существующих диагнозов
    val initialChecked = remember(existingDiagnoses) {
        existingDiagnoses.map { it.diagnosisText }.toSet()
    }
    val initialToothNumbers = remember(existingDiagnoses) {
        existingDiagnoses.associate { it.diagnosisText to (it.toothNumber?.takeIf { n -> n.isNotBlank() }) }
    }

    var checkedItems by remember { mutableStateOf(initialChecked) }
    var toothNumbers by remember { mutableStateOf<Map<String, String?>>(initialToothNumbers) }

    val isDirty = checkedItems != initialChecked || toothNumbers != initialToothNumbers

    fun buildDiagnoses(): List<Diagnosis> {
        return PREDEFINED_DIAGNOSES
            .filter { it.text in checkedItems }
            .map { pred ->
                Diagnosis(
                    patientId = patientId,
                    code = pred.code,
                    diagnosisText = pred.text,
                    toothNumber = toothNumbers[pred.text]
                )
            }
    }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    fun requestExit() {
        if (isDirty) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор диагнозов") },
                navigationIcon = {
                    IconButton(onClick = { requestExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onSave(buildDiagnoses()) },
                icon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text("Сохранить") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Зуб",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = "Диагноз",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            itemsIndexed(PREDEFINED_DIAGNOSES) { _, pred ->
                val isChecked = pred.text in checkedItems

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = (toothNumbers[pred.text] ?: ""),
                        onValueChange = { value ->
                            toothNumbers = toothNumbers + (pred.text to value)
                        },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        enabled = isChecked,
                        placeholder = { Text("№", fontSize = 12.sp) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                        if (pred.code != null) {
                            Text(
                                text = "${pred.code} — ${pred.text}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp
                            )
                        } else {
                            Text(
                                text = pred.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Сохранить изменения?") },
            text = { Text("Внесённые изменения не сохранены. Сохранить их перед выходом?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        onSave(buildDiagnoses())
                        onBack()
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showUnsavedDialog = false
                            onBack()
                        }
                    ) { Text("Не сохранять") }
                    TextButton(onClick = { showUnsavedDialog = false }) { Text("Отмена") }
                }
            }
        )
    }
}
