package com.dental.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dental.model.Patient
import com.dental.model.Sex
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientEditDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (Patient) -> Unit
) {
    var lastName by remember(patient.id) { mutableStateOf(patient.lastName) }
    var firstName by remember(patient.id) { mutableStateOf(patient.firstName) }
    var middleName by remember(patient.id) { mutableStateOf(patient.middleName.orEmpty()) }
    var phone by remember(patient.id) { mutableStateOf(patient.phone.orEmpty()) }
    var email by remember(patient.id) { mutableStateOf(patient.email.orEmpty()) }
    var birthDateText by remember(patient.id) { mutableStateOf(formatBirthDate(patient.birthDate)) }
    var birthDateError by remember(patient.id) { mutableStateOf(false) }
    var sex by remember(patient.id) { mutableStateOf(patient.sex) }
    var notes by remember(patient.id) { mutableStateOf(patient.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать пациента") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Фамилия *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Имя *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = middleName,
                    onValueChange = { middleName = it },
                    label = { Text("Отчество") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = birthDateText,
                    onValueChange = {
                        birthDateText = it
                        birthDateError = false
                    },
                    label = { Text("Дата рождения") },
                    placeholder = { Text("ДД.ММ.ГГГГ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = birthDateError,
                    supportingText = if (birthDateError) {
                        { Text("Неверная дата. Формат: ДД.ММ.ГГГГ") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                Text("Пол", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sex == Sex.MALE,
                        onClick = { sex = Sex.MALE },
                        label = { Text("Мужской") }
                    )
                    FilterChip(
                        selected = sex == Sex.FEMALE,
                        onClick = { sex = Sex.FEMALE },
                        label = { Text("Женский") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = lastName.isNotBlank() && firstName.isNotBlank(),
                onClick = {
                    val birthDate = if (birthDateText.isBlank()) {
                        null
                    } else {
                        parseBirthDate(birthDateText)
                    }
                    if (birthDateText.isNotBlank() && birthDate == null) {
                        birthDateError = true
                    } else {
                        onSave(
                            patient.copy(
                                lastName = lastName.trim(),
                                firstName = firstName.trim(),
                                middleName = middleName.trim().ifBlank { null },
                                phone = phone.trim().ifBlank { null },
                                email = email.trim().ifBlank { null },
                                birthDate = birthDate,
                                sex = sex,
                                notes = notes.trim().ifBlank { null }
                            )
                        )
                    }
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun formatBirthDate(millis: Long?): String {
    if (millis == null) return ""
    val date = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day.$month.${date.year}"
}

private fun parseBirthDate(text: String): Long? {
    val parts = text.trim().split(".", "/", "-", " ").filter { it.isNotBlank() }
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    if (year < 1900) return null
    return try {
        LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }
}
