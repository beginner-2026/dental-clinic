package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.data.db.InvoiceEntity
import com.dental.data.db.InvoiceItemEntity
import com.dental.model.Invoice
import com.dental.model.InvoiceItem
import com.dental.model.InvoiceStatus
import kotlinx.datetime.Clock

class InvoiceRepository(db: DentalDatabase) {

    private val queries = db.invoiceQueries

    fun getByPatient(patientId: Long): Invoice? {
        val entity = queries.getInvoicesByPatient(patientId).executeAsOneOrNull() ?: return null
        val items = queries.getItemsByInvoiceId(entity.id).executeAsList().map { it.toInvoiceItem() }
        return entity.toInvoice(items)
    }

    fun getAllByPatient(patientId: Long): List<Invoice> {
        return queries.getInvoicesByPatient(patientId).executeAsList().map { entity ->
            val items = queries.getItemsByInvoiceId(entity.id).executeAsList().map { it.toInvoiceItem() }
            entity.toInvoice(items)
        }
    }

    fun save(invoice: Invoice): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        if (invoice.id == 0L) {
            var invoiceId = 0L
            queries.transaction {
                queries.insertInvoice(
                    patientId = invoice.patientId,
                    title = invoice.title,
                    dateCreated = now,
                    dateUpdated = now,
                    status = invoice.status.name,
                    discountPercent = invoice.discountPercent.toDouble(),
                    totalBeforeDiscount = invoice.totalBeforeDiscount,
                    discountAmount = invoice.discountAmount,
                    totalAfterDiscount = invoice.totalAfterDiscount
                )
                invoiceId = queries.getLastInsertId().executeAsOne()
                invoice.items.forEach { item ->
                    queries.insertInvoiceItem(
                        invoiceId = invoiceId,
                        serviceName = item.serviceName,
                        serviceCode = item.serviceCode,
                        quantity = item.quantity.toLong(),
                        unitPrice = item.unitPrice,
                        lineTotal = item.lineTotal
                    )
                }
            }
            return invoiceId
        } else {
            queries.transaction {
                queries.deleteItemsByInvoiceId(invoice.id)
                invoice.items.forEach { item ->
                    queries.insertInvoiceItem(
                        invoiceId = invoice.id,
                        serviceName = item.serviceName,
                        serviceCode = item.serviceCode,
                        quantity = item.quantity.toLong(),
                        unitPrice = item.unitPrice,
                        lineTotal = item.lineTotal
                    )
                }
                queries.updateInvoice(
                    dateUpdated = now,
                    status = invoice.status.name,
                    discountPercent = invoice.discountPercent.toDouble(),
                    totalBeforeDiscount = invoice.totalBeforeDiscount,
                    discountAmount = invoice.discountAmount,
                    totalAfterDiscount = invoice.totalAfterDiscount,
                    id = invoice.id
                )
            }
            return invoice.id
        }
    }

    fun delete(invoiceId: Long) {
        queries.transaction {
            queries.deleteItemsByInvoiceId(invoiceId)
            queries.deleteInvoice(invoiceId)
        }
    }

    private fun InvoiceEntity.toInvoice(items: List<InvoiceItem>): Invoice {
        return Invoice(
            id = id,
            patientId = patientId,
            title = title,
            dateCreated = dateCreated,
            dateUpdated = dateUpdated,
            status = InvoiceStatus.valueOf(status),
            discountPercent = discountPercent.toFloat(),
            totalBeforeDiscount = totalBeforeDiscount,
            discountAmount = discountAmount,
            totalAfterDiscount = totalAfterDiscount,
            items = items
        )
    }

    private fun InvoiceItemEntity.toInvoiceItem(): InvoiceItem {
        return InvoiceItem(
            id = id,
            invoiceId = invoiceId,
            serviceName = serviceName,
            serviceCode = serviceCode,
            quantity = quantity.toInt(),
            unitPrice = unitPrice,
            lineTotal = lineTotal
        )
    }
}
