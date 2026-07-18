package com.dental.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val patients: List<BackupPatient> = emptyList(),
    val appointments: List<BackupAppointment> = emptyList(),
    val teeth: List<BackupTooth> = emptyList(),
    val prostheticItems: List<BackupProstheticItem> = emptyList(),
    val diagnoses: List<BackupDiagnosis> = emptyList(),
    val treatmentPlans: List<BackupTreatmentPlan> = emptyList(),
    val visits: List<BackupVisit> = emptyList(),
    val visitPositions: List<BackupVisitPosition> = emptyList(),
    val positions: List<BackupPosition> = emptyList(),
    val invoices: List<BackupInvoice> = emptyList(),
    val invoiceItems: List<BackupInvoiceItem> = emptyList(),
    val priceListItems: List<BackupPriceListItem> = emptyList()
)

@Serializable
data class BackupPatient(
    val id: Long,
    val lastName: String,
    val firstName: String,
    val middleName: String? = null,
    val birthDate: Long? = null,
    val phone: String? = null,
    val email: String? = null,
    val sex: String,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupAppointment(
    val id: Long,
    val patientId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Long,
    val doctorId: Long? = null,
    val chairId: Long? = null,
    val type: String,
    val status: String,
    val note: String? = null
)

@Serializable
data class BackupTooth(
    val id: Long,
    val patientId: Long,
    val number: Int,
    val arch: String,
    val quadrant: Int,
    val status: String,
    val examType: String? = null,
    val crownOption: String? = null,
    val rootOption: String? = null
)

@Serializable
data class BackupProstheticItem(
    val id: Long,
    val patientId: Long,
    val toothIds: String,
    val type: String,
    val material: String,
    val stage: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupDiagnosis(
    val id: Long,
    val patientId: Long,
    val code: String? = null,
    val diagnosisText: String,
    val toothNumber: String? = null,
    val createdAt: Long
)

@Serializable
data class BackupTreatmentPlan(
    val id: Long,
    val patientId: Long,
    val toothNumbers: String,
    val procedure: String,
    val createdAt: Long
)

@Serializable
data class BackupVisit(
    val id: Long,
    val patientId: Long,
    val visitDate: Long,
    val createdAt: Long
)

@Serializable
data class BackupVisitPosition(
    val id: Long,
    val visitId: Long,
    val positionId: Long,
    val toothNumbers: String,
    val selectedAt: Long,
    val sortOrder: Long
)

@Serializable
data class BackupPosition(
    val id: Long,
    val name: String,
    val sortOrder: Long
)

@Serializable
data class BackupInvoice(
    val id: Long,
    val patientId: Long,
    val title: String? = null,
    val dateCreated: Long,
    val dateUpdated: Long,
    val status: String,
    val discountPercent: Double,
    val totalBeforeDiscount: Long,
    val discountAmount: Long,
    val totalAfterDiscount: Long
)

@Serializable
data class BackupInvoiceItem(
    val id: Long,
    val invoiceId: Long,
    val serviceName: String,
    val serviceCode: String? = null,
    val quantity: Long,
    val unitPrice: Long,
    val lineTotal: Long
)

@Serializable
data class BackupPriceListItem(
    val id: Long,
    val category: String,
    val name: String,
    val defaultPrice: Long,
    val sortOrder: Long
)
