package com.dental.ui.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dental.data.DiagnosisRepository
import com.dental.data.InvoiceCalculator
import com.dental.data.InvoiceRepository
import com.dental.data.TreatmentPlanRepository
import com.dental.data.PriceListRepository
import com.dental.data.VisitPositionRepository
import com.dental.model.*
import com.dental.ui.diagnosis.DiagnosisEditorScreen
import kotlinx.datetime.*
import com.dental.ui.treatmentplan.TreatmentPlanEditorScreen
import com.dental.ui.odontogram.OdontogramLayers
import com.dental.ui.odontogram.OdontogramViewModel
import com.dental.ui.odontogram.QuadrantOdontogram
import com.dental.ui.odontogram.ToothActionSheet
import com.dental.ui.odontogram.ToothPartSheet

private class ToothPainter : Painter() {
    override val intrinsicSize: Size get() = Size(24f, 24f)

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.25f, h * 0.2f)
            cubicTo(w * 0.25f, h * 0.05f,
                w * 0.75f, h * 0.05f,
                w * 0.75f, h * 0.2f)
            lineTo(w * 0.75f, h * 0.45f)
            lineTo(w * 0.6f, h * 0.45f)
            lineTo(w * 0.6f, h * 0.85f)
            cubicTo(w * 0.6f, h * 0.95f,
                w * 0.4f, h * 0.95f,
                w * 0.4f, h * 0.85f)
            lineTo(w * 0.4f, h * 0.45f)
            close()
        }
        drawPath(path, color = Color.Black, style = Stroke(width = 2f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patient: Patient,
    odontogramViewModel: OdontogramViewModel,
    invoiceRepository: InvoiceRepository,
    diagnosisRepository: DiagnosisRepository,
    treatmentPlanRepository: TreatmentPlanRepository,
    visitPositionRepository: VisitPositionRepository,
    priceListRepository: PriceListRepository,
    onBack: () -> Unit
) {
    val state by odontogramViewModel.state.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    var isQuadrantView by remember { mutableStateOf(false) }

    var currentInvoice by remember { mutableStateOf<Invoice?>(null) }
    var allInvoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var showInvoiceEditor by remember { mutableStateOf(false) }
    var discountInput by remember { mutableStateOf("0") }
    var showPriceListDialog by remember { mutableStateOf(false) }
    var showEditInvoiceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var diagnoses by remember { mutableStateOf<List<Diagnosis>>(emptyList()) }
    var showDiagnosisEditor by remember { mutableStateOf(false) }

    var treatmentPlans by remember { mutableStateOf<List<TreatmentPlanItem>>(emptyList()) }
    var showTreatmentPlanEditor by remember { mutableStateOf(false) }

    var visits by remember { mutableStateOf<List<Visit>>(emptyList()) }
    var allPositions by remember { mutableStateOf<List<Position>>(emptyList()) }
    var showAddVisitDialog by remember { mutableStateOf(false) }
    var showPositionSelectionDialog by remember { mutableStateOf(false) }
    var editingVisit by remember { mutableStateOf<Visit?>(null) }
    var selectedPositionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var positionToothNumbers by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var visitDateMillis by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var showVisitDatePicker by remember { mutableStateOf(false) }

    fun loadVisits() {
        visits = visitPositionRepository.getVisitsByPatientId(patient.id)
    }

    fun loadAllPositions() {
        allPositions = visitPositionRepository.getAllPositions()
    }

    fun loadDiagnoses() {
        diagnoses = diagnosisRepository.getByPatientId(patient.id)
    }

    fun loadTreatmentPlans() {
        treatmentPlans = treatmentPlanRepository.getByPatientId(patient.id)
    }

    LaunchedEffect(patient.id) {
        odontogramViewModel.loadPatientData(
            patientId = patient.id,
            patientName = "${patient.lastName} ${patient.firstName}"
        )
        loadDiagnoses()
        loadTreatmentPlans()
        loadAllPositions()
        loadVisits()
        allInvoices = invoiceRepository.getAllByPatient(patient.id)
        val existing = allInvoices.firstOrNull()
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

    fun loadAllInvoices() {
        allInvoices = invoiceRepository.getAllByPatient(patient.id)
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
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${patient.lastName} ${patient.firstName} ${patient.middleName ?: ""}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (patient.phone != null) {
                            Text(patient.phone, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (patient.notes != null) {
                        Text("Заметки: ${patient.notes}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Медицинская карта") }, icon = { Icon(Icons.Default.Favorite, contentDescription = null) })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Прейскурант") }, icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) })
            }

            when (tabIndex) {
                0 -> {
                    val sections = listOf("Зубная формула", "Диагноз", "План лечения", "Дневник посещений", "Счёт")
                    var expandedSection by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        sections.forEach { section ->
                            val isExpanded = expandedSection == section
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp),
                                onClick = { expandedSection = if (isExpanded) "" else section },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (section == "Зубная формула") {
                                        Icon(
                                            painter = ToothPainter(),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        val icon = when (section) {
                                            "Диагноз" -> Icons.Default.LocalHospital
                                            "План лечения" -> Icons.Default.Checklist
                                            "Дневник посещений" -> Icons.Default.DateRange
                                            "Счёт" -> Icons.Default.MonetizationOn
                                            else -> Icons.Default.Info
                                        }
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = section,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть"
                                    )
                                }
                            }

                            if (isExpanded) {
                                when (section) {
                                    "Зубная формула" -> {
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
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(240.dp).padding(horizontal = 8.dp)
                                        ) {
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
                                                    modifier = Modifier.fillMaxSize()
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
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    odontogramViewModel.saveAll()
                                                },
                                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Сохранить зубную формулу")
                                            }
                                        }
                                    }
                                    "Диагноз" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Диагноз",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { showDiagnosisEditor = true }) {
                                                Icon(Icons.Default.Add, contentDescription = "Добавить диагноз")
                                            }
                                        }
                                        if (diagnoses.isEmpty()) {
                                            Text(
                                                text = "Нет диагнозов. Нажмите + чтобы добавить.",
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            diagnoses.forEach { d ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (d.toothNumber != null) {
                                                            Text(
                                                                text = "Зуб ${d.toothNumber}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.width(70.dp)
                                                            )
                                                        } else {
                                                            Spacer(Modifier.width(70.dp))
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            val label = if (d.code != null) "${d.code} — ${d.diagnosisText}" else d.diagnosisText
                                                            Text(
                                                                text = label,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    "План лечения" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "План лечения",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { showTreatmentPlanEditor = true }) {
                                                Icon(Icons.Default.Add, contentDescription = "Добавить пункт плана")
                                            }
                                        }
                                        if (treatmentPlans.isEmpty()) {
                                            Text(
                                                text = "Нет пунктов плана лечения. Нажмите + чтобы добавить.",
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            treatmentPlans.forEach { item ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (item.toothNumbers.isNotBlank()) {
                                                            Text(
                                                                text = item.toothNumbers,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.width(70.dp)
                                                            )
                                                        } else {
                                                            Spacer(Modifier.width(70.dp))
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = item.procedure,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    "Дневник посещений" -> {
                                        VisitDiarySection(
                                            visits = visits,
                                            onAddVisit = { showAddVisitDialog = true },
                                            onEditPositions = { visit ->
                                                editingVisit = visit
                                                selectedPositionIds = visit.positions.map { it.position.id }.toSet()
                                                positionToothNumbers = visit.positions.associate { it.position.id to it.toothNumbers }
                                                showPositionSelectionDialog = true
                                            },
                                            onDeleteVisit = { visit ->
                                                visitPositionRepository.deleteVisit(visit.id)
                                                loadVisits()
                                            }
                                        )
                                    }
                                    "Счёт" -> {
                                        if (showInvoiceEditor) {
                                            currentInvoice?.let { inv ->
                                                InvoiceTabContent(
                                                    invoice = inv,
                                                    discountInput = discountInput,
                                                    onDiscountInputChange = { discountInput = it },
                                                    onInvoiceUpdate = { updateInvoice(it) },
                                                    onOpenPriceList = { showPriceListDialog = true },
                                                    onEditInvoice = { showEditInvoiceDialog = true },
                                                    onDeleteInvoiceClick = { showDeleteConfirm = true },
                                                    onSave = {
                                                        val toSave = currentInvoice ?: return@InvoiceTabContent
                                                        val savedId = invoiceRepository.save(InvoiceCalculator.recalculate(toSave))
                                                        loadAllInvoices()
                                                        val saved = allInvoices.firstOrNull { it.id == savedId }
                                                        updateInvoice(saved ?: toSave.copy(id = savedId))
                                                        showInvoiceEditor = false
                                                    }
                                                )
                                            }
                                        } else {
                                            InvoiceHistoryView(
                                                key = allInvoices.size,
                                                invoices = allInvoices,
                                                onNewInvoice = {
                                                    currentInvoice = Invoice(patientId = patient.id)
                                                    discountInput = "0"
                                                    showInvoiceEditor = true
                                                    showPriceListDialog = true
                                                },
                                                onRefresh = { loadAllInvoices() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    PriceListTabContent(priceListRepository)
                }
            }
        }
    }

    if (showDiagnosisEditor) {
        DiagnosisEditorScreen(
            patientId = patient.id,
            existingDiagnoses = diagnoses,
            onSave = { saved ->
                diagnosisRepository.saveAll(patient.id, saved)
                loadDiagnoses()
                showDiagnosisEditor = false
            },
            onBack = { showDiagnosisEditor = false }
        )
    }

    if (showTreatmentPlanEditor) {
        TreatmentPlanEditorScreen(
            patientId = patient.id,
            existingItems = treatmentPlans,
            onSave = { saved ->
                treatmentPlanRepository.saveAll(patient.id, saved)
                loadTreatmentPlans()
                showTreatmentPlanEditor = false
            },
            onBack = { showTreatmentPlanEditor = false }
        )
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
            repository = priceListRepository,
            onDismiss = { showPriceListDialog = false },
            onSelect = { selections ->
                currentInvoice?.let { inv ->
                    val newItems = selections.map { (plItem, qty) ->
                        InvoiceItem(
                            serviceName = plItem.name,
                            quantity = qty,
                            unitPrice = plItem.defaultPrice
                        )
                    }
                    val updated = InvoiceCalculator.recalculate(inv.copy(items = inv.items + newItems))
                    updateInvoice(updated)
                }
                showPriceListDialog = false
            }
        )
    }

    if (showEditInvoiceDialog) {
        val currentItems = currentInvoice?.items ?: emptyList()
        PriceListSelectionDialog(
            repository = priceListRepository,
            initialItems = currentItems.map { it.serviceName to it.quantity },
            onDismiss = { showEditInvoiceDialog = false },
            onSelect = { selections ->
                currentInvoice?.let { inv ->
                    val newItems = selections.map { (plItem, qty) ->
                        InvoiceItem(
                            serviceName = plItem.name,
                            quantity = qty,
                            unitPrice = plItem.defaultPrice
                        )
                    }
                    val updated = InvoiceCalculator.recalculate(inv.copy(items = newItems))
                    updateInvoice(updated)
                }
                showEditInvoiceDialog = false
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

    if (showAddVisitDialog) {
        AddVisitDialog(
            initialDateMillis = visitDateMillis,
            onDateChanged = { visitDateMillis = it },
            onConfirm = {
                if (visitDateMillis > 0) {
                    visitPositionRepository.createVisit(patient.id, visitDateMillis)
                    visitDateMillis = Clock.System.now().toEpochMilliseconds()
                    showAddVisitDialog = false
                    loadVisits()
                }
            },
            onDismiss = {
                visitDateMillis = Clock.System.now().toEpochMilliseconds()
                showAddVisitDialog = false
            }
        )
    }

    if (showPositionSelectionDialog) {
        val visit = editingVisit
        if (visit != null) {
            PositionSelectionDialog(
                allPositions = allPositions,
                selectedPositionIds = selectedPositionIds,
                positionToothNumbers = positionToothNumbers,
                onTogglePosition = { posId ->
                    selectedPositionIds = if (posId in selectedPositionIds) {
                        selectedPositionIds - posId
                    } else {
                        selectedPositionIds + posId
                    }
                },
                onToothNumbersChanged = { posId, value ->
                    positionToothNumbers = positionToothNumbers + (posId to value)
                },
                onSave = {
                    visitPositionRepository.saveVisitPositions(
                        visitId = visit.id,
                        positionIds = selectedPositionIds.toList(),
                        toothNumbersMap = positionToothNumbers
                    )
                    showPositionSelectionDialog = false
                    editingVisit = null
                    loadVisits()
                },
                onReset = {
                    selectedPositionIds = emptySet()
                    positionToothNumbers = emptyMap()
                },
                onDismiss = {
                    showPositionSelectionDialog = false
                    editingVisit = null
                }
            )
        }
    }
}

@Composable
private fun InvoiceTabContent(
    invoice: Invoice,
    discountInput: String,
    onDiscountInputChange: (String) -> Unit,
    onInvoiceUpdate: (Invoice) -> Unit,
    onOpenPriceList: () -> Unit,
    onEditInvoice: () -> Unit,
    onDeleteInvoiceClick: () -> Unit,
    onSave: () -> Unit
) {
    var itemsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(invoice.items.size) {
        if (invoice.items.isNotEmpty()) itemsExpanded = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
            IconButton(onClick = onEditInvoice, enabled = invoice.items.isNotEmpty()) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать счёт")
            }
            IconButton(
                onClick = onDeleteInvoiceClick,
                enabled = invoice.id != 0L || invoice.items.isNotEmpty()
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить счёт", tint = Color.Red)
            }
        }

        // === Status section (clickable) ===
        val invoiceDate = if (invoice.dateCreated > 0) " от ${formatVisitDate(invoice.dateCreated)}" else ""
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(enabled = invoice.items.isNotEmpty()) { itemsExpanded = !itemsExpanded }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (invoice.id != 0L) "Счёт №${invoice.id}$invoiceDate" else "Новый счёт",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Позиций: ${invoice.items.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (invoice.items.isNotEmpty()) {
                Icon(
                    if (itemsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (itemsExpanded) "Свернуть" else "Развернуть",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // === Новый список section ===
        if (itemsExpanded && invoice.items.isNotEmpty()) {
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
                            onEdit = onEditInvoice,
                            onDelete = {
                                val updated = invoice.items.toMutableList()
                                updated.removeAt(index)
                                onInvoiceUpdate(InvoiceCalculator.recalculate(invoice.copy(items = updated)))
                            }
                        )
                    }
                }
            }
        } else if (invoice.items.isEmpty()) {
            Text(
                "Нет позиций. Нажмите «Выбрать из Прейскуранта» чтобы добавить услугу.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text("Сохранить счёт")
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InvoiceHistoryView(
    key: Int = 0,
    invoices: List<Invoice>,
    onNewInvoice: () -> Unit,
    onRefresh: () -> Unit
) {
    var expandedId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onNewInvoice) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Выбрать из Прейскуранта")
            }
            Spacer(Modifier.weight(1f))
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        if (invoices.isEmpty()) {
            Text(
                "Нет сохранённых счетов. Нажмите «Выбрать из Прейскуранта» чтобы создать первый счёт.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            invoices.forEach { inv ->
                val isExpanded = expandedId == inv.id
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { expandedId = if (isExpanded) null else inv.id },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Счёт №${inv.id} от ${formatVisitDate(inv.dateCreated)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Позиций: ${inv.items.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (inv.items.isNotEmpty()) {
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isExpanded && inv.items.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            inv.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.serviceName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        "${item.quantity} × ${formatPrice(item.unitPrice)} = ${formatPrice(item.lineTotal)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(
                                    text = "Итого: ${formatPrice(inv.totalAfterDiscount)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceListTabContent(repository: PriceListRepository) {
    var searchQuery by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<PriceListItem>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PriceListItem?>(null) }
    var addingCategory by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        items = repository.getAll()
    }

    val filtered = remember(searchQuery, items) {
        if (searchQuery.isBlank()) items
        else {
            val q = searchQuery.lowercase()
            items.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Поиск услуг...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (category, categoryItems) ->
                item(key = "cat_$category") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                        )
                        IconButton(
                            onClick = {
                                addingCategory = category
                                showAddDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                items(categoryItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        onClick = {
                            editingItem = item
                            showEditDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 18.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = _formatPrice(item.defaultPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    repository.delete(item.id)
                                    reloadKey++
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }
    }

    if (showEditDialog && editingItem != null) {
        _PriceItemEditDialog(
            title = "Редактировать услугу",
            initialName = editingItem!!.name,
            initialCategory = editingItem!!.category,
            initialPrice = editingItem!!.defaultPrice,
            showCategoryField = true,
            onSave = { name, category, price ->
                repository.update(editingItem!!.id, category, name, price)
                editingItem = null
                showEditDialog = false
                reloadKey++
            },
            onDismiss = {
                editingItem = null
                showEditDialog = false
            }
        )
    }

    if (showAddDialog) {
        _PriceItemEditDialog(
            title = "Добавить услугу",
            initialName = "",
            initialCategory = addingCategory,
            initialPrice = 0,
            showCategoryField = true,
            onSave = { name, category, price ->
                repository.create(category, name, price)
                showAddDialog = false
                reloadKey++
            },
            onDismiss = {
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun InvoiceItemRow(
    item: InvoiceItem,
    onQuantityChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.serviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Уменьшить", modifier = Modifier.size(16.dp))
                    }
                    Text("${item.quantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Увеличить", modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${formatPrice(item.unitPrice)} × ${item.quantity} = ${formatPrice(item.lineTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PriceListSelectionDialog(
    repository: PriceListRepository,
    initialItems: List<Pair<String, Int>> = emptyList(),
    onDismiss: () -> Unit,
    onSelect: (List<Pair<PriceListItem, Int>>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var allItems by remember { mutableStateOf<List<PriceListItem>>(emptyList()) }
    val selectedItems = remember { mutableStateMapOf<Long, Int>() }

    LaunchedEffect(Unit) {
        allItems = repository.getAll()
        selectedItems.clear()
        initialItems.forEach { (name, qty) ->
            allItems.firstOrNull { it.name == name }?.let { plItem ->
                selectedItems[plItem.id] = qty
            }
        }
    }

    val filtered = remember(searchQuery, allItems) {
        if (searchQuery.isBlank()) allItems
        else {
            val q = searchQuery.lowercase()
            allItems.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Выбрать из Прейскуранта", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

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
                            val qty = selectedItems[plItem.id] ?: 0
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = qty > 0,
                                    onCheckedChange = { checked ->
                                        selectedItems[plItem.id] = if (checked) 1 else 0
                                    }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(plItem.name, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(formatPrice(plItem.defaultPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                if (qty > 0) {
                                    IconButton(onClick = { if (qty > 1) selectedItems[plItem.id] = qty - 1 }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Remove, contentDescription = "Уменьшить", modifier = Modifier.size(18.dp))
                                    }
                                    Text("$qty", style = MaterialTheme.typography.bodyMedium)
                                    IconButton(onClick = { selectedItems[plItem.id] = qty + 1 }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = "Увеличить", modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Spacer(Modifier.width(72.dp))
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val selections = selectedItems.entries
                            .filter { it.value > 0 }
                            .mapNotNull { entry ->
                                allItems.find { it.id == entry.key }?.let { it to entry.value }
                            }
                        onSelect(selections)
                    }) { Text("Готово") }
                }
            }
        }
    }
}

@Composable
private fun VisitDiarySection(
    visits: List<Visit>,
    onAddVisit: () -> Unit,
    onEditPositions: (Visit) -> Unit,
    onDeleteVisit: (Visit) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Дневник посещений",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAddVisit) {
            Icon(Icons.Default.Add, contentDescription = "Добавить посещение")
        }
    }
    if (visits.isEmpty()) {
        Text(
            text = "Нет посещений. Нажмите + чтобы добавить.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        visits.forEach { visit ->
            val dateStr = formatVisitDate(visit.visitDate)
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                onClick = { expanded = !expanded }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onEditPositions(visit) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Выбрать позиции", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDeleteVisit(visit) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Свернуть" else "Развернуть"
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        if (visit.positions.isEmpty()) {
                            Text(
                                text = "Позиции не выбраны",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        } else {
                            visit.positions.forEach { sel ->
                                Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
                                    Text("• ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    val label = if (sel.toothNumbers.isNotBlank()) {
                                        "${sel.position.name} — зуб(ы) ${sel.toothNumbers}"
                                    } else {
                                        sel.position.name
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        FilledTonalButton(
                            onClick = { onEditPositions(visit) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Выбрать позиции", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddVisitDialog(
    initialDateMillis: Long,
    onDateChanged: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var dateText by remember(initialDateMillis) {
        mutableStateOf(formatVisitDate(initialDateMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить посещение") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Дата посещения",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    placeholder = { Text("ДД.ММ.ГГГГ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Дата") }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Будет создано новое посещение. После создания вы сможете выбрать позиции.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun PositionSelectionDialog(
    allPositions: List<Position>,
    selectedPositionIds: Set<Long>,
    positionToothNumbers: Map<Long, String>,
    onTogglePosition: (Long) -> Unit,
    onToothNumbersChanged: (Long, String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Выбор позиций для посещения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Зуб(ы)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(72.dp)
                    )
                    Text(
                        text = "Позиция",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allPositions, key = { it.id }) { position ->
                        val isChecked = position.id in selectedPositionIds

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = positionToothNumbers[position.id] ?: "",
                                onValueChange = { onToothNumbersChanged(position.id, it) },
                                modifier = Modifier.width(72.dp),
                                singleLine = true,
                                enabled = isChecked,
                                placeholder = { Text("№", fontSize = 11.sp) },
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.width(4.dp))
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onTogglePosition(position.id) },
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = position.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сбросить выбор")
                    }
                    Button(
                        onClick = onSave,
                        enabled = selectedPositionIds.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить выбранные позиции")
                    }
                }

                Spacer(Modifier.height(4.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}

private fun formatVisitDate(millis: Long): String {
    if (millis <= 0) return "Нет даты"
    val instant = Instant.fromEpochMilliseconds(millis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val month = local.monthNumber.toString().padStart(2, '0')
    val year = local.year
    return "$day.$month.$year"
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
    val rubles = amount / 100
    val cents = amount % 100
    return rubles.toString().reversed().chunked(3).joinToString(" ").reversed() + "." + (if (cents < 10) "0" else "") + cents
}

private fun _formatPrice(amount: Long): String {
    val rubles = amount / 100
    return rubles.toString().reversed().chunked(3).joinToString(" ").reversed()
}

private fun _formatPriceInput(amount: Long): String {
    val rubles = amount / 100
    return if (rubles == 0L) "" else rubles.toString()
}

private fun _parsePriceInput(text: String): Long {
    val rubles = text.filter { it.isDigit() }.toLongOrNull() ?: return -1
    return rubles * 100
}

@Composable
private fun _PriceItemEditDialog(
    title: String,
    initialName: String,
    initialCategory: String,
    initialPrice: Long,
    showCategoryField: Boolean,
    onSave: (name: String, category: String, price: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(initialCategory) }
    var priceText by remember { mutableStateOf(_formatPriceInput(initialPrice)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Наименование услуги") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                if (showCategoryField) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Категория") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Цена (руб)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("1500") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val price = _parsePriceInput(priceText)
                    if (name.isNotBlank() && category.isNotBlank() && price >= 0) {
                        onSave(name.trim(), category.trim(), price)
                    }
                },
                enabled = name.isNotBlank() && category.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
