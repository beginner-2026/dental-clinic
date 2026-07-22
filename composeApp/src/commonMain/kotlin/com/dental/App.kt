package com.dental

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
import com.dental.ui.DentalLogoHeader

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

    var currentScreenName by rememberSaveable { mutableStateOf(AppScreen.PATIENTS.name) }
    var currentScreen by remember(currentScreenName) { mutableStateOf(AppScreen.valueOf(currentScreenName)) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var dataGeneration by remember { mutableIntStateOf(0) }
    var patients by remember { mutableStateOf(patientRepo.getAll()) }
    fun reloadPatients() {
        patients = patientRepo.getAll()
        dataGeneration++
    }

    LaunchedEffect(Unit) {
        SeedData.seedIfEmpty(database)
        if (priceListRepo.getAll().none { it.name == "Перебазировка протеза" }) {
            priceListRepo.create("Пользовательские услуги", "Перебазировка протеза", 6000_00)
        }
        if (visitPositionRepo.getAllPositions().none { it.name == "Перебазировка протеза" }) {
            val maxOrder = visitPositionRepo.getAllPositions().maxOfOrNull { it.sortOrder } ?: 0
            visitPositionRepo.createPosition("Перебазировка протеза", maxOrder + 1)
        }
        reloadPatients()
    }

    var selectedPatientId by rememberSaveable { mutableStateOf(-1L) }
    val selectedPatient = patients.find { it.id == selectedPatientId }
    val odontogramViewModel = remember { OdontogramViewModel(toothRepo) }

    var showAddPatientDialog by rememberSaveable { mutableStateOf(false) }
    var newPatientLastName by rememberSaveable { mutableStateOf("") }
    var newPatientFirstName by rememberSaveable { mutableStateOf("") }
    var newPatientMiddleName by rememberSaveable { mutableStateOf("") }
    var newPatientPhone by rememberSaveable { mutableStateOf("") }

    DentalTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.padding(start = 20.dp)
                ) {
                    DentalLogoHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        surfaceColor = MaterialTheme.colorScheme.surface
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Spacer(Modifier.height(8.dp))

                    NavigationDrawerItem(
                        icon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(-4.dp))
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        },
                        label = { Text("Пациенты") },
                        selected = currentScreen == AppScreen.PATIENTS,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreenName = AppScreen.PATIENTS.name
                            currentScreen = AppScreen.PATIENTS
                            reloadPatients()
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                        label = { Text("Прейскурант") },
                        selected = currentScreen == AppScreen.PRICE_LIST,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreenName = AppScreen.PRICE_LIST.name
                            currentScreen = AppScreen.PRICE_LIST
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Настройки") },
                        selected = currentScreen == AppScreen.SETTINGS,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreenName = AppScreen.SETTINGS.name
                            currentScreen = AppScreen.SETTINGS
                        }
                    )
                }
            }
        ) {
            when (currentScreen) {
                AppScreen.SETTINGS -> SettingsScreen(
                    backupManager = backupManager,
                    cloudSyncStorage = cloudSyncStorage,
                    onBack = {
                        dataGeneration++
                        patients = patientRepo.getAll()
                        currentScreenName = AppScreen.PATIENTS.name
                        currentScreen = AppScreen.PATIENTS
                    },
                    onDataReloaded = { reloadPatients() }
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
                            onBack = { selectedPatientId = -1L }
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
                                onPatientClick = { selectedPatientId = it.id }
                            )
                            }
                            AppScreen.PRICE_LIST -> PriceListScreen(priceListRepo)
                            AppScreen.SETTINGS -> { /* handled above */ }
                        }
                    }
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
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPatientFirstName,
                        onValueChange = { newPatientFirstName = it },
                        label = { Text("Имя *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPatientMiddleName,
                        onValueChange = { newPatientMiddleName = it },
                        label = { Text("Отчество") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPatientPhone,
                        onValueChange = { newPatientPhone = it },
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
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
