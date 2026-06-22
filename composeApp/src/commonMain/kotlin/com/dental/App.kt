package com.dental

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dental.data.AppointmentRepository
import com.dental.data.DatabaseDriverFactory
import com.dental.data.InvoiceRepository
import com.dental.data.PatientRepository
import com.dental.data.SeedData
import com.dental.data.ToothRepository
import com.dental.data.db.DentalDatabase
import com.dental.model.*
import com.dental.ui.calendar.CalendarScreen
import com.dental.ui.calendar.CalendarViewModel
import com.dental.ui.invoice.InvoiceScreen
import com.dental.ui.navigation.AppScreen
import com.dental.ui.odontogram.OdontogramScreen
import com.dental.ui.odontogram.OdontogramViewModel
import com.dental.ui.patient.PatientDetailScreen
import com.dental.ui.patient.PatientScreen
import com.dental.ui.theme.DentalTheme

@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    val driver = remember { driverFactory.createDriver() }
    val database = remember { DentalDatabase(driver) }
    LaunchedEffect(Unit) { SeedData.seedIfEmpty(database) }
    val appointmentRepo = remember { AppointmentRepository(database) }
    val patientRepo = remember { PatientRepository(database) }
    val toothRepo = remember { ToothRepository(database) }
    val invoiceRepo = remember { InvoiceRepository(database) }
    val calendarViewModel = remember { CalendarViewModel(appointmentRepo) }
    val odontogramViewModel = remember { OdontogramViewModel(toothRepo) }

    var currentScreen by remember { mutableStateOf(AppScreen.CALENDAR) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var reloadKey by remember { mutableStateOf(0) }
    val patients = remember(reloadKey) { patientRepo.getAll() }
    var drawerOpen by remember { mutableStateOf(false) }

    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var showAddPatientDialog by remember { mutableStateOf(false) }
    var newPatientLastName by remember { mutableStateOf("") }
    var newPatientFirstName by remember { mutableStateOf("") }
    var newPatientMiddleName by remember { mutableStateOf("") }
    var newPatientPhone by remember { mutableStateOf("") }

    LaunchedEffect(drawerOpen) {
        if (drawerOpen) drawerState.open() else drawerState.close()
    }

    DentalTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
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
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("Календарь") },
                        selected = currentScreen == AppScreen.CALENDAR,
                        onClick = {
                            currentScreen = AppScreen.CALENDAR
                            calendarViewModel.loadAppointments()
                            drawerOpen = false
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Пациенты") },
                        selected = currentScreen == AppScreen.PATIENTS,
                        onClick = {
                            currentScreen = AppScreen.PATIENTS
                            drawerOpen = false
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Зубная формула") },
                        selected = currentScreen == AppScreen.ODONTOGRAM,
                        onClick = {
                            currentScreen = AppScreen.ODONTOGRAM
                            drawerOpen = false
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Счёт") },
                        selected = currentScreen == AppScreen.INVOICE,
                        onClick = {
                            currentScreen = AppScreen.INVOICE
                            drawerOpen = false
                        }
                    )
                }
            }
        ) {
                when (currentScreen) {
                    AppScreen.CALENDAR -> CalendarScreen(
                        viewModel = calendarViewModel,
                        patients = patients,
                        onMenuClick = { drawerOpen = !drawerOpen }
                    )
                    AppScreen.PATIENTS -> PatientScreen(
                        patients = patients,
                        onPatientClick = { patient ->
                            selectedPatient = patient
                            odontogramViewModel.setTeeth(emptyList())
                            currentScreen = AppScreen.PATIENT_DETAIL
                        },
                        onAddPatient = { showAddPatientDialog = true },
                        onMenuClick = { drawerOpen = !drawerOpen }
                    )
                    AppScreen.PATIENT_DETAIL -> selectedPatient?.let { patient ->
                        PatientDetailScreen(
                            patient = patient,
                            odontogramViewModel = odontogramViewModel,
                            invoiceRepository = invoiceRepo,
                            onBack = {
                                currentScreen = AppScreen.PATIENTS
                                odontogramViewModel.clearPatientData()
                                reloadKey++
                            }
                        )
                    }
                    AppScreen.ODONTOGRAM -> OdontogramScreen(
                        viewModel = odontogramViewModel,
                        onMenuClick = { drawerOpen = !drawerOpen }
                    )
                    AppScreen.INVOICE -> InvoiceScreen(
                        invoiceParam = null,
                        patients = patients,
                        onSave = {},
    
                        onMenuClick = { drawerOpen = !drawerOpen }
                    )
                }
        }
    }

    // Add Patient Dialog
    if (showAddPatientDialog) {
        AlertDialog(
            onDismissRequest = { showAddPatientDialog = false },
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
                            reloadKey++
                            showAddPatientDialog = false
                        }
                    },
                    enabled = newPatientLastName.isNotBlank() && newPatientFirstName.isNotBlank()
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPatientDialog = false }) { Text("Отмена") }
            }
        )
    }
}
