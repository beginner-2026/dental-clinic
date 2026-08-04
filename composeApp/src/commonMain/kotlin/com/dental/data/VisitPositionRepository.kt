package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.data.db.PositionEntity
import com.dental.data.db.VisitEntity
import com.dental.model.Position
import com.dental.model.PositionSelection
import com.dental.model.Visit
import kotlinx.datetime.Clock

class VisitPositionRepository(private val db: DentalDatabase) {

    private val queries = db.visitPositionQueries

    fun getAllPositions(): List<Position> {
        return queries.getAllPositions().executeAsList().map { it.toPosition() }
    }

    fun createPosition(name: String, sortOrder: Int = 0): Long {
        queries.insertPosition(name = name, sortOrder = sortOrder.toLong())
        return queries.getLastInsertId().executeAsOne()
    }

    fun ensurePosition(name: String): Position {
        val existing = getAllPositions().firstOrNull { it.name == name }
        if (existing != null) return existing
        val maxOrder = getAllPositions().maxOfOrNull { it.sortOrder } ?: 0
        val id = createPosition(name, maxOrder + 1)
        return Position(id = id, name = name, sortOrder = maxOrder + 1)
    }

    fun getPositionById(id: Long): Position? {
        return queries.getPositionById(id).executeAsOneOrNull()?.toPosition()
    }

    fun getVisitsByPatientId(patientId: Long): List<Visit> {
        return queries.getVisitsByPatientId(patientId).executeAsList().map { entity ->
            val positions = getPositionSelectionsByVisitId(entity.id)
            entity.toVisit(positions)
        }
    }

    fun getVisitById(id: Long): Visit? {
        val entity = queries.getVisitById(id).executeAsOneOrNull() ?: return null
        val positions = getPositionSelectionsByVisitId(entity.id)
        return entity.toVisit(positions)
    }

    fun createVisit(patientId: Long, visitDate: Long): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertVisit(
            patientId = patientId,
            visitDate = visitDate,
            createdAt = now
        )
        return queries.getLastInsertId().executeAsOne()
    }

    fun saveVisitPositions(visitId: Long, positionIds: List<Long>, toothNumbersMap: Map<Long, String> = emptyMap()) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            queries.deleteVisitPositionsByVisitId(visitId)
            positionIds.forEachIndexed { index, posId ->
                queries.insertVisitPosition(
                    visitId = visitId,
                    positionId = posId,
                    toothNumbers = toothNumbersMap[posId] ?: "",
                    selectedAt = now,
                    sortOrder = index.toLong()
                )
            }
        }
    }

    fun deleteVisit(visitId: Long) {
        queries.transaction {
            queries.deleteVisitPositionsByVisitId(visitId)
            queries.deleteVisit(visitId)
        }
    }

    private fun getPositionSelectionsByVisitId(visitId: Long): List<PositionSelection> {
        return queries.getPositionsByVisitId(visitId).executeAsList().map { row ->
            PositionSelection(
                position = Position(id = row.id, name = row.name, sortOrder = row.sortOrder.toInt()),
                toothNumbers = row.toothNumbers
            )
        }
    }

    private fun PositionEntity.toPosition(): Position {
        return Position(
            id = id,
            name = name,
            sortOrder = sortOrder.toInt()
        )
    }

    private fun VisitEntity.toVisit(positions: List<PositionSelection>): Visit {
        return Visit(
            id = id,
            patientId = patientId,
            visitDate = visitDate,
            createdAt = createdAt,
            positions = positions
        )
    }
}
