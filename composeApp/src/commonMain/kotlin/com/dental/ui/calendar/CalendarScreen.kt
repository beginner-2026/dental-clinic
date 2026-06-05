package com.dental.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dental.model.Patient
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, patients: List<Patient> = emptyList(), onMenuClick: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.goToToday() }) {
                        Text("Today")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showNewAppointment(null) }) {
                Icon(Icons.Default.Add, contentDescription = "New Appointment")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CalendarHeader(
                currentDate = state.currentDate,
                viewMode = state.viewMode,
                onPrevious = { viewModel.goPrevious() },
                onNext = { viewModel.goNext() },
                onViewModeChange = { viewModel.setViewMode(it) }
            )

            if (state.viewMode == ViewMode.DAY) {
                DayView(
                    date = state.currentDate,
                    appointments = state.appointments,
                    onTimeSlotClick = { timeSlot -> viewModel.showNewAppointment(timeSlot) },
                    onAppointmentClick = { app -> viewModel.showEditAppointment(app.appointment) }
                )
            } else {
                WeekView(
                    weekStart = CalendarViewModel.startOfWeek(state.currentDate),
                    appointments = state.appointments,
                    onTimeSlotClick = { timeSlot -> viewModel.showNewAppointment(timeSlot) },
                    onAppointmentClick = { app -> viewModel.showEditAppointment(app.appointment) }
                )
            }
        }
    }

    if (state.showAppointmentDialog) {
        AppointmentDialog(
            appointment = state.editingAppointment,
            selectedTimeSlot = state.selectedTimeSlot,
            patients = patients,
            onDismiss = { viewModel.dismissAppointmentDialog() },
            onSave = { viewModel.saveAppointment(it) },
            onDelete = { id -> viewModel.deleteAppointment(id) }
        )
    }
}

@Composable
private fun CalendarHeader(
    currentDate: LocalDate,
    viewMode: ViewMode,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
                }

                Text(
                    text = formatHeaderDate(currentDate, viewMode),
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val selectedChip = if (viewMode == ViewMode.DAY) 0 else 1
                FilterChip(
                    selected = selectedChip == 0,
                    onClick = { onViewModeChange(ViewMode.DAY) },
                    label = { Text("Day") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = selectedChip == 1,
                    onClick = { onViewModeChange(ViewMode.WEEK) },
                    label = { Text("Week") }
                )
            }
        }
    }
}

private fun formatHeaderDate(date: LocalDate, viewMode: ViewMode): String {
    return if (viewMode == ViewMode.DAY) {
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val days = listOf(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        )
        "${days[date.dayOfWeek.ordinal]}, ${months[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}"
    } else {
        val weekStart = CalendarViewModel.startOfWeek(date)
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
        "${weekStart.dayOfMonth}/${weekStart.monthNumber} - ${weekEnd.dayOfMonth}/${weekEnd.monthNumber}/${weekEnd.year}"
    }
}
