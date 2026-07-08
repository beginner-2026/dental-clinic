package com.dental.ui.settings

import androidx.compose.foundation.layout.*
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
import com.dental.data.sync.CloudSyncStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    backupManager: BackupManager,
    cloudSyncStorage: CloudSyncStorage,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("") }
    var syncStatus by remember { mutableStateOf(cloudSyncStorage.statusText()) }

    val pickFile = rememberSyncFilePickerLauncher { path ->
        cloudSyncStorage.configure(path)
        syncStatus = cloudSyncStorage.statusText()
    }

    fun showStatus(msg: String) {
        statusMessage = msg
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Настройки и синхронизация") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Резервное копирование", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val json = backupManager.exportToJson()
                            val ok = withContext(Dispatchers.IO) { BackupStorage.save(json) }
                            if (ok) showStatus("Бэкап сохранён на устройстве")
                            else showStatus("Ошибка сохранения бэкапа")
                        } catch (e: Exception) {
                            showStatus("${e.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Создать резервную копию (экспорт)")
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val json = withContext(Dispatchers.IO) { BackupStorage.load() }
                            if (json != null) {
                                backupManager.importFromJson(json)
                                showStatus("Бэкап восстановлен")
                            } else {
                                showStatus("Нет сохранённого бэкапа")
                            }
                        } catch (e: Exception) {
                            showStatus("${e.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Восстановить из резервной копии (импорт)")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Синхронизация с облаком", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = { pickFile() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Выбрать файл синхронизации")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val json = backupManager.exportToJson()
                                val ok = withContext(Dispatchers.IO) { cloudSyncStorage.save(json) }
                                if (ok) showStatus("Данные отправлены в облако")
                                else showStatus("Ошибка отправки в облако")
                            } catch (e: Exception) {
                                showStatus("${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = cloudSyncStorage.isConfigured
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Отправить в облако")
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val json = withContext(Dispatchers.IO) { cloudSyncStorage.load() }
                                if (json != null) {
                                    backupManager.importFromJson(json)
                                    showStatus("Данные загружены из облака")
                                } else {
                                    showStatus("В облаке нет данных")
                                }
                            } catch (e: Exception) {
                                showStatus("${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = cloudSyncStorage.isConfigured
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Загрузить из облака")
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
