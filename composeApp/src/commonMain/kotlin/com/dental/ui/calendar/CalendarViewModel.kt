package com.dental.ui.calendar

import com.dental.data.AppointmentRepository
import com.dental.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.*

data class CalendarState(
    val currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val viewMode: ViewMode = ViewMode.DAY,
    val appointments: List<AppointmentWithPatient> = emptyList(),
    val showAppointmentDialog: Boolean = false,
    val editingAppointment: Appointment? = null,
    val selectedTimeSlot: Long? = null,
    val isLoading: Boolean = false
)

enum class ViewMode { DAY, WEEK }

class CalendarViewModel(private val repository: AppointmentRepository) {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        loadAppointments()
    }

    fun loadAppointments() {
        val s = _state.value
        val (startMs, endMs) = if (s.viewMode == ViewMode.DAY) {
            val start = dateToEpochMillis(s.currentDate)
            val end = dateToEpochMillis(s.currentDate.plus(1, DateTimeUnit.DAY))
            Pair(start, end)
        } else {
            val weekStart = startOfWeek(s.currentDate)
            val start = dateToEpochMillis(weekStart)
            val end = dateToEpochMillis(weekStart.plus(7, DateTimeUnit.DAY))
            Pair(start, end)
        }
        val apps = repository.getAppointmentsInRange(startMs, endMs)
        _state.value = s.copy(appointments = apps, isLoading = false)
    }

    fun setViewMode(mode: ViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
        loadAppointments()
    }

    fun goToToday() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        _state.value = _state.value.copy(currentDate = today)
        loadAppointments()
    }

    fun goNext() {
        val s = _state.value
        val offset = if (s.viewMode == ViewMode.DAY) 1 else 7
        _state.value = s.copy(currentDate = s.currentDate.plus(offset, DateTimeUnit.DAY))
        loadAppointments()
    }

    fun goPrevious() {
        val s = _state.value
        val offset = if (s.viewMode == ViewMode.DAY) 1 else 7
        _state.value = s.copy(currentDate = s.currentDate.minus(offset, DateTimeUnit.DAY))
        loadAppointments()
    }

    fun showNewAppointment(timeSlot: Long?) {
        _state.value = _state.value.copy(
            showAppointmentDialog = true,
            editingAppointment = null,
            selectedTimeSlot = timeSlot
        )
    }

    fun showEditAppointment(appointment: Appointment) {
        _state.value = _state.value.copy(
            showAppointmentDialog = true,
            editingAppointment = appointment,
            selectedTimeSlot = null
        )
    }

    fun dismissAppointmentDialog() {
        _state.value = _state.value.copy(
            showAppointmentDialog = false,
            editingAppointment = null,
            selectedTimeSlot = null
        )
    }

    fun saveAppointment(appointment: Appointment) {
        repository.createAppointment(appointment)
        dismissAppointmentDialog()
        loadAppointments()
    }

    fun updateAppointmentStatus(id: Long, status: AppointmentStatus) {
        repository.updateStatus(id, status)
        loadAppointments()
    }

    fun deleteAppointment(id: Long) {
        repository.deleteAppointment(id)
        loadAppointments()
    }

    companion object {
        fun dateToEpochMillis(date: LocalDate): Long {
            val dateTime = LocalDateTime(date = date, time = LocalTime(0, 0))
            return dateTime.toInstant(TimeZone.UTC).toEpochMilliseconds()
        }

        fun startOfWeek(date: LocalDate): LocalDate {
            val dayOfWeek = date.dayOfWeek
            val daysFromMonday = dayOfWeek.ordinal
            return date.minus(daysFromMonday, DateTimeUnit.DAY)
        }

        val HOURS: List<Int> = (8..20).toList()
        const val SLOT_MINUTES = 30
        const val SLOT_HEIGHT_DP = 60
        const val HOUR_HEIGHT_DP = 120
    }
}
