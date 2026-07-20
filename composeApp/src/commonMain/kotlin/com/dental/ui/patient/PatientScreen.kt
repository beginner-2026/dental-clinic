package com.dental.ui.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dental.model.Patient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientScreen(
    patients: List<Patient>,
    onAddPatient: () -> Unit,
    onDeletePatient: (Long) -> Unit,
    onReload: () -> Unit,
    onPatientClick: (Patient) -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<Patient?>(null) }

    var deleteMode by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пациенты") },
                actions = {
                    IconButton(onClick = { deleteMode = !deleteMode }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Режим удаления",
                            tint = if (deleteMode) Color(0xFFE53935) else LocalContentColor.current
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPatient) {
                Icon(Icons.Default.Add, contentDescription = "Добавить пациента")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск пациентов...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            val filteredPatients = if (searchQuery.isBlank()) patients
            else patients.filter {
                it.lastName.contains(searchQuery, ignoreCase = true) ||
                it.firstName.contains(searchQuery, ignoreCase = true) ||
                it.middleName?.contains(searchQuery, ignoreCase = true) == true ||
                it.phone?.contains(searchQuery) == true
            }

            LazyColumn {
                items(filteredPatients, key = { it.id }) { patient ->
                    PatientCard(
                        patient = patient,
                        deleteMode = deleteMode,
                        onDelete = { showDeleteConfirm = patient },
                        onClick = { onPatientClick(patient) }
                    )
                }
            }
        }
    }

    showDeleteConfirm?.let { patient ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Удалить пациента") },
            text = {
                Text("Вы уверены, что хотите удалить ${patient.lastName} ${patient.firstName}? Все данные пациента будут удалены.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePatient(patient.id)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun PatientCard(patient: Patient, deleteMode: Boolean = false, onDelete: () -> Unit, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(patient.lastName)
                        append(" ")
                        append(patient.firstName)
                        if (!patient.middleName.isNullOrBlank()) {
                            append(" ")
                            append(patient.middleName)
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

            }
            if (deleteMode) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}
