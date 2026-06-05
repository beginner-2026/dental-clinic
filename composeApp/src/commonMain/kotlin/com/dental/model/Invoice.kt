package com.dental.model

data class Invoice(
    val id: Long = 0,
    val patientId: Long,
    val title: String? = null,
    val dateCreated: Long = 0,
    val dateUpdated: Long = 0,
    val status: InvoiceStatus = InvoiceStatus.NEW,
    val discountPercent: Float = 0f,
    val totalBeforeDiscount: Long = 0,
    val discountAmount: Long = 0,
    val totalAfterDiscount: Long = 0,
    val items: List<InvoiceItem> = emptyList()
)

enum class InvoiceStatus { NEW, PAID, COMPLETED }

data class InvoiceItem(
    val id: Long = 0,
    val invoiceId: Long = 0,
    val serviceName: String,
    val serviceCode: String? = null,
    val quantity: Int = 1,
    val unitPrice: Long,
    val lineTotal: Long = 0
)

data class AppointmentWithPatient(
    val appointment: Appointment,
    val patientName: String
)
