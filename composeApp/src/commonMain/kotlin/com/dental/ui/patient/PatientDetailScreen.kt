package com.dental.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.dental.data.InvoiceRepository
import com.dental.model.*
import com.dental.ui.odontogram.OdontogramLayers
import com.dental.ui.odontogram.OdontogramViewModel
import com.dental.ui.odontogram.QuadrantOdontogram
import com.dental.ui.odontogram.ToothActionSheet
import com.dental.ui.odontogram.ToothPartSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patient: Patient,
    odontogramViewModel: OdontogramViewModel,
    invoiceRepository: InvoiceRepository,
    onBack: () -> Unit
) {
    val state by odontogramViewModel.state.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    var isQuadrantView by remember { mutableStateOf(false) }

    var currentInvoice by remember { mutableStateOf<Invoice?>(null) }
    var discountInput by remember { mutableStateOf("0") }
    var showPriceListDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(patient.id) {
        odontogramViewModel.loadPatientData(
            patientId = patient.id,
            patientName = "${patient.lastName} ${patient.firstName}"
        )
        val existing = invoiceRepository.getByPatient(patient.id)
        if (existing != null) {
            currentInvoice = existing
            discountInput = existing.discountPercent.toString()
        } else {
            currentInvoice = Invoice(patientId = patient.id)
            discountInput = "0"
        }
    }

    fun updateInvoice(newInvoice: Invoice) {
        currentInvoice = newInvoice
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${patient.lastName} ${patient.firstName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${patient.lastName} ${patient.firstName} ${patient.middleName ?: ""}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (patient.birthDate != null) {
                        Text("Дата рождения: ${patient.birthDate}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (patient.phone != null) {
                        Text("Телефон: ${patient.phone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (patient.notes != null) {
                        Text("Заметки: ${patient.notes}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Медицинская карта") }, icon = { Icon(Icons.Default.Favorite, contentDescription = null) })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Счёт") }, icon = { Icon(Icons.Default.DateRange, contentDescription = null) })
            }

            when (tabIndex) {
                0 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Зубная формула",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isQuadrantView,
                            onClick = { isQuadrantView = false },
                            label = { Text("Полная", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = isQuadrantView,
                            onClick = { isQuadrantView = true },
                            label = { Text("По квадратам", fontSize = 12.sp) }
                        )
                    }
                    if (isQuadrantView) {
                        QuadrantOdontogram(
                            teeth = state.teeth,
                            prostheticItems = state.prostheticItems,
                            selectedTooth = state.selectedTooth,
                            selectedToothPart = state.selectedToothPart,
                            activeLayer = state.activeLayer,
                            onToothClick = { number, part -> odontogramViewModel.selectTooth(number, part) },
                            crownSelections = state.crownSelections,
                            rootSelections = state.rootSelections,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    } else {
                        OdontogramLayers(
                            teeth = state.teeth,
                            prostheticItems = state.prostheticItems,
                            selectedTooth = state.selectedTooth,
                            selectedToothPart = state.selectedToothPart,
                            activeLayer = state.activeLayer,
                            onToothClick = { number, part -> odontogramViewModel.selectTooth(number, part) },
                            crownSelections = state.crownSelections,
                            rootSelections = state.rootSelections,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                }
                1 -> {
                    currentInvoice?.let { inv ->
                        InvoiceTabContent(
                            invoice = inv,
                            discountInput = discountInput,
                            onDiscountInputChange = { discountInput = it },
                            onInvoiceUpdate = { updateInvoice(it) },
                            onOpenPriceList = { showPriceListDialog = true },
                            onDeleteInvoiceClick = { showDeleteConfirm = true },
                            onSave = {
                                val toSave = currentInvoice ?: return@InvoiceTabContent
                                val savedId = invoiceRepository.save(InvoiceCalculator.recalculate(toSave))
                                if (toSave.id == 0L) {
                                    val loaded = invoiceRepository.getByPatient(patient.id)
                                    if (loaded != null) {
                                        updateInvoice(loaded)
                                        discountInput = loaded.discountPercent.toString()
                                    } else {
                                        updateInvoice(toSave.copy(id = savedId))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Tooth part bottom sheet (crown / root selection)
    if (state.showToothPartMenu && state.selectedTooth != null && state.selectedToothPart != null) {
        ToothPartSheet(
            toothNumber = state.selectedTooth!!,
            part = state.selectedToothPart!!,
            onDismiss = { odontogramViewModel.dismissPartMenu() },
            onApplyCrown = { option -> odontogramViewModel.applyCrownOption(state.selectedTooth!!, option) },
            onApplyRoot = { option -> odontogramViewModel.applyRootOption(state.selectedTooth!!, option) }
        )
    }

    // Tooth action bottom sheet (prosthetic selection)
    if (state.showToothMenu && state.selectedTooth != null) {
        val tooth = state.teeth.find { it.number == state.selectedTooth }
        ToothActionSheet(
            toothNumber = state.selectedTooth!!,
            toothStatus = tooth?.status ?: ToothStatus.PRESENT,
            existingProsthetics = state.prostheticItems,
            isBridgeMode = state.bridgeMode,
            onDismiss = { odontogramViewModel.dismissMenu() },
            onSelectProsthetic = { type, material, stage ->
                odontogramViewModel.applyProsthetic(type, material, stage)
            },
            onToggleBridgeMode = { odontogramViewModel.toggleBridgeMode() },
            onChangeToothStatus = { status -> odontogramViewModel.setToothStatus(state.selectedTooth!!, status) }
        )
    }

    if (showPriceListDialog) {
        PriceListSelectionDialog(
            onDismiss = { showPriceListDialog = false },
            onSelect = { plItem, qty ->
                currentInvoice?.let { inv ->
                    val newItem = InvoiceItem(
                        serviceName = plItem.name,
                        serviceCode = plItem.code,
                        quantity = qty,
                        unitPrice = plItem.defaultPrice
                    )
                    val updated = InvoiceCalculator.recalculate(inv.copy(items = inv.items + newItem))
                    updateInvoice(updated)
                }
                showPriceListDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить счёт") },
            text = { Text("Вы уверены, что хотите удалить этот счёт? Все позиции будут удалены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val inv = currentInvoice
                        if (inv != null && inv.id != 0L) {
                            invoiceRepository.delete(inv.id)
                        }
                        updateInvoice(Invoice(patientId = patient.id))
                        discountInput = "0"
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun InvoiceTabContent(
    invoice: Invoice,
    discountInput: String,
    onDiscountInputChange: (String) -> Unit,
    onInvoiceUpdate: (Invoice) -> Unit,
    onOpenPriceList: () -> Unit,
    onDeleteInvoiceClick: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onOpenPriceList) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Выбрать из Прейскуранта")
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (invoice.id != 0L) "Счёт #${invoice.id}" else "Новый счёт",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onDeleteInvoiceClick,
                enabled = invoice.id != 0L || invoice.items.isNotEmpty()
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить счёт", tint = Color.Red)
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            // === Status section ===
            Text(
                text = "Статус: ${if (invoice.id != 0L) "Счёт #${invoice.id}" else "Новый счёт"}" +
                       "  |  Позиций: ${invoice.items.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // === Новый список section ===
            if (invoice.items.isEmpty()) {
                Text(
                    "Нет позиций. Нажмите «Выбрать из Прейскуранта» чтобы добавить услугу.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
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
                        invoice.items.forEachIndexed { index, item ->
                            InvoiceItemRow(
                                item = item,
                                onQuantityChange = { qty ->
                                    val updated = invoice.items.toMutableList()
                                    updated[index] = item.copy(quantity = qty)
                                    onInvoiceUpdate(InvoiceCalculator.recalculate(invoice.copy(items = updated)))
                                },
                                onDelete = {
                                    val updated = invoice.items.toMutableList()
                                    updated.removeAt(index)
                                    onInvoiceUpdate(InvoiceCalculator.recalculate(invoice.copy(items = updated)))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // === Discount ===
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Скидка %:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { input ->
                        onDiscountInputChange(input)
                        val pct = input.toFloatOrNull() ?: 0f
                        onInvoiceUpdate(InvoiceCalculator.recalculate(
                            invoice.copy(discountPercent = pct.coerceIn(0f, 100f))
                        ))
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(Modifier.height(8.dp))

            // === Totals ===
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    TotalRow("Подытог:", invoice.totalBeforeDiscount)
                    if (invoice.discountAmount > 0) {
                        TotalRow(
                            "Скидка (-${invoice.discountPercent}%):",
                            -invoice.discountAmount,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    TotalRow("Итого:", invoice.totalAfterDiscount, bold = true)
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        Surface(tonalElevation = 3.dp) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Сохранить счёт")
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
private fun PriceListSelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (PriceListItem, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    val filtered = remember(searchQuery) { PriceList.search(searchQuery) }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    Dialog(
        onDismissRequest = onDismiss
    ) {
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
private fun TotalRow(
    label: String,
    amount: Long,
    bold: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface
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
