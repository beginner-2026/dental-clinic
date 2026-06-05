package com.dental.data

import com.dental.model.Invoice
import com.dental.model.InvoiceItem

object InvoiceCalculator {
    fun recalculate(invoice: Invoice): Invoice {
        val items = invoice.items.map { item ->
            item.copy(lineTotal = item.unitPrice * item.quantity)
        }
        val totalBefore = items.sumOf { it.lineTotal }
        val discountAmount = (totalBefore * invoice.discountPercent / 100f).toLong()
        val totalAfter = totalBefore - discountAmount
        return invoice.copy(
            items = items,
            totalBeforeDiscount = totalBefore,
            discountAmount = discountAmount,
            totalAfterDiscount = totalAfter
        )
    }
}
