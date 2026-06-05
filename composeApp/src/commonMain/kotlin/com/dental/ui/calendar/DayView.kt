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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dental.model.AppointmentStatus
import com.dental.model.AppointmentType
import com.dental.model.AppointmentWithPatient
import com.dental.ui.calendar.CalendarViewModel.Companion.HOURS
import com.dental.ui.calendar.CalendarViewModel.Companion.SLOT_HEIGHT_DP
import com.dental.ui.theme.DentalColors
import kotlinx.datetime.*

@Composable
fun DayView(
    date: LocalDate,
    appointments: List<AppointmentWithPatient>,
    onTimeSlotClick: (Long) -> Unit,
    onAppointmentClick: (AppointmentWithPatient) -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column {
            HOURS.forEach { hour ->
                TimeSlotRow(
                    date = date,
                    hour = hour,
                    appointments = appointments.filter { isAppointmentInHour(it, hour) },
                    onSlotClick = { minutes ->
                        val time = LocalDateTime(
                            date = date,
                            time = LocalTime(hour, minutes)
                        )
                        onTimeSlotClick(time.toInstant(TimeZone.UTC).toEpochMilliseconds())
                    },
                    onAppointmentClick = onAppointmentClick
                )
            }
        }
    }
}

@Composable
private fun TimeSlotRow(
    date: LocalDate,
    hour: Int,
    appointments: List<AppointmentWithPatient>,
    onSlotClick: (Int) -> Unit,
    onAppointmentClick: (AppointmentWithPatient) -> Unit
) {
    Column {
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

            // Slot content area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // First half (0-30 min)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        .pointerInput(Unit) {
                            detectTapGestures { onSlotClick(0) }
                        }
                )

                // Second half (30-60 min)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        .pointerInput(Unit) {
                            detectTapGestures { onSlotClick(30) }
                        }
                )
            }
        }

        // Render appointments in this hour
        appointments.forEach { app ->
            AppointmentBlock(
                appointmentWithPatient = app,
                onClick = { onAppointmentClick(app) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 64.dp, end = 4.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun AppointmentBlock(
    appointmentWithPatient: AppointmentWithPatient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = appointmentWithPatient.appointment
    val bgColor = when (app.type) {
        AppointmentType.INITIAL -> DentalColors.AppointmentInitial
        AppointmentType.FOLLOW_UP -> DentalColors.AppointmentFollowUp
        AppointmentType.EMERGENCY -> DentalColors.AppointmentEmergency
        AppointmentType.OTHER -> DentalColors.AppointmentOther
    }

    val startLocal = Instant.fromEpochMilliseconds(app.startTime)
        .toLocalDateTime(TimeZone.UTC)
    val endLocal = Instant.fromEpochMilliseconds(app.endTime)
        .toLocalDateTime(TimeZone.UTC)

    Card(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = appointmentWithPatient.patientName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "%02d:%02d - %02d:%02d".format(
                    startLocal.hour, startLocal.minute,
                    endLocal.hour, endLocal.minute
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f)
            )
            if (app.status == AppointmentStatus.CANCELED) {
                Text(
                    text = "CANCELED",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun isAppointmentInHour(app: AppointmentWithPatient, hour: Int): Boolean {
    val startLocal = Instant.fromEpochMilliseconds(app.appointment.startTime)
        .toLocalDateTime(TimeZone.UTC)
    return startLocal.hour == hour
}
