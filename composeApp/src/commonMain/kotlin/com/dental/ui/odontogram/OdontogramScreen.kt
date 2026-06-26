package com.dental.ui.odontogram

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dental.model.*

private val NavBlue = Color(0xFF0D47A1)
private val NavBlueLight = Color(0xFF1565C0)
private val TabActive = Color(0xFF1565C0)
private val TabInactive = Color(0xFF9E9E9E)
private val ContentBg = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdontogramScreen(
    viewModel: OdontogramViewModel,
    onMenuClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showLegend by remember { mutableStateOf(false) }
    var activeTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Обзор", "Быстрый выбор", "Пародонтальное зондирование")

    Row(modifier = Modifier.fillMaxSize().background(ContentBg)) {
        // Left Nav Panel
        LeftNavPanel(
            activeTab = activeTabIndex,
            onTabChange = { activeTabIndex = it },
            modifier = Modifier.width(56.dp).fillMaxHeight()
        )

        // Main content
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Top bar with patient name
            Surface(
                tonalElevation = 0.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню", tint = Color(0xFF212121))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val name = state.currentPatientName.ifBlank { "Выберите пациента" }
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF212121))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showLegend = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Легенда", tint = Color(0xFF616161))
                        }
                        IconButton(onClick = { viewModel.toggleReadOnly() }) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = if (state.readOnly) "Только чтение" else "Режим редактирования",
                                tint = if (state.readOnly) Color(0xFFBDBDBD) else Color(0xFF1565C0)
                            )
                        }
                    }
                }
            }

            // Tab row
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    val isActive = activeTabIndex == index
                    TextButton(
                        onClick = { activeTabIndex = index },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isActive) TabActive else TabInactive
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) TabActive else TabInactive
                        )
                        if (isActive) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(4.dp).height(4.dp)
                                    .background(TabActive, CircleShape)
                            )
                        }
                    }
                }
            }

            // View mode + Bridge controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    FilterChip(
                        selected = state.viewMode == OdontogramViewMode.FULL_JAW,
                        onClick = { viewModel.setViewMode(OdontogramViewMode.FULL_JAW) },
                        label = { Text("Полный", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    Spacer(Modifier.width(4.dp))
                    FilterChip(
                        selected = state.viewMode == OdontogramViewMode.QUADRANT,
                        onClick = { viewModel.setViewMode(OdontogramViewMode.QUADRANT) },
                        label = { Text("Квадрант", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                // Bridge mode toggle
                if (state.bridgeMode) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Мост: выберите опоры", fontSize = 10.sp)
                            Spacer(Modifier.width(4.dp))
                            TextButton(onClick = { viewModel.toggleBridgeMode() }, contentPadding = PaddingValues(4.dp)) {
                                Text("Отмена", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // 4-Layer Odontogram Canvas (main content)
            Box(modifier = Modifier.weight(1f)) {
                if (state.currentPatientId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFFBDBDBD))
                            Spacer(Modifier.height(8.dp))
                            Text("Выберите пациента из списка", color = Color(0xFF9E9E9E), fontSize = 14.sp)
                        }
                    }
                } else if (state.viewMode == OdontogramViewMode.FULL_JAW) {
                    OdontogramLayers(
                        teeth = state.teeth,
                        prostheticItems = state.prostheticItems,
                        selectedTooth = state.selectedTooth,
                        selectedToothPart = state.selectedToothPart,
                        activeLayer = state.activeLayer,
                        onToothClick = { number, part -> viewModel.selectTooth(number, part) },
                        crownSelections = state.crownSelections,
                        rootSelections = state.rootSelections
                    )
                } else {
                    QuadrantView(
                        quadrant = state.currentQuadrant,
                        teeth = state.teeth,
                        prostheticItems = state.prostheticItems,
                        activeLayer = state.activeLayer,
                        selectedTooth = state.selectedTooth,
                        selectedToothPart = state.selectedToothPart,
                        bridgeMode = state.bridgeMode,
                        bridgeFirstTooth = state.bridgeFirstTooth,
                        bridgeSecondTooth = state.bridgeSecondTooth,
                        onToothClick = { number, part -> viewModel.selectTooth(number, part) },
                        crownSelections = state.crownSelections,
                        rootSelections = state.rootSelections
                    )
                }
            }

            // Quadrant navigation
            if (state.viewMode == OdontogramViewMode.QUADRANT) {
                QuadrantNavBar(
                    currentQuadrant = state.currentQuadrant,
                    onSelect = { viewModel.setQuadrant(it) },
                    onSwipeLeft = {
                        val next = if (state.currentQuadrant < 4) state.currentQuadrant + 1 else 1
                        viewModel.setQuadrant(next)
                    },
                    onSwipeRight = {
                        val prev = if (state.currentQuadrant > 1) state.currentQuadrant - 1 else 4
                        viewModel.setQuadrant(prev)
                    }
                )
            }

            // Undo FAB is now part of bottom area
            if (state.undoStack.isNotEmpty() && !state.readOnly) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    SmallFloatingActionButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Отменить")
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
            onDismiss = { viewModel.dismissPartMenu() },
            onApplyCrown = { option -> viewModel.applyCrownOption(state.selectedTooth!!, option) },
            onApplyRoot = { option -> viewModel.applyRootOption(state.selectedTooth!!, option) }
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
            onDismiss = { viewModel.dismissMenu() },
            onSelectProsthetic = { type, material, stage ->
                viewModel.applyProsthetic(type, material, stage)
            },
            onToggleBridgeMode = { viewModel.toggleBridgeMode() },
            onChangeToothStatus = { status -> viewModel.setToothStatus(state.selectedTooth!!, status) }
        )
    }

    // Legend dialog
    if (showLegend) {
        LegendDialog(onDismiss = { showLegend = false })
    }
}

