package com.dental.ui.invoice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dental.data.InvoiceCalculator
import com.dental.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    invoiceParam: Invoice?,
    patients: List<Patient>,
    onSave: (Invoice) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    var currentInvoice by remember(invoiceParam) { mutableStateOf(invoiceParam ?: Invoice(patientId = 0L)) }
    var discountInput by remember(invoiceParam) { mutableStateOf(invoiceParam?.discountPercent?.toString() ?: "0") }
    var showPriceList by remember { mutableStateOf(false) }
    var showPatientSelector by remember { mutableStateOf(false) }
    var patientSearch by remember { mutableStateOf("") }
    var selectedPatient by remember(invoiceParam) {
        mutableStateOf(invoiceParam?.let { inv -> patients.find { it.id == inv.patientId } })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Счёт / План лечения") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPriceList = true }) {
                Icon(Icons.Default.Add, contentDescription = "Выбрать из прайс-листа")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Patient selector
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPatientSelector = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Пациент:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedPatient?.let { "${it.lastName} ${it.firstName}" } ?: "Не выбран",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPatient != null) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.error
                    )
                }
            }

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

            // Status label
            Text(
                text = "Статус: ${if (currentInvoice.id != 0L) "Счёт #${currentInvoice.id}" else "Новый счёт"}" +
                       "  |  Позиций: ${currentInvoice.items.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Items list
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                if (currentInvoice.items.isEmpty()) {
                    item {
                        Text(
                            "Нет позиций. Нажмите + чтобы добавить услугу.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                Text(
                                    "Новый список",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                                currentInvoice.items.forEachIndexed { index, item ->
                                    InvoiceItemRow(
                                        item = item,
                                        onQuantityChange = { qty ->
                                            val updatedList = currentInvoice.items.toMutableList()
                                            updatedList[index] = item.copy(quantity = qty)
                                            currentInvoice = InvoiceCalculator.recalculate(
                                                currentInvoice.copy(items = updatedList)
                                            )
                                        },
                                        onDelete = {
                                            val updatedList = currentInvoice.items.toMutableList()
                                            updatedList.removeAt(index)
                                            currentInvoice = InvoiceCalculator.recalculate(
                                                currentInvoice.copy(items = updatedList)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {

                    Spacer(Modifier.height(8.dp))

                    // Discount section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Скидка %:", style = MaterialTheme.typography.bodyMedium)
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
                            TotalRow("Подытог:", currentInvoice.totalBeforeDiscount)
                            if (currentInvoice.discountAmount > 0) {
                                TotalRow(
                                    "Скидка (-${currentInvoice.discountPercent}%):",
                                    -currentInvoice.discountAmount,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            TotalRow("Итого:", currentInvoice.totalAfterDiscount, bold = true)
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
                    onClick = {
                        val invWithPatient = if (selectedPatient != null) {
                            currentInvoice.copy(patientId = selectedPatient!!.id)
                        } else currentInvoice
                        onSave(InvoiceCalculator.recalculate(invWithPatient))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = selectedPatient != null
                ) {
                    Text("Сохранить счёт")
                }
            }
        }
    }

    // Price List Dialog
    if (showPriceList) {
        PriceListDialog(
            onDismiss = { showPriceList = false },
            onSelect = { item, qty ->
                val newItem = InvoiceItem(
                    serviceName = item.name,
                    serviceCode = item.code,
                    quantity = qty,
                    unitPrice = item.defaultPrice
                )
                val updated = currentInvoice.copy(items = currentInvoice.items + newItem)
                currentInvoice = InvoiceCalculator.recalculate(updated)
                showPriceList = false
            }
        )
    }

    // Patient Selector Dialog
    if (showPatientSelector) {
        AlertDialog(
            onDismissRequest = { showPatientSelector = false },
            title = { Text("Выбрать пациента") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    OutlinedTextField(
                        value = patientSearch,
                        onValueChange = { patientSearch = it },
                        label = { Text("Поиск") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    val filtered = remember(patients, patientSearch) {
                        if (patientSearch.isBlank()) patients
                        else patients.filter {
                            it.lastName.contains(patientSearch, ignoreCase = true) ||
                            it.firstName.contains(patientSearch, ignoreCase = true)
                        }
                    }
                    LazyColumn {
                        items(filtered) { patient ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPatient = patient
                                        currentInvoice = currentInvoice.copy(patientId = patient.id)
                                        showPatientSelector = false
                                        patientSearch = ""
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text("${patient.lastName} ${patient.firstName}")
                            }
                            HorizontalDivider()
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    "Пациенты не найдены",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showPatientSelector = false
                    patientSearch = ""
                }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun PriceListDialog(
    onDismiss: () -> Unit,
    onSelect: (PriceListItem, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    val filtered = remember(searchQuery) { PriceList.search(searchQuery) }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Выбрать из прайс-листа", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Готово") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Text("Количество:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() }.let { if (it.isEmpty()) "1" else it } },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    grouped.forEach { (category, group) ->
                        item(key = "header_$category") {
                            Text(category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        items(group, key = { it.id }) { plItem ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(plItem.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(plItem.code ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(formatPrice(plItem.defaultPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(onClick = { onSelect(plItem, quantity.toIntOrNull() ?: 1) }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                    Text("+", fontSize = 14.sp)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceItemRow(
    item: InvoiceItem,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.serviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Кол-во:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = item.quantity.toString(),
                    onValueChange = { text ->
                        val qty = text.filter { it.isDigit() }.toIntOrNull() ?: 1
                        onQuantityChange(qty)
                    },
                    modifier = Modifier.width(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${formatPrice(item.unitPrice)} × ${item.quantity} = ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatPrice(item.lineTotal),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
