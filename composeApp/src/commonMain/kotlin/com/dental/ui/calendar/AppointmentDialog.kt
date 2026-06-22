package com.dental.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dental.model.*
import kotlin.math.roundToInt
import kotlinx.datetime.*

@Composable
fun AppointmentDialog(
    appointment: Appointment?,
    selectedTimeSlot: Long?,
    patients: List<Patient>,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit,
    onDelete: (Long) -> Unit
) {
    val isEdit = appointment != null
    val startTime = selectedTimeSlot ?: appointment?.startTime ?: 0L
    val startLocal = if (startTime > 0) {
        Instant.fromEpochMilliseconds(startTime).toLocalDateTime(TimeZone.UTC)
    } else null

    var type by remember { mutableStateOf(appointment?.type ?: AppointmentType.INITIAL) }
    var status by remember { mutableStateOf(appointment?.status ?: AppointmentStatus.PLANNED) }
    var durationMinutes by remember { mutableStateOf(appointment?.durationMinutes ?: 30) }
    var note by remember { mutableStateOf(appointment?.note ?: "") }
    var selectedPatient by remember { mutableStateOf(patients.find { it.id == appointment?.patientId }) }
    var searchQuery by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }

    val filteredPatients = remember(searchQuery) {
        if (searchQuery.isBlank()) patients
        else patients.filter {
            "${it.lastName} ${it.firstName} ${it.middleName ?: ""}".contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(searchQuery, filteredPatients) {
        if (searchQuery.isNotBlank() && selectedPatient == null && filteredPatients.size == 1) {
            selectedPatient = filteredPatients[0]
            searchQuery = "${filteredPatients[0].lastName} ${filteredPatients[0].firstName}"
            showDropdown = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Редактировать запись" else "Новая запись") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (startLocal != null) {
                    Text(
                        text = "Время: %02d:%02d".format(startLocal.hour, startLocal.minute),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Patient search
                Text("Пациент:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectedPatient?.let { "${it.lastName} ${it.firstName}" } ?: searchQuery,
                    onValueChange = {
                        searchQuery = it
                        selectedPatient = null
                        showDropdown = it.isNotEmpty()
                    },
                    label = { Text("Поиск пациента") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isEdit
                )
                if (showDropdown && filteredPatients.isNotEmpty() && selectedPatient == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            filteredPatients.take(10).forEach { patient ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPatient = patient
                                            searchQuery = "${patient.lastName} ${patient.firstName}"
                                            showDropdown = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text("${patient.lastName} ${patient.firstName} ${patient.middleName ?: ""}")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Duration
                Text(
                    text = "Длительность: ${durationMinutes}мин",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = durationMinutes.toFloat(),
                    onValueChange = { durationMinutes = (it / 30f).roundToInt() * 30 },
                    valueRange = 30f..240f,
                    steps = 7
                )

                Spacer(Modifier.height(8.dp))

                // Type selector
                Text("Тип:", style = MaterialTheme.typography.bodyMedium)
                Row {
                    AppointmentType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Status selector
                Text("Статус:", style = MaterialTheme.typography.bodyMedium)
                Row {
                    AppointmentStatus.entries.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pid = selectedPatient?.id ?: return@TextButton
                    val endTime = startTime + durationMinutes * 60_000L
                    onSave(
                        Appointment(
                            id = appointment?.id ?: 0L,
                            patientId = pid,
                            startTime = startTime,
                            endTime = endTime,
                            durationMinutes = durationMinutes,
                            type = type,
                            status = status,
                            note = note.ifBlank { null }
                        )
                    )
                },
                enabled = selectedPatient != null
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {
                if (isEdit) {
                    TextButton(
                        onClick = { onDelete(appointment!!.id) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Удалить")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}
