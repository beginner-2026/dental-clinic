package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.data.db.TreatmentPlanEntity
import com.dental.model.TreatmentPlanItem
import kotlinx.datetime.Clock

class TreatmentPlanRepository(private val db: DentalDatabase) {

    private val queries = db.treatmentPlanQueries

    fun getByPatientId(patientId: Long): List<TreatmentPlanItem> {
        return queries.getByPatientId(patientId).executeAsList().map { it.toTreatmentPlanItem() }
    }

    fun saveAll(patientId: Long, items: List<TreatmentPlanItem>) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            queries.deleteByPatientId(patientId)
            items.forEach { item ->
                queries.insert(
                    patientId = patientId,
                    toothNumbers = item.toothNumbers,
                    procedure = item.procedure,
                    createdAt = now
                )
            }
        }
    }

    private fun TreatmentPlanEntity.toTreatmentPlanItem(): TreatmentPlanItem {
        return TreatmentPlanItem(
            id = id,
            patientId = patientId,
            toothNumbers = toothNumbers,
            procedure = procedure,
            createdAt = createdAt
        )
    }
}
