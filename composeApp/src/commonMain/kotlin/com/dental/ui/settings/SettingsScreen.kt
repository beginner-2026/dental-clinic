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
import androidx.compose.ui.unit.dp
import com.dental.data.sync.BackupManager
import com.dental.data.sync.BackupStorage
import com.dental.data.sync.SyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    backupManager: BackupManager,
    defaultAddress: String = "http://localhost:9876",
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var syncServerAddress by remember { mutableStateOf(defaultAddress) }
    var statusMessage by remember { mutableStateOf("") }
    var syncStatus by remember { mutableStateOf("") }

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
                            if (ok) showStatus("✅ Бэкап сохранён на устройстве")
                            else showStatus("❌ Ошибка сохранения бэкапа")
                        } catch (e: Exception) {
                            showStatus("❌ ${e.message}")
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
                                showStatus("✅ Бэкап восстановлен")
                            } else {
                                showStatus("❌ Нет сохранённого бэкапа")
                            }
                        } catch (e: Exception) {
                            showStatus("❌ ${e.message}")
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

            Text("Синхронизация по сети", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = syncServerAddress,
                onValueChange = { syncServerAddress = it },
                label = { Text("Адрес сервера (ПК)") },
                placeholder = { Text("http://192.168.1.100:9876") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val client = SyncClient(syncServerAddress.trimEnd('/'))
                                val ok = withContext(Dispatchers.IO) { client.ping() }
                                if (ok) {
                                    showStatus("✅ Сервер доступен")
                                    val remoteData = withContext(Dispatchers.IO) { client.pull() }
                                    backupManager.importFromData(remoteData)
                                    showStatus("✅ Данные получены с сервера")
                                } else {
                                    showStatus("❌ Сервер недоступен")
                                }
                                client.close()
                            } catch (e: Exception) {
                                showStatus("❌ ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Загрузить с сервера (Pull)")
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val client = SyncClient(syncServerAddress.trimEnd('/'))
                                val ok = withContext(Dispatchers.IO) { client.ping() }
                                if (ok) {
                                    val data = backupManager.exportAll()
                                    val result = withContext(Dispatchers.IO) { client.push(data) }
                                    if (result.success) showStatus("✅ Данные отправлены на сервер")
                                    else showStatus("❌ ${result.message}")
                                } else {
                                    showStatus("❌ Сервер недоступен")
                                }
                                client.close()
                            } catch (e: Exception) {
                                showStatus("❌ ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отправить на сервер (Push)")
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusMessage.startsWith("✅")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
