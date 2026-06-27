package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.data.db.DiagnosisEntity
import com.dental.model.Diagnosis
import kotlinx.datetime.Clock

class DiagnosisRepository(private val db: DentalDatabase) {

    private val queries = db.diagnosisQueries

    fun getByPatientId(patientId: Long): List<Diagnosis> {
        return queries.getByPatientId(patientId).executeAsList().map { it.toDiagnosis() }
    }

    fun saveAll(patientId: Long, diagnoses: List<Diagnosis>) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            queries.deleteByPatientId(patientId)
            diagnoses.forEach { d ->
                queries.insert(
                    patientId = patientId,
                    code = d.code,
                    diagnosisText = d.diagnosisText,
                    toothNumber = d.toothNumber,
                    createdAt = now
                )
            }
        }
    }

    private fun DiagnosisEntity.toDiagnosis(): Diagnosis {
        return Diagnosis(
            id = id,
            patientId = patientId,
            code = code,
            diagnosisText = diagnosisText,
            toothNumber = toothNumber,
            createdAt = createdAt
        )
    }
}
