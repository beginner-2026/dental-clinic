package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.data.db.ProstheticItemEntity
import com.dental.data.db.ToothEntity
import com.dental.model.*
import kotlinx.datetime.Clock

class ToothRepository(db: DentalDatabase) {

    private val toothQueries = db.toothQueries
    private val prostheticQueries = db.prostheticItemQueries

    fun getTeethByPatientId(patientId: Long): List<Tooth> {
        return toothQueries.getByPatientId(patientId).executeAsList().map { it.toTooth() }
    }

    fun saveTooth(tooth: Tooth) {
        if (tooth.id == 0L) {
            toothQueries.insert(
                patientId = tooth.patientId,
                number = tooth.number.toLong(),
                arch = tooth.arch.name,
                quadrant = tooth.quadrant.toLong(),
                status = tooth.status.name,
                examType = null,
                crownOption = tooth.crownOption?.name,
                rootOption = tooth.rootOption?.name
            )
        } else {
            toothQueries.upsert(
                id = tooth.id,
                patientId = tooth.patientId,
                number = tooth.number.toLong(),
                arch = tooth.arch.name,
                quadrant = tooth.quadrant.toLong(),
                status = tooth.status.name,
                examType = null,
                crownOption = tooth.crownOption?.name,
                rootOption = tooth.rootOption?.name
            )
        }
    }

    fun saveTeeth(teeth: List<Tooth>) {
        teeth.forEach { saveTooth(it) }
    }

    fun updateToothStatus(patientId: Long, number: Int, status: ToothStatus) {
        toothQueries.updateStatus(status.name, patientId, number.toLong())
    }

    fun updateCrownOption(patientId: Long, number: Int, option: CrownOption?) {
        toothQueries.updateCrownOption(option?.name, patientId, number.toLong())
    }

    fun updateRootOption(patientId: Long, number: Int, option: RootOption?) {
        toothQueries.updateRootOption(option?.name, patientId, number.toLong())
    }

    fun initDefaultTeeth(patientId: Long) {
        toothQueries.deleteByPatientId(patientId)
        val numbers = (18 downTo 11).toList() + (21..28).toList() + (31..38).toList() + (48 downTo 41).toList()
        numbers.forEach { n ->
            toothQueries.insert(
                patientId = patientId,
                number = n.toLong(),
                arch = if (n / 10 <= 2) Arch.UPPER.name else Arch.LOWER.name,
                quadrant = (n / 10).toLong(),
                status = ToothStatus.PRESENT.name,
                examType = null,
                crownOption = null,
                rootOption = null
            )
        }
    }

    fun getProstheticItemsByPatientId(patientId: Long): List<ProstheticItem> {
        return prostheticQueries.getByPatientId(patientId).executeAsList().map { it.toProstheticItem() }
    }

    fun saveProstheticItem(item: ProstheticItem): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        prostheticQueries.insert(
            patientId = item.patientId,
            toothIds = item.toothIds.joinToString(","),
            type = item.type.name,
            material = item.material.name,
            stage = item.stage.name,
            createdAt = now,
            updatedAt = now
        )
        return prostheticQueries.getByPatientId(item.patientId).executeAsList().lastOrNull()?.id ?: 0L
    }

    fun deleteProstheticItem(id: Long) {
        prostheticQueries.deleteById(id)
    }

    fun deleteAllProstheticItems(patientId: Long) {
        prostheticQueries.deleteByPatientId(patientId)
    }

    private fun ToothEntity.toTooth(): Tooth {
        return Tooth(
            id = id,
            patientId = patientId,
            number = number.toInt(),
            arch = Arch.valueOf(arch),
            quadrant = quadrant.toInt(),
            status = ToothStatus.valueOf(status),
            crownOption = crownOption?.let { try { CrownOption.valueOf(it) } catch (_: Exception) { null } },
            rootOption = rootOption?.let { try { RootOption.valueOf(it) } catch (_: Exception) { null } }
        )
    }

    private fun ProstheticItemEntity.toProstheticItem(): ProstheticItem {
        return ProstheticItem(
            id = id,
            patientId = patientId,
            toothIds = toothIds.split(",").map { it.trim().toInt() },
            type = ProstheticType.valueOf(type),
            material = ProstheticMaterial.valueOf(material),
            stage = ProstheticStage.valueOf(stage),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
