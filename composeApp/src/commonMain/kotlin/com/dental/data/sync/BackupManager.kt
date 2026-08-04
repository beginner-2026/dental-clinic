package com.dental.data.sync

import com.dental.data.db.DentalDatabase
import com.dental.getPlatformName
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupManager(private val database: DentalDatabase) {

    private val queries = database
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun exportAll(): BackupData {
        val patients = queries.patientQueries.getAll().executeAsList().map { e ->
            BackupPatient(
                id = e.id, lastName = e.lastName, firstName = e.firstName,
                middleName = e.middleName, birthDate = e.birthDate, phone = e.phone,
                email = e.email, sex = e.sex, notes = e.notes,
                createdAt = e.createdAt, updatedAt = e.updatedAt
            )
        }

        val appointments = queries.appointmentQueries.getAll().executeAsList().map { e ->
            BackupAppointment(
                id = e.id, patientId = e.patientId, startTime = e.startTime,
                endTime = e.endTime, durationMinutes = e.durationMinutes,
                doctorId = e.doctorId, chairId = e.chairId, type = e.type,
                status = e.status, note = e.note
            )
        }

        val teeth = queries.toothQueries.getAll().executeAsList().map { e ->
            BackupTooth(
                id = e.id, patientId = e.patientId, number = e.number.toInt(),
                arch = e.arch, quadrant = e.quadrant.toInt(), status = e.status,
                examType = e.examType, crownOption = e.crownOption,
                rootOption = e.rootOption
            )
        }

        val prostheticItems = queries.prostheticItemQueries.getAll().executeAsList().map { e ->
            BackupProstheticItem(
                id = e.id, patientId = e.patientId, toothIds = e.toothIds,
                type = e.type, material = e.material, stage = e.stage,
                createdAt = e.createdAt, updatedAt = e.updatedAt
            )
        }

        val diagnoses = queries.diagnosisQueries.getAll().executeAsList().map { e ->
            BackupDiagnosis(
                id = e.id, patientId = e.patientId, code = e.code,
                diagnosisText = e.diagnosisText, toothNumber = e.toothNumber,
                createdAt = e.createdAt
            )
        }

        val treatmentPlans = queries.treatmentPlanQueries.getAll().executeAsList().map { e ->
            BackupTreatmentPlan(
                id = e.id, patientId = e.patientId, toothNumbers = e.toothNumbers,
                procedure = e.procedure, createdAt = e.createdAt
            )
        }

        val visits = queries.visitPositionQueries.getAllVisits().executeAsList().map { e ->
            BackupVisit(
                id = e.id, patientId = e.patientId, visitDate = e.visitDate,
                createdAt = e.createdAt
            )
        }

        val visitPositions = queries.visitPositionQueries.getAllVisitPositions().executeAsList().map { e ->
            BackupVisitPosition(
                id = e.id, visitId = e.visitId, positionId = e.positionId,
                toothNumbers = e.toothNumbers, selectedAt = e.selectedAt,
                sortOrder = e.sortOrder
            )
        }

        val positions = queries.visitPositionQueries.getAllPositions().executeAsList().map { e ->
            BackupPosition(
                id = e.id, name = e.name, sortOrder = e.sortOrder
            )
        }

        val invoices = queries.invoiceQueries.getAllInvoices().executeAsList().map { e ->
            BackupInvoice(
                id = e.id, patientId = e.patientId, title = e.title,
                dateCreated = e.dateCreated, dateUpdated = e.dateUpdated,
                status = e.status, discountPercent = e.discountPercent,
                totalBeforeDiscount = e.totalBeforeDiscount,
                discountAmount = e.discountAmount, totalAfterDiscount = e.totalAfterDiscount
            )
        }

        val invoiceItems = queries.invoiceQueries.getAllInvoiceItems().executeAsList().map { e ->
            BackupInvoiceItem(
                id = e.id, invoiceId = e.invoiceId, serviceName = e.serviceName,
                serviceCode = e.serviceCode, quantity = e.quantity,
                unitPrice = e.unitPrice, lineTotal = e.lineTotal
            )
        }

        val priceListItems = queries.priceListQueries.getAll().executeAsList().map { e ->
            BackupPriceListItem(
                id = e.id, category = e.category, name = e.name,
                defaultPrice = e.defaultPrice, sortOrder = e.sortOrder
            )
        }

        return BackupData(
            version = 2,
            lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
            syncedByDevice = getPlatformName(),
            patients = patients, appointments = appointments, teeth = teeth,
            prostheticItems = prostheticItems, diagnoses = diagnoses,
            treatmentPlans = treatmentPlans, visits = visits,
            visitPositions = visitPositions, positions = positions,
            invoices = invoices, invoiceItems = invoiceItems,
            priceListItems = priceListItems
        )
    }

    fun exportToJson(): String {
        return json.encodeToString(exportAll())
    }

    fun importFromData(data: BackupData) {
        queries.transaction {
            clearAllData()

            data.positions.forEach { p ->
                queries.visitPositionQueries.insertPositionWithId(
                    id = p.id, name = p.name, sortOrder = p.sortOrder
                )
            }
            data.priceListItems.forEach { p ->
                queries.priceListQueries.insertWithId(
                    id = p.id, category = p.category, name = p.name,
                    defaultPrice = p.defaultPrice, sortOrder = p.sortOrder
                )
            }
            data.patients.forEach { p ->
                queries.patientQueries.insertWithId(
                    id = p.id, lastName = p.lastName, firstName = p.firstName,
                    middleName = p.middleName, birthDate = p.birthDate,
                    phone = p.phone, email = p.email, sex = p.sex,
                    notes = p.notes, createdAt = p.createdAt, updatedAt = p.updatedAt
                )
            }
            data.appointments.forEach { a ->
                queries.appointmentQueries.insertWithId(
                    id = a.id, patientId = a.patientId, startTime = a.startTime,
                    endTime = a.endTime, durationMinutes = a.durationMinutes,
                    doctorId = a.doctorId, chairId = a.chairId,
                    type = a.type, status = a.status, note = a.note
                )
            }
            data.teeth.forEach { t ->
                queries.toothQueries.upsert(
                    id = t.id, patientId = t.patientId, number = t.number.toLong(),
                    arch = t.arch, quadrant = t.quadrant.toLong(),
                    status = t.status, examType = t.examType,
                    crownOption = t.crownOption, rootOption = t.rootOption
                )
            }
            data.prostheticItems.forEach { p ->
                queries.prostheticItemQueries.insertWithId(
                    id = p.id, patientId = p.patientId, toothIds = p.toothIds,
                    type = p.type, material = p.material, stage = p.stage,
                    createdAt = p.createdAt, updatedAt = p.updatedAt
                )
            }
            data.diagnoses.forEach { d ->
                queries.diagnosisQueries.insertWithId(
                    id = d.id, patientId = d.patientId, code = d.code,
                    diagnosisText = d.diagnosisText, toothNumber = d.toothNumber,
                    createdAt = d.createdAt
                )
            }
            data.treatmentPlans.forEach { t ->
                queries.treatmentPlanQueries.insertWithId(
                    id = t.id, patientId = t.patientId, toothNumbers = t.toothNumbers,
                    procedure = t.procedure, createdAt = t.createdAt
                )
            }
            data.visits.forEach { v ->
                queries.visitPositionQueries.insertVisitWithId(
                    id = v.id, patientId = v.patientId, visitDate = v.visitDate,
                    createdAt = v.createdAt
                )
            }
            data.visitPositions.forEach { vp ->
                queries.visitPositionQueries.insertVisitPositionWithId(
                    id = vp.id, visitId = vp.visitId, positionId = vp.positionId,
                    toothNumbers = vp.toothNumbers, selectedAt = vp.selectedAt,
                    sortOrder = vp.sortOrder
                )
            }
            data.invoices.forEach { inv ->
                queries.invoiceQueries.insertInvoiceWithId(
                    id = inv.id, patientId = inv.patientId, title = inv.title,
                    dateCreated = inv.dateCreated, dateUpdated = inv.dateUpdated,
                    status = inv.status, discountPercent = inv.discountPercent,
                    totalBeforeDiscount = inv.totalBeforeDiscount,
                    discountAmount = inv.discountAmount,
                    totalAfterDiscount = inv.totalAfterDiscount
                )
            }
            data.invoiceItems.forEach { item ->
                queries.invoiceQueries.insertInvoiceItemWithId(
                    id = item.id, invoiceId = item.invoiceId, serviceName = item.serviceName,
                    serviceCode = item.serviceCode, quantity = item.quantity,
                    unitPrice = item.unitPrice, lineTotal = item.lineTotal
                )
            }
        }
    }

    fun importFromJson(jsonString: String) {
        val data = json.decodeFromString<BackupData>(jsonString)
        importFromData(data)
    }

    fun parseMetadata(jsonString: String): BackupData {
        return json.decodeFromString<BackupData>(jsonString)
    }

    private fun clearAllData() {
        queries.patientQueries.deleteAll()
        queries.appointmentQueries.deleteAll()
        queries.toothQueries.deleteAll()
        queries.prostheticItemQueries.deleteAll()
        queries.diagnosisQueries.deleteAll()
        queries.treatmentPlanQueries.deleteAll()
        queries.priceListQueries.deleteAll()
        queries.visitPositionQueries.deleteAllVisitPositions()
        queries.visitPositionQueries.deleteAllVisits()
        queries.visitPositionQueries.deleteAllPositions()
        queries.invoiceQueries.deleteAllItems()
        queries.invoiceQueries.deleteAllInvoices()
    }
}
