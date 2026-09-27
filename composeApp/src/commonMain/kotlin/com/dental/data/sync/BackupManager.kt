package com.dental.data.sync

import com.dental.data.db.DentalDatabase
import com.dental.getPlatformName
import com.dental.model.AppointmentStatus
import com.dental.model.AppointmentType
import com.dental.model.Arch
import com.dental.model.CrownOption
import com.dental.model.InvoiceStatus
import com.dental.model.ProstheticMaterial
import com.dental.model.ProstheticStage
import com.dental.model.ProstheticType
import com.dental.model.RootOption
import com.dental.model.Sex
import com.dental.model.ToothStatus
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupManager(private val database: DentalDatabase) {

    private val queries = database
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportAll(): BackupData = queries.transactionWithResult { readSnapshot() }
        .let(::removeOrphanedRecords)
        .also(::validateBackupSnapshot)

    private fun readSnapshot(): BackupData {
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

    private fun removeOrphanedRecords(data: BackupData): BackupData {
        val patientIds = data.patients.mapTo(mutableSetOf()) { it.id }
        val visitIds = data.visits.mapTo(mutableSetOf()) { it.id }
        val positionIds = data.positions.mapTo(mutableSetOf()) { it.id }
        val invoiceIds = data.invoices.mapTo(mutableSetOf()) { it.id }
        return data.copy(
            appointments = data.appointments.filter { it.patientId in patientIds },
            teeth = data.teeth.filter { it.patientId in patientIds },
            prostheticItems = data.prostheticItems.filter { it.patientId in patientIds },
            diagnoses = data.diagnoses.filter { it.patientId in patientIds },
            treatmentPlans = data.treatmentPlans.filter { it.patientId in patientIds },
            visits = data.visits.filter { it.patientId in patientIds },
            visitPositions = data.visitPositions.filter { it.visitId in visitIds && it.positionId in positionIds },
            invoices = data.invoices.filter { it.patientId in patientIds },
            invoiceItems = data.invoiceItems.filter { it.invoiceId in invoiceIds }
        )
    }

    fun importFromData(data: BackupData) {
        validateBackupSnapshot(data)
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
        val data = parseMetadata(jsonString)
        importFromData(data)
    }

    fun parseMetadata(jsonString: String): BackupData = parseBackupSnapshot(jsonString)

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

internal fun parseBackupSnapshot(jsonString: String): BackupData {
    if (jsonString.length > MAX_IMPORT_CHARS) {
        throw IllegalArgumentException("Файл слишком большой для восстановления")
    }
    val root = snapshotJson.parseToJsonElement(jsonString)
    val keys = (root as? kotlinx.serialization.json.JsonObject)?.keys
        ?: throw IllegalArgumentException("Файл не является объектом JSON")
    if (keys.isEmpty()) {
        throw IllegalArgumentException("Резервная копия пуста")
    }
    val missingSections = REQUIRED_SECTIONS.filterNot(keys::contains)
    val legacyExport = keys.contains("lastSyncedAt") && keys.contains("syncedByDevice")
    if (missingSections.isNotEmpty() && !legacyExport) {
        throw IllegalArgumentException("Резервная копия неполная: отсутствуют разделы ${missingSections.joinToString(", ")}")
    }
    return snapshotJson.decodeFromString<BackupData>(jsonString).also(::validateBackupSnapshot)
}

internal fun validateBackupSnapshot(data: BackupData) {
    require(data.version == SUPPORTED_VERSION) {
        "Неподдерживаемая версия резервной копии: ${data.version}"
    }
    data.lastSyncedAt?.let { require(it > 0) { "Некорректное время синхронизации" } }

    validateUniqueIds("пациентов", data.patients.map { it.id })
    validateUniqueIds("записей", data.appointments.map { it.id })
    validateUniqueIds("зубов", data.teeth.map { it.id })
    validateUniqueIds("протезных конструкций", data.prostheticItems.map { it.id })
    validateUniqueIds("диагнозов", data.diagnoses.map { it.id })
    validateUniqueIds("планов лечения", data.treatmentPlans.map { it.id })
    validateUniqueIds("визитов", data.visits.map { it.id })
    validateUniqueIds("позиций визитов", data.visitPositions.map { it.id })
    validateUniqueIds("позиций", data.positions.map { it.id })
    validateUniqueIds("счетов", data.invoices.map { it.id })
    validateUniqueIds("позиций счетов", data.invoiceItems.map { it.id })
    validateUniqueIds("услуг прайс-листа", data.priceListItems.map { it.id })

    val patientIds = data.patients.mapTo(mutableSetOf()) { it.id }
    val visitIds = data.visits.mapTo(mutableSetOf()) { it.id }
    val positionIds = data.positions.mapTo(mutableSetOf()) { it.id }
    val invoiceIds = data.invoices.mapTo(mutableSetOf()) { it.id }

    require(data.patients.all { it.lastName.isNotBlank() && it.firstName.isNotBlank() && it.sex in VALID_SEXES }) {
        "Некорректные данные пациента"
    }
    require(data.appointments.all {
        it.patientId in patientIds && it.startTime >= 0 && it.endTime >= it.startTime &&
            it.durationMinutes > 0 && it.type in VALID_APPOINTMENT_TYPES && it.status in VALID_APPOINTMENT_STATUSES
    }) { "Запись содержит некорректные данные" }
    require(data.teeth.all { tooth ->
        tooth.patientId in patientIds && tooth.number in VALID_FDI_NUMBERS &&
            tooth.number / 10 == tooth.quadrant && tooth.quadrant in 1..4 &&
            (tooth.quadrant <= 2) == (tooth.arch == Arch.UPPER.name) &&
            tooth.arch in VALID_ARCHES && tooth.status in VALID_TOOTH_STATUSES &&
            (tooth.crownOption == null || tooth.crownOption in VALID_CROWN_OPTIONS) &&
            (tooth.rootOption == null || tooth.rootOption in VALID_ROOT_OPTIONS)
    }) { "Некорректные данные одонтограммы" }
    require(data.prostheticItems.all {
        it.patientId in patientIds && it.type in VALID_PROSTHETIC_TYPES &&
            it.material in VALID_PROSTHETIC_MATERIALS && it.stage in VALID_PROSTHETIC_STAGES
    }) { "Некорректные данные протезной конструкции" }
    require(data.diagnoses.all { it.patientId in patientIds }) {
        "Диагноз ссылается на несуществующего пациента"
    }
    require(data.treatmentPlans.all { it.patientId in patientIds }) {
        "План лечения ссылается на несуществующего пациента"
    }
    require(data.visits.all { it.patientId in patientIds && it.visitDate > 0 }) {
        "Некорректные данные визита"
    }
    require(data.visitPositions.all {
        it.visitId in visitIds && it.positionId in positionIds && it.selectedAt > 0 && it.sortOrder >= 0
    }) { "Позиция визита ссылается на несуществующую запись" }
    require(data.positions.all { it.name.isNotBlank() && it.sortOrder >= 0 }) {
        "Некорректная позиция"
    }
    require(data.invoices.all {
        it.patientId in patientIds && it.status in VALID_INVOICE_STATUSES &&
            it.discountPercent.isFinite() && it.discountPercent in 0.0..100.0 &&
            it.totalBeforeDiscount >= 0 && it.discountAmount >= 0 && it.totalAfterDiscount >= 0 &&
            it.totalBeforeDiscount - it.totalAfterDiscount == it.discountAmount
    }) { "Некорректные данные счёта" }
    require(data.invoiceItems.all {
        it.invoiceId in invoiceIds && it.serviceName.isNotBlank() && it.quantity > 0 &&
            it.unitPrice >= 0 && it.lineTotal >= 0 && it.lineTotal % it.quantity == 0L &&
            it.lineTotal / it.quantity == it.unitPrice
    }) { "Некорректная позиция счёта" }
    require(data.priceListItems.all { it.category.isNotBlank() && it.name.isNotBlank() && it.defaultPrice >= 0 && it.sortOrder >= 0 }) {
        "Некорректная услуга прайс-листа"
    }

    val toothKeys = data.teeth.map { it.patientId to it.number }
    require(toothKeys.size == toothKeys.distinct().size) { "Обнаружены повторяющиеся зубы пациента" }
}

private fun validateUniqueIds(name: String, ids: List<Long>) {
    require(ids.all { it > 0 }) { "Некорректный идентификатор: $name" }
    require(ids.size == ids.distinct().size) { "Обнаружены повторяющиеся идентификаторы: $name" }
}

private const val SUPPORTED_VERSION = 2
private const val MAX_IMPORT_CHARS = 25 * 1024 * 1024
private val snapshotJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}
private val REQUIRED_SECTIONS = setOf(
    "version",
    "patients",
    "appointments",
    "teeth",
    "prostheticItems",
    "diagnoses",
    "treatmentPlans",
    "visits",
    "visitPositions",
    "positions",
    "invoices",
    "invoiceItems",
    "priceListItems"
)
private val VALID_FDI_NUMBERS = buildSet {
    addAll(11..18)
    addAll(21..28)
    addAll(31..38)
    addAll(41..48)
}
private val VALID_SEXES = Sex.values().mapTo(mutableSetOf()) { it.name }
private val VALID_APPOINTMENT_TYPES = AppointmentType.values().mapTo(mutableSetOf()) { it.name }
private val VALID_APPOINTMENT_STATUSES = AppointmentStatus.values().mapTo(mutableSetOf()) { it.name }
private val VALID_ARCHES = Arch.values().mapTo(mutableSetOf()) { it.name }
private val VALID_TOOTH_STATUSES = ToothStatus.values().mapTo(mutableSetOf()) { it.name }
private val VALID_CROWN_OPTIONS = CrownOption.values().mapTo(mutableSetOf()) { it.name }
private val VALID_ROOT_OPTIONS = RootOption.values().mapTo(mutableSetOf()) { it.name }
private val VALID_PROSTHETIC_TYPES = ProstheticType.values().mapTo(mutableSetOf()) { it.name }
private val VALID_PROSTHETIC_MATERIALS = ProstheticMaterial.values().mapTo(mutableSetOf()) { it.name }
private val VALID_PROSTHETIC_STAGES = ProstheticStage.values().mapTo(mutableSetOf()) { it.name }
private val VALID_INVOICE_STATUSES = InvoiceStatus.values().mapTo(mutableSetOf()) { it.name }