@Composable
private fun LeftNavPanel(
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(NavBlue)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Nav items
        NavItem(
            icon = { ToothNavIcon() },
            label = "Эндо",
            isActive = activeTab == 0,
            onClick = { onTabChange(0) }
        )
        Spacer(Modifier.height(24.dp))
        NavItem(
            icon = { GumNavIcon() },
            label = "Перио",
            isActive = activeTab == 1,
            onClick = { onTabChange(1) }
        )
        Spacer(Modifier.height(24.dp))
        NavItem(
            icon = { ToothOutlineNavIcon() },
            label = "Вид",
            isActive = activeTab == 2,
            onClick = { onTabChange(2) }
        )
    }
}

@Composable
private fun NavItem(
    icon: @Composable () -> Unit,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) Color.White.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (isActive) Color.White else Color.White.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .width(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ToothNavIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val path = Path().apply {
            moveTo(cx - 6f, cy + 4f)
            lineTo(cx - 6f, cy - 4f)
            quadraticTo(cx - 4f, cy - 8f, cx, cy - 6f)
            quadraticTo(cx + 4f, cy - 8f, cx + 6f, cy - 4f)
            lineTo(cx + 6f, cy + 4f)
            close()
        }
        drawPath(path, Color.White, style = Stroke(width = 1.5f))
        drawLine(Color.White, Offset(cx - 3f, cy + 4f), Offset(cx - 3f, cy + 8f), strokeWidth = 1.5f)
        drawLine(Color.White, Offset(cx + 3f, cy + 4f), Offset(cx + 3f, cy + 8f), strokeWidth = 1.5f)
    }
}

@Composable
private fun GumNavIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val path = Path().apply {
            moveTo(cx - 8f, cy + 2f)
            cubicTo(cx - 8f, cy - 5f, cx + 8f, cy - 5f, cx + 8f, cy + 2f)
            lineTo(cx + 8f, cy + 6f)
            lineTo(cx - 8f, cy + 6f)
            close()
        }
        drawPath(path, Color.White, style = Stroke(width = 1.5f))
    }
}

