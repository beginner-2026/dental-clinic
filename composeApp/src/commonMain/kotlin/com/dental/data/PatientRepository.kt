package com.dental.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dental.data.db.DentalDatabase
import com.dental.data.db.PatientEntity
import com.dental.model.Patient
import com.dental.model.Sex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class PatientRepository(private val db: DentalDatabase) {

    private val queries = db.patientQueries

    @OptIn(FlowPreview::class)
    fun observeAll(): Flow<List<Patient>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toPatient() } }
    }

    fun getAll(): List<Patient> {
        return queries.getAll().executeAsList().map { it.toPatient() }
    }

    fun getById(id: Long): Patient? {
        return queries.getById(id).executeAsOneOrNull()?.toPatient()
    }

    fun search(query: String): List<Patient> {
        return queries.search(query, query, query).executeAsList().map { it.toPatient() }
    }

    fun delete(id: Long) {
        db.transaction {
            db.visitPositionQueries.deleteVisitPositionsByPatientId(id)
            db.invoiceQueries.deleteItemsByPatientId(id)
            db.appointmentQueries.deleteByPatientId(id)
            db.toothQueries.deleteByPatientId(id)
            db.prostheticItemQueries.deleteByPatientId(id)
            db.diagnosisQueries.deleteByPatientId(id)
            db.treatmentPlanQueries.deleteByPatientId(id)
            db.visitPositionQueries.deleteVisitsByPatientId(id)
            db.invoiceQueries.deleteInvoicesByPatientId(id)
            queries.deleteById(id)
        }
    }

    fun getAllSortedByLastVisit(): List<Patient> {
        val allPatients = getAll()
        val lastVisitByPatient = db.appointmentQueries.getLastVisitPerPatient().executeAsList()
            .associate { it.patientId to it.lastVisit }
        return allPatients.sortedByDescending { lastVisitByPatient[it.id] ?: 0L }
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
