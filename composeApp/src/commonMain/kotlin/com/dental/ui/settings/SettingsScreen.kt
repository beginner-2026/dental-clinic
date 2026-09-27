package com.dental.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dental.data.sync.BackupManager
import com.dental.data.sync.BackupStorage
import com.dental.data.sync.CloudSyncResult
import com.dental.data.sync.CloudSyncStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PendingRestore(
    val json: String,
    val source: RestoreSource,
    val patientCount: Int,
    val deviceName: String?
)

private enum class RestoreSource {
    LOCAL,
    CLOUD
}

@Composable
fun SettingsScreen(
    backupManager: BackupManager,
    cloudSyncStorage: CloudSyncStorage,
    onBack: () -> Unit,
    onDataReloaded: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onBusyChanged: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("") }
    var syncStatus by remember { mutableStateOf("Проверка облачной синхронизации…") }
    var isBusy by remember { mutableStateOf(false) }
    var isCloudConfigured by remember { mutableStateOf(false) }
    var canPushToCloud by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<PendingRestore?>(null) }
    var pendingPush by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isBusy) {
        onBusyChanged(isBusy)
    }

    DisposableEffect(Unit) {
        onDispose { onBusyChanged(false) }
    }

    fun showStatus(message: String) {
        statusMessage = message
    }

    suspend fun refreshSyncStatus() {
        val state = withContext(Dispatchers.IO) {
            Triple(cloudSyncStorage.statusText(), cloudSyncStorage.isConfigured, cloudSyncStorage.canPush)
        }
        syncStatus = state.first
        isCloudConfigured = state.second
        canPushToCloud = state.third
    }

    fun failureMessage(error: Throwable, prefix: String): String {
        return "$prefix: ${error.message ?: "неизвестная ошибка"}"
    }

    LaunchedEffect(cloudSyncStorage) {
        isBusy = true
        try {
            withContext(Dispatchers.IO) { cloudSyncStorage.initialize() }
            refreshSyncStatus()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            showStatus(failureMessage(error, "Облачная синхронизация не инициализирована"))
        } finally {
            isBusy = false
        }
    }

    val pickFile = rememberSyncFilePickerLauncher(
        onSelected = { path ->
            scope.launch {
                isBusy = true
                try {
                    when (val result = withContext(Dispatchers.IO) { cloudSyncStorage.configure(path) }) {
                        is CloudSyncResult.Success -> {
                            refreshSyncStatus()
                            showStatus(
                                if (canPushToCloud) "Общий файл sync.json подключён"
                                else "Общий файл sync.json подключён. Сначала загрузите облачные данные"
                            )
                        }
                        is CloudSyncResult.Failure -> {
                            refreshSyncStatus()
                            showStatus(result.message)
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    showStatus(failureMessage(error, "Общий файл не подключён"))
                } finally {
                    isBusy = false
                }
            }
        },
        onError = { message ->
            showStatus(message)
        }
    )

    suspend fun createLocalBackup() {
        withContext(Dispatchers.IO) {
            val json = backupManager.exportToJson()
            if (!BackupStorage.save(json)) {
                throw IllegalStateException("Не удалось создать резервную копию перед восстановлением")
            }
        }
    }

    fun applyRestore(restore: PendingRestore) {
        pendingRestore = null
        scope.launch {
            isBusy = true
            try {
                if (restore.source == RestoreSource.CLOUD) {
                    when (val verification = withContext(Dispatchers.IO) { cloudSyncStorage.verify(restore.json) }) {
                        is CloudSyncResult.Success -> Unit
                        is CloudSyncResult.Failure -> {
                            showStatus(verification.message)
                            isBusy = false
                            return@launch
                        }
                    }
                }
                createLocalBackup()
                withContext(Dispatchers.IO) { backupManager.importFromJson(restore.json) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showStatus(failureMessage(error, "Восстановление отменено"))
                isBusy = false
                return@launch
            }

            val reloadFailed = runCatching { onDataReloaded() }.exceptionOrNull()
            if (restore.source == RestoreSource.CLOUD && isCloudConfigured) {
                val pushResult = runCatching {
                    when (val accepted = withContext(Dispatchers.IO) { cloudSyncStorage.accept(restore.json) }) {
                        is CloudSyncResult.Success -> {
                            val pushJson = withContext(Dispatchers.IO) { backupManager.exportToJson() }
                            withContext(Dispatchers.IO) { cloudSyncStorage.save(pushJson) }
                        }
                        is CloudSyncResult.Failure -> accepted
                    }
                }.getOrElse { error ->
                    if (error is CancellationException) throw error
                    CloudSyncResult.Failure(error.message ?: "ошибка экспорта")
                }
                showStatus(
                    when (pushResult) {
                        is CloudSyncResult.Success -> "Данные восстановлены и отправлены в Google Диск"
                        is CloudSyncResult.Failure -> "Данные восстановлены, но отправка не выполнена: ${pushResult.message}"
                    }
                )
            } else {
                showStatus("Восстановлено ${restore.patientCount} пациентов")
            }
            if (reloadFailed != null) {
                showStatus("Данные восстановлены, но интерфейс не обновился: ${reloadFailed.message ?: "неизвестная ошибка"}")
            }
            try {
                refreshSyncStatus()
            } finally {
                isBusy = false
            }
        }
    }

    fun applyPush() {
        val content = pendingPush ?: return
        pendingPush = null
        scope.launch {
            isBusy = true
            try {
                when (val result = withContext(Dispatchers.IO) { cloudSyncStorage.save(content) }) {
                    is CloudSyncResult.Success -> {
                        refreshSyncStatus()
                        showStatus("Файл sync.json обновлён")
                    }
                    is CloudSyncResult.Failure -> {
                        refreshSyncStatus()
                        showStatus(result.message)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showStatus(failureMessage(error, "Отправка не выполнена"))
            } finally {
                isBusy = false
            }
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Настройки и синхронизация") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isBusy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onMenuClick, enabled = !isBusy) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text("Обмен данными", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Text("Локальная копия", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = {
                    scope.launch {
                        isBusy = true
                        try {
                            val json = withContext(Dispatchers.IO) { backupManager.exportToJson() }
                            val ok = withContext(Dispatchers.IO) { BackupStorage.save(json) }
                            showStatus(if (ok) "Резервная копия сохранена на устройстве" else "Ошибка сохранения резервной копии")
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            showStatus(error.message ?: "Ошибка сохранения резервной копии")
                        } finally {
                            isBusy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Создать резервную копию (экспорт)")
            }

            Button(
                onClick = {
                    scope.launch {
                        isBusy = true
                        try {
                            val json = withContext(Dispatchers.IO) { BackupStorage.load() }
                            if (json == null) {
                                showStatus("Нет сохранённой резервной копии")
                            } else {
                                val data = withContext(Dispatchers.IO) { backupManager.parseMetadata(json) }
                                pendingRestore = PendingRestore(json, RestoreSource.LOCAL, data.patients.size, data.syncedByDevice)
                                showStatus("Проверьте данные резервной копии перед восстановлением")
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            showStatus(failureMessage(error, "Резервная копия не восстановлена"))
                        } finally {
                            isBusy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Восстановить из резервной копии (импорт)")
            }

            Text("Синхронизация с Google Диском", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(
                "На компьютере сначала выберите папку Google Диск и отправьте данные. На Android выберите созданный файл DentalClinic/sync.json и сначала загрузите облачные данные. После подтверждения они автоматически отправляются обратно.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = syncStatus,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pickFile() },
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Подключить sync.json")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isBusy = true
                            try {
                                when (val result = withContext(Dispatchers.IO) { cloudSyncStorage.openFolder() }) {
                                    is CloudSyncResult.Success -> Unit
                                    is CloudSyncResult.Failure -> showStatus(result.message)
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                showStatus(failureMessage(error, "Google Диск не открыт"))
                            } finally {
                                isBusy = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy && isCloudConfigured
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Открыть")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isBusy = true
                            try {
                                pendingPush = withContext(Dispatchers.IO) { backupManager.exportToJson() }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                showStatus(failureMessage(error, "Отправка не подготовлена"))
                            } finally {
                                isBusy = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy && canPushToCloud
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Отправить")
                }

                Button(
                    onClick = {
                        scope.launch {
                            isBusy = true
                            try {
                                when (val result = withContext(Dispatchers.IO) { cloudSyncStorage.load() }) {
                                    is CloudSyncResult.Success -> {
                                        val snapshot = result.value
                                        pendingRestore = PendingRestore(snapshot.json, RestoreSource.CLOUD, snapshot.patientCount, snapshot.deviceName)
                                        refreshSyncStatus()
                                        showStatus("Облачные данные найдены. Подтвердите замену локальной базы")
                                    }
                                    is CloudSyncResult.Failure -> {
                                        refreshSyncStatus()
                                        showStatus(result.message)
                                    }
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                showStatus(failureMessage(error, "Облачные данные не загружены"))
                            } finally {
                                isBusy = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy && isCloudConfigured
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Загрузить")
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusMessage.contains("ошиб", ignoreCase = true) ||
                        statusMessage.contains("не удалось", ignoreCase = true) ||
                        statusMessage.contains("отменено", ignoreCase = true)
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Text(
                "Перед восстановлением текущая база автоматически сохраняется отдельной резервной копией. Не работайте одновременно на ПК и Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    pendingPush?.let {
        AlertDialog(
            onDismissRequest = { pendingPush = null },
            title = { Text("Перезаписать файл в Google Диске?") },
            text = { Text("Облачный sync.json будет полностью заменён текущей локальной базой. Перед отправкой убедитесь, что на другом устройстве нет несохранённых изменений.") },
            confirmButton = {
                Button(onClick = { applyPush() }) {
                    Text("Перезаписать")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPush = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    pendingRestore?.let { restore ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Заменить локальные данные?") },
            text = {
                Text(
                    if (restore.source == RestoreSource.CLOUD) {
                        "В Google Диске найдено ${restore.patientCount} пациентов" +
                            (restore.deviceName?.let { " (с устройства: $it)" } ?: "") +
                            ". Текущие данные сначала будут сохранены в резервную копию, затем заменены и отправлены обратно в Google Диск."
                    } else {
                        "В локальной резервной копии найдено ${restore.patientCount} пациентов" +
                            (restore.deviceName?.let { " (с устройства: $it)" } ?: "") +
                            ". Текущие данные сначала будут сохранены, затем заменены."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = { applyRestore(restore) }
                ) {
                    Text("Заменить")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
