package com.dental

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    var currentScreen by remember { mutableStateOf(AppScreen.PATIENTS) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var reloadKey by remember { mutableStateOf(0) }
    fun reloadPatients() { reloadKey++ }
    val patients = remember(reloadKey) { patientRepo.getAll() }

    LaunchedEffect(Unit) {
        SeedData.seedIfEmpty(database)
        reloadPatients()
    }

    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    val odontogramViewModel = remember { OdontogramViewModel(toothRepo) }

    var showAddPatientDialog by remember { mutableStateOf(false) }
    var newPatientLastName by remember { mutableStateOf("") }
    var newPatientFirstName by remember { mutableStateOf("") }
    var newPatientMiddleName by remember { mutableStateOf("") }
    var newPatientPhone by remember { mutableStateOf("") }

    DentalTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Стоматологическая клиника",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()

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
                            currentScreen = AppScreen.PATIENTS
                            reloadPatients()
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) },
                        label = { Text("Прейскурант") },
                        selected = currentScreen == AppScreen.PRICE_LIST,
                        onClick = {
                            currentScreen = AppScreen.PRICE_LIST
                        }
                    )
                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Настройки") },
                        selected = currentScreen == AppScreen.SETTINGS,
                        onClick = {
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
                    onBack = { currentScreen = AppScreen.PATIENTS }
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
                            onBack = { selectedPatient = null }
                        )
                    } else {
                        when (currentScreen) {
                            AppScreen.PATIENTS -> PatientScreen(
                                patients = patients,
                                onAddPatient = { showAddPatientDialog = true },
                                onDeletePatient = { id ->
                                    patientRepo.delete(id)
                                    reloadPatients()
                                },
                                onReload = { reloadPatients() },
                                onPatientClick = { selectedPatient = it }
                            )
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
