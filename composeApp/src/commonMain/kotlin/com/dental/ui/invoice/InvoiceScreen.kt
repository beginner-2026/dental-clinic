package com.dental.ui.invoice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dental.data.InvoiceCalculator
import com.dental.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    invoiceParam: Invoice?,
    onSave: (Invoice) -> Unit,
    onAddItem: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    var currentInvoice by remember(invoiceParam) { mutableStateOf(invoiceParam ?: Invoice(patientId = 1L)) }
    var discountInput by remember(invoiceParam) { mutableStateOf(invoiceParam?.discountPercent?.toString() ?: "0") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPriceList by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice / Treatment Plan") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                InvoiceStatus.entries.forEach { status ->
                    FilterChip(
                        selected = currentInvoice.status == status,
                        onClick = { currentInvoice = currentInvoice.copy(status = status) },
                        label = { Text(status.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Items list
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text("Service", Modifier.weight(2f), style = MaterialTheme.typography.labelMedium)
                        Text("Qty", Modifier.weight(0.5f), style = MaterialTheme.typography.labelMedium)
                        Text("Price", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text("Total", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    }
                    HorizontalDivider()
                }

                items(currentInvoice.items) { item ->
                    InvoiceItemRow(
                        item = item,
                        onQuantityChange = { qty ->
                            val updatedItems = currentInvoice.items.map {
                                if (it.id == item.id) it.copy(quantity = qty) else it
                            }
                            currentInvoice = InvoiceCalculator.recalculate(
                                currentInvoice.copy(items = updatedItems)
                            )
                        },
                        onDelete = {
                            currentInvoice = currentInvoice.copy(
                                items = currentInvoice.items.filter { it.id != item.id }
                            )
                        }
                    )
                }

                item {
                    if (currentInvoice.items.isEmpty()) {
                        Text(
                            text = "No items. Tap + to add a service.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Discount section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discount %:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { input ->
                                discountInput = input
                                val pct = input.toFloatOrNull() ?: 0f
                                currentInvoice = InvoiceCalculator.recalculate(
                                    currentInvoice.copy(discountPercent = pct.coerceIn(0f, 100f))
                                )
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Totals
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            TotalRow("Subtotal:", currentInvoice.totalBeforeDiscount)
                            if (currentInvoice.discountAmount > 0) {
                                TotalRow(
                                    "Discount (-${currentInvoice.discountPercent}%):",
                                    -currentInvoice.discountAmount,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            TotalRow("Total:", currentInvoice.totalAfterDiscount, bold = true)
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }

            // Bottom save button
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onSave(InvoiceCalculator.recalculate(currentInvoice)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Save Invoice")
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { serviceName, quantity, unitPrice ->
                val newItem = InvoiceItem(
                    id = currentInvoice.items.size + 1L,
                    invoiceId = currentInvoice.id,
                    serviceName = serviceName,
                    quantity = quantity,
                    unitPrice = unitPrice
                )
                val updated = currentInvoice.copy(items = currentInvoice.items + newItem)
                currentInvoice = InvoiceCalculator.recalculate(updated)
                showAddDialog = false
            },
            onOpenPriceList = {
                showAddDialog = false
                showPriceList = true
            }
        )
    }

    // Price List Dialog
    if (showPriceList) {
        PriceListDialog(
            onDismiss = { showPriceList = false },
            onSelect = { item ->
                val newItem = InvoiceItem(
                    id = currentInvoice.items.size + 1L,
                    invoiceId = currentInvoice.id,
                    serviceName = item.name,
                    serviceCode = item.code,
                    quantity = 1,
                    unitPrice = item.defaultPrice
                )
                val updated = currentInvoice.copy(items = currentInvoice.items + newItem)
                currentInvoice = InvoiceCalculator.recalculate(updated)
                showPriceList = false
            }
        )
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int, Long) -> Unit,
    onOpenPriceList: () -> Unit
) {
    var serviceName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Service") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Service name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() }.let { if (it.isEmpty()) "1" else it } },
                        label = { Text("Qty") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = unitPrice,
                        onValueChange = { unitPrice = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price (som)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenPriceList,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select from Price List")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    val price = (unitPrice.toFloatOrNull()?.let { (it * 100).toLong() } ?: 0L)
                    onAdd(serviceName, qty, price)
                },
                enabled = serviceName.isNotBlank() && unitPrice.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PriceListDialog(
    onDismiss: () -> Unit,
    onSelect: (PriceListItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) { PriceList.search(searchQuery) }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Price List") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(items) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        item.code,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatPrice(item.defaultPrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InvoiceItemRow(
    item: InvoiceItem,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.serviceName,
            Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "${item.quantity}",
            Modifier.weight(0.5f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = formatPrice(item.unitPrice),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = formatPrice(item.lineTotal),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    amount: Long,
    bold: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
        Text(
            text = formatPrice(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

private fun formatPrice(amount: Long): String {
    val units = amount / 100
    val cents = amount % 100
    return "$units.${if (cents < 10) "0" else ""}$cents"
}
