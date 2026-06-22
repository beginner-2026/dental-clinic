package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.model.*
import kotlinx.datetime.*

object SeedData {

    fun seedIfEmpty(database: DentalDatabase) {
        val patientQueries = database.patientQueries
        val appointmentQueries = database.appointmentQueries

        if (patientQueries.getAll().executeAsList().isNotEmpty()) return

        val now = Clock.System.now().toEpochMilliseconds()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val patients = listOf(
            Triple("Иванов", "Иван", "Иванович"),
            Triple("Петрова", "Мария", "Сергеевна"),
            Triple("Сидоров", "Алексей", "Владимирович"),
            Triple("Кузнецова", "Ольга", "Дмитриевна"),
            Triple("Смирнов", "Дмитрий", "Анатольевич")
        )

        val patientIds = patients.map { (last, first, middle) ->
            patientQueries.insert(
                lastName = last,
                firstName = first,
                middleName = middle,
                birthDate = now - (30L * 365 * 24 * 60 * 60 * 1000),
                phone = "+7 (999) 123-45-67",
                email = null,
                sex = if (last.endsWith("а") || last.endsWith("я")) Sex.FEMALE.name else Sex.MALE.name,
                notes = null,
                createdAt = now,
                updatedAt = now
            )
            patientQueries.getLastInsertId().executeAsOne()
        }

        fun dateToEpochMillis(date: LocalDate, hour: Int, minute: Int): Long {
            val time = LocalTime(hour, minute.coerceIn(0, 59))
            val dateTime = LocalDateTime(date, time)
            return dateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }

        data class AppointmentSeed(val patientIndex: Int, val hour: Int, val minute: Int, val type: AppointmentType)

        val appointments = listOf(
            AppointmentSeed(0, 9, 0, AppointmentType.INITIAL),
            AppointmentSeed(1, 10, 30, AppointmentType.FOLLOW_UP),
            AppointmentSeed(2, 14, 0, AppointmentType.EMERGENCY),
            AppointmentSeed(3, 11, 0, AppointmentType.FOLLOW_UP),
            AppointmentSeed(4, 15, 30, AppointmentType.INITIAL),
            AppointmentSeed(0, 16, 0, AppointmentType.FOLLOW_UP),
            AppointmentSeed(2, 8, 30, AppointmentType.INITIAL)
        )

        appointments.forEach { seed ->
            val start = dateToEpochMillis(today, seed.hour, seed.minute)
            val endMinute = seed.minute + 30
            val endHour = seed.hour + endMinute / 60
            val end = dateToEpochMillis(today, endHour, endMinute % 60)
            appointmentQueries.insert(
                patientId = patientIds[seed.patientIndex],
                startTime = start,
                endTime = end,
                durationMinutes = 30L,
                doctorId = null,
                chairId = null,
                type = seed.type.name,
                status = AppointmentStatus.PLANNED.name,
                note = null
            )
        }
    }
}
