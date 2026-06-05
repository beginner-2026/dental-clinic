package com.dental.model

data class Appointment(
    val id: Long = 0,
    val patientId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int = 30,
    val doctorId: Long? = null,
    val chairId: Long? = null,
    val type: AppointmentType = AppointmentType.INITIAL,
    val status: AppointmentStatus = AppointmentStatus.PLANNED,
    val note: String? = null
)

enum class AppointmentType {
    INITIAL, FOLLOW_UP, EMERGENCY, OTHER
}

enum class AppointmentStatus {
    PLANNED, CHECKED_IN, COMPLETED, CANCELED, NO_SHOW
}
