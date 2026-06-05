package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.model.Appointment
import com.dental.model.AppointmentStatus
import com.dental.model.AppointmentType
import com.dental.model.AppointmentWithPatient
import com.dental.model.Patient
import com.dental.model.Sex

class AppointmentRepository(db: DentalDatabase) {

    private val appointmentQueries = db.appointmentQueries
    private val patientQueries = db.patientQueries

    fun getAppointmentsInRange(startOfDay: Long, endOfDay: Long): List<AppointmentWithPatient> {
        val entities = appointmentQueries.getByDateRange(startOfDay, endOfDay).executeAsList()
        return entities.map { entity ->
            val patient = patientQueries.getById(entity.patientId).executeAsOneOrNull()
            AppointmentWithPatient(
                appointment = Appointment(
                    id = entity.id,
                    patientId = entity.patientId,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    durationMinutes = entity.durationMinutes.toInt(),
                    doctorId = entity.doctorId,
                    chairId = entity.chairId,
                    type = AppointmentType.valueOf(entity.type),
                    status = AppointmentStatus.valueOf(entity.status),
                    note = entity.note
                ),
                patientName = patient?.let { "${it.lastName} ${it.firstName}" } ?: "Unknown"
            )
        }
    }

    fun getAppointmentsByPatient(patientId: Long): List<Appointment> {
        return appointmentQueries.getByPatientId(patientId).executeAsList().map { entity ->
            Appointment(
                id = entity.id,
                patientId = entity.patientId,
                startTime = entity.startTime,
                endTime = entity.endTime,
                durationMinutes = entity.durationMinutes.toInt(),
                doctorId = entity.doctorId,
                chairId = entity.chairId,
                type = AppointmentType.valueOf(entity.type),
                status = AppointmentStatus.valueOf(entity.status),
                note = entity.note
            )
        }
    }

    fun createAppointment(appointment: Appointment) {
        appointmentQueries.insert(
            patientId = appointment.patientId,
            startTime = appointment.startTime,
            endTime = appointment.endTime,
            durationMinutes = appointment.durationMinutes.toLong(),
            doctorId = appointment.doctorId,
            chairId = appointment.chairId,
            type = appointment.type.name,
            status = appointment.status.name,
            note = appointment.note
        )
    }

    fun updateStatus(id: Long, status: AppointmentStatus) {
        appointmentQueries.updateStatus(status.name, id)
    }

    fun deleteAppointment(id: Long) {
        appointmentQueries.deleteById(id)
    }
}
