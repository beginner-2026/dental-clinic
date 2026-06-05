package com.dental.ui.calendar

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dental.model.AppointmentWithPatient
import com.dental.ui.calendar.CalendarViewModel.Companion.HOURS
import com.dental.ui.calendar.CalendarViewModel.Companion.SLOT_HEIGHT_DP
import com.dental.ui.theme.DentalColors
import kotlinx.datetime.*

@Composable
fun WeekView(
    weekStart: LocalDate,
    appointments: List<AppointmentWithPatient>,
    onTimeSlotClick: (Long) -> Unit,
    onAppointmentClick: (AppointmentWithPatient) -> Unit
) {
    val scrollState = rememberScrollState()
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val days = (0..6).map { weekStart.plus(it, DateTimeUnit.DAY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Day headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Spacer(Modifier.width(60.dp))
            days.forEachIndexed { index, day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayNames[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${day.dayOfMonth}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Time slots
        HOURS.forEach { hour ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SLOT_HEIGHT_DP.dp)
            ) {
                // Time label
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                        .padding(end = 4.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "%02d:00".format(hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Day columns
                days.forEach { day ->
                    val dayAppointments = appointments.filter { isOnDate(it, day, hour) }
                    val startTime = CalendarViewModel.dateToEpochMillis(day)
                    val slotMs = startTime + hour * 3600_000L

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            .pointerInput(Unit) {
                                detectTapGestures { onTimeSlotClick(slotMs) }
                            }
                    ) {
                        dayAppointments.forEach { app ->
                            AppointmentChip(
                                appointmentWithPatient = app,
                                onClick = { onAppointmentClick(app) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentChip(
    appointmentWithPatient: AppointmentWithPatient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = appointmentWithPatient.appointment
    val bgColor = when (app.type) {
        com.dental.model.AppointmentType.INITIAL -> DentalColors.AppointmentInitial
        com.dental.model.AppointmentType.FOLLOW_UP -> DentalColors.AppointmentFollowUp
        com.dental.model.AppointmentType.EMERGENCY -> DentalColors.AppointmentEmergency
        com.dental.model.AppointmentType.OTHER -> DentalColors.AppointmentOther
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor.copy(alpha = 0.85f))
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Text(
            text = appointmentWithPatient.patientName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun isOnDate(app: AppointmentWithPatient, date: LocalDate, hour: Int): Boolean {
    val start = Instant.fromEpochMilliseconds(app.appointment.startTime)
        .toLocalDateTime(TimeZone.UTC)
    return start.date == date && start.hour == hour
}
