package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.data.db.PatientEntity
import com.dental.model.Patient
import com.dental.model.Sex
import kotlinx.datetime.Clock

class PatientRepository(db: DentalDatabase) {

    private val queries = db.patientQueries

    fun getAll(): List<Patient> {
        return queries.getAll().executeAsList().map { it.toPatient() }
    }

    fun getById(id: Long): Patient? {
        return queries.getById(id).executeAsOneOrNull()?.toPatient()
    }

    fun search(query: String): List<Patient> {
        return queries.search(query, query, query).executeAsList().map { it.toPatient() }
    }

    fun create(patient: Patient): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insert(
            lastName = patient.lastName,
            firstName = patient.firstName,
            middleName = patient.middleName,
            birthDate = patient.birthDate,
            phone = patient.phone,
            email = patient.email,
            sex = patient.sex.name,
            notes = patient.notes,
            createdAt = now,
            updatedAt = now
        )
        return queries.getLastInsertId().executeAsOne()
    }

    private fun PatientEntity.toPatient(): Patient {
        return Patient(
            id = id,
            lastName = lastName,
            firstName = firstName,
            middleName = middleName,
            birthDate = birthDate,
            phone = phone,
            email = email,
            sex = Sex.valueOf(sex),
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
