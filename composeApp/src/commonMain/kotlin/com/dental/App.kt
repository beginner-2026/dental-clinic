package com.dental

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dental.data.AppointmentRepository
import com.dental.data.DatabaseDriverFactory
import com.dental.data.DiagnosisRepository
import com.dental.data.InvoiceRepository
import com.dental.data.PatientRepository
import com.dental.data.SeedData
import com.dental.data.ToothRepository
import com.dental.data.PriceListRepository
import com.dental.data.TreatmentPlanRepository
import com.dental.data.VisitPositionRepository
import com.dental.data.db.DentalDatabase
import com.dental.data.sync.BackupManager
import com.dental.data.sync.CloudSyncStorage
import com.dental.model.*
import com.dental.ui.navigation.AppScreen
import com.dental.ui.odontogram.OdontogramViewModel
import com.dental.ui.patient.PatientDetailScreen
import com.dental.ui.patient.PatientScreen
import com.dental.ui.pricelist.PriceListScreen
import com.dental.ui.settings.SettingsScreen
import com.dental.ui.theme.DentalTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import dentalclinic.composeapp.generated.resources.*

const val APP_VERSION = "1.1.0"

@Composable
fun App(
    driverFactory: DatabaseDriverFactory
) {
    val driver = remember { driverFactory.createDriver() }
    val database = remember { DentalDatabase(driver) }
    val backupManager = remember { BackupManager(database) }
    val cloudSyncStorage = remember { CloudSyncStorage() }
    val appointmentRepo = remember { AppointmentRepository(database) }
    val patientRepo = remember { PatientRepository(database) }
    val toothRepo = remember { ToothRepository(database) }
    val invoiceRepo = remember { InvoiceRepository(database) }
    val diagnosisRepo = remember { DiagnosisRepository(database) }
    val treatmentPlanRepo = remember { TreatmentPlanRepository(database) }
    val visitPositionRepo = remember { VisitPositionRepository(database) }
    val priceListRepo = remember { PriceListRepository(database) }

    var currentScreenStack by rememberSaveable {
        mutableStateOf(listOf(AppScreen.PATIENTS.name))
    }
    val currentScreen = AppScreen.valueOf(currentScreenStack.last())
    var drawerOpen by rememberSaveable { mutableStateOf(false) }

    var dataGeneration by remember { mutableIntStateOf(0) }
    var patients by remember { mutableStateOf(patientRepo.getAll()) }
    fun reloadPatients() {
        patients = patientRepo.getAll()
        dataGeneration++
    }

    fun navigateTo(screen: AppScreen) {
        if (currentScreenStack.last() != screen.name) {
            currentScreenStack = currentScreenStack + screen.name
        }
    }

    fun navigateHome() {
        currentScreenStack = listOf(AppScreen.PATIENTS.name)
        reloadPatients()
    }

    fun navigateBack() {
        if (currentScreenStack.size > 1) {
            currentScreenStack = currentScreenStack.dropLast(1)
        }
    }

    LaunchedEffect(Unit) {
        SeedData.seedIfEmpty(database)
        if (priceListRepo.getAll().none { it.name == "Перебазировка протеза" }) {
            priceListRepo.create("Пользовательские услуги", "Перебазировка протеза", 6000_00)
        }
        visitPositionRepo.ensurePosition("Перебазировка протеза")
        visitPositionRepo.ensurePosition("Оплата")
        reloadPatients()
    }

    var selectedPatientId by rememberSaveable { mutableStateOf(-1L) }
    val selectedPatient = patients.find { it.id == selectedPatientId }
    val odontogramViewModel = remember { OdontogramViewModel(toothRepo) }

    val canGoBackBySystem = selectedPatientId == -1L && currentScreenStack.size > 1
    PlatformBackHandler(enabled = drawerOpen || canGoBackBySystem) {
        if (drawerOpen) {
            drawerOpen = false
        } else {
            navigateBack()
        }
    }

    var showAddPatientDialog by rememberSaveable { mutableStateOf(false) }
    var newPatientLastName by rememberSaveable { mutableStateOf("") }
    var newPatientFirstName by rememberSaveable { mutableStateOf("") }
    var newPatientMiddleName by rememberSaveable { mutableStateOf("") }
    var newPatientPhone by rememberSaveable { mutableStateOf("") }

    DentalTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .swipeFromRightEdgeToOpen { drawerOpen = true }
            ) {
                when (currentScreen) {
                    AppScreen.SETTINGS -> SettingsScreen(
                        backupManager = backupManager,
                        cloudSyncStorage = cloudSyncStorage,
                        onBack = {
                            navigateHome()
                        },
                        onDataReloaded = { reloadPatients() },
                        onMenuClick = { drawerOpen = true }
                    )
                    else -> {
                        val currentPatient = selectedPatient
                        if (currentPatient != null) {
                            PatientDetailScreen(
                                patient = currentPatient,
                                odontogramViewModel = odontogramViewModel,
                                invoiceRepository = invoiceRepo,
                                diagnosisRepository = diagnosisRepo,
                                treatmentPlanRepository = treatmentPlanRepo,
                                visitPositionRepository = visitPositionRepo,
                                priceListRepository = priceListRepo,
                                onBack = { selectedPatientId = -1L },
                                onMenuClick = { drawerOpen = true }
                            )
                        } else {
                            when (currentScreen) {
                                AppScreen.PATIENTS -> key(dataGeneration) {
                                    PatientScreen(
                                        patients = patients,
                                        onAddPatient = { showAddPatientDialog = true },
                                        onDeletePatient = { id ->
                                            patientRepo.delete(id)
                                            reloadPatients()
                                        },
                                        onReload = { reloadPatients() },
                                        onPatientClick = { selectedPatientId = it.id },
                                        onMenuClick = { drawerOpen = true }
                                    )
                                }
                                AppScreen.PRICE_LIST -> PriceListScreen(
                                    priceListRepo,
                                    onMenuClick = { drawerOpen = true }
                                )
                                AppScreen.SETTINGS -> { /* handled above */ }
                            }
                        }
                    }
                }
            }

            if (drawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { drawerOpen = false }
                )
            }

            AnimatedVisibility(
                visible = drawerOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .swipeRightToClose { drawerOpen = false }
                ) {
                    Image(
                        painter = painterResource(Res.drawable.background),
                        contentDescription = "Логотип",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp)
                            .height(100.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Версия $APP_VERSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Spacer(Modifier.height(8.dp))

                    NavigationDrawerItem(
                        icon = {
                            Image(
                                painter = painterResource(Res.drawable.patients),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                contentScale = ContentScale.Fit
                            )
                        },
                        label = { Text("Пациенты") },
                        selected = currentScreen == AppScreen.PATIENTS,
                        onClick = {
                            drawerOpen = false
                            navigateHome()
                        }
                    )
                    NavigationDrawerItem(
                        icon = {
                            Image(
                                painter = painterResource(Res.drawable.price),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                contentScale = ContentScale.Fit
                            )
                        },
                        label = { Text("Прейскурант") },
                        selected = currentScreen == AppScreen.PRICE_LIST,
                        onClick = {
                            drawerOpen = false
                            navigateTo(AppScreen.PRICE_LIST)
                        }
                    )
                    NavigationDrawerItem(
                        icon = {
                            Image(
                                painter = painterResource(Res.drawable.settings),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                contentScale = ContentScale.Fit
                            )
                        },
                        label = { Text("Настройки") },
                        selected = currentScreen == AppScreen.SETTINGS,
                        onClick = {
                            drawerOpen = false
                            navigateTo(AppScreen.SETTINGS)
                        }
                    )
                }
            }
        }
    }

    if (showAddPatientDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddPatientDialog = false
                newPatientLastName = ""
                newPatientFirstName = ""
                newPatientMiddleName = ""
                newPatientPhone = ""
            },
            title = { Text("Добавить пациента") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newPatientLastName,
                        onValueChange = { newPatientLastName = it },
                        label = { Text("Фамилия *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPatientFirstName,
                        onValueChange = { newPatientFirstName = it },
                        label = { Text("Имя *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPatientMiddleName,
                        onValueChange = { newPatientMiddleName = it },
                        label = { Text("Отчество") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPatientPhone,
                        onValueChange = { newPatientPhone = it },
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPatientLastName.isNotBlank() && newPatientFirstName.isNotBlank()) {
                            patientRepo.create(
                                Patient(
                                    lastName = newPatientLastName.trim(),
                                    firstName = newPatientFirstName.trim(),
                                    middleName = newPatientMiddleName.trim().ifBlank { null },
                                    phone = newPatientPhone.trim().ifBlank { null }
                                )
                            )
                            newPatientLastName = ""
                            newPatientFirstName = ""
                            newPatientMiddleName = ""
                            newPatientPhone = ""
                            reloadPatients()
                            showAddPatientDialog = false
                        }
                    },
                    enabled = newPatientLastName.isNotBlank() && newPatientFirstName.isNotBlank()
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddPatientDialog = false
                    newPatientLastName = ""
                    newPatientFirstName = ""
                    newPatientMiddleName = ""
                    newPatientPhone = ""
                }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun Modifier.swipeFromRightEdgeToOpen(onOpen: () -> Unit): Modifier {
    val edgeWidthPx = with(LocalDensity.current) { 32.dp.toPx() }
    return this.pointerInput(Unit) {
        val edgeWidth = edgeWidthPx
        var fromRightEdge = false
        var accumulated = 0f
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                fromRightEdge = offset.x >= size.width - edgeWidth
                accumulated = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                if (fromRightEdge) {
                    change.consume()
                    accumulated += dragAmount
                }
            },
            onDragEnd = {
                if (fromRightEdge && accumulated < -80f) {
                    onOpen()
                }
                fromRightEdge = false
            },
            onDragCancel = { fromRightEdge = false }
        )
    }
}

private fun Modifier.swipeRightToClose(onClose: () -> Unit): Modifier {
    return this.pointerInput(Unit) {
        var accumulated = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                accumulated += dragAmount
            },
            onDragEnd = {
                if (accumulated > 80f) {
                    onClose()
                }
                accumulated = 0f
            },
            onDragCancel = { accumulated = 0f }
        )
    }
}