@Composable
private fun ToothOutlineNavIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val path = Path().apply {
            moveTo(cx - 7f, cy + 3f)
            lineTo(cx - 7f, cy - 4f)
            quadraticTo(cx - 4f, cy - 8f, cx, cy - 7f)
            quadraticTo(cx + 4f, cy - 8f, cx + 7f, cy - 4f)
            lineTo(cx + 7f, cy + 3f)
            close()
        }
        drawPath(path, Color.White, style = Stroke(width = 1.5f))
    }
}

@Composable
private fun QuadrantView(
    quadrant: Int,
    teeth: List<Tooth>,
    prostheticItems: List<ProstheticItem>,
    activeLayer: OdontogramLayer,
    selectedTooth: Int?,
    selectedToothPart: ToothPart? = null,
    bridgeMode: Boolean,
    bridgeFirstTooth: Int?,
    bridgeSecondTooth: Int?,
    onToothClick: (Int, ToothPart?) -> Unit,
    crownSelections: Map<Int, CrownOption> = emptyMap(),
    rootSelections: Map<Int, RootOption> = emptyMap()
) {
    val toothNumbers = OdontogramViewModel.getQuadrantToothNumbers(quadrant)
    val jawLabel = if (quadrant <= 2) "Верхняя" else "Нижняя"

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Квадрант $quadrant ($jawLabel)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        val layerToothNumbers = toothNumbers
        OdontogramLayers(
            teeth = teeth,
            prostheticItems = prostheticItems,
            selectedTooth = selectedTooth,
            selectedToothPart = selectedToothPart,
            activeLayer = activeLayer,
            onToothClick = onToothClick,
            upperTeeth = if (quadrant <= 2) layerToothNumbers else emptyList(),
            lowerTeeth = if (quadrant >= 3) layerToothNumbers else emptyList(),
            crownSelections = crownSelections,
            rootSelections = rootSelections
        )

        Spacer(Modifier.height(16.dp))
        if (!bridgeMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onToothClick(toothNumbers.first(), null) },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Процедура", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("История", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun QuadrantNavBar(
    currentQuadrant: Int,
    onSelect: (Int) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSwipeRight) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Назад")
            }
            (1..4).forEach { q ->
                val label = when (q) { 1 -> "К1"; 2 -> "К2"; 3 -> "К3"; else -> "К4" }
                FilterChip(
                    selected = currentQuadrant == q,
                    onClick = { onSelect(q) },
                    label = { Text(label) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            IconButton(onClick = onSwipeLeft) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Далее")
            }
        }
    }
}

@Composable
private fun LegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Легенда") },
        text = {
            Column {
                Text("Цветовая схема:", fontWeight = FontWeight.Bold)
                LegendRow(Color(0xFFFFFFFF), "Здоровый зуб")
                LegendRow(Color(0xFF9C27B0), "Пломба / Кариес / Проблема с десной")
                LegendRow(Color(0xFF607D8B), "Коронка / Металлокерамика / Имплант")
                LegendRow(Color(0xFFFFEB3B), "Эндодонтическое лечение")
                LegendRow(Color(0xFFE53935), "Линия десны / Карман")
                LegendRow(Color(0xFFD7CCC8), "Костная ткань")
                LegendRow(Color(0xFF757575), "Имплант (титан)")
                Spacer(Modifier.height(8.dp))
                Text("Слои:", fontWeight = FontWeight.Bold)
                Text("- Анатомический: поперечное сечение с корнями и костью")
                Text("- Коронки: вид сверху на тип реставрации")
                Text("- Реставрации: овальная карта пломб/проблем")
                Text("- Контур: схематичный контур / абатменты импланта")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
                .border(1.dp, Color.Gray)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp)
    }
}
