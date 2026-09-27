package com.dental.data.sync

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.Locale

actual class CloudSyncStorage {
    private var syncFileUri: Uri? = null
    private var lastTimestamp: Long? = null
    private var deviceName: String? = null
    private var displayName: String? = null
    private var lastAccessError: String? = null
    private var remoteFingerprint: String? = null
    private var baselineFingerprint: String? = null
    private var initialized = false
    private val initializationMutex = Mutex()
    private val operationLock = Any()

    actual suspend fun initialize() {
        if (initialized) return
        initializationMutex.withLock {
            if (initialized) return
            val uri = loadSavedUri()
            if (uri != null && hasPersistedPermission(uri, requireWrite = true)) {
                syncFileUri = uri
                baselineFingerprint = loadBaselineFingerprint()
                refreshDisplayName(uri)
                runCatching { readContent(uri) }
                    .onSuccess { applyRemoteContent(it) }
                    .onFailure { setReadError(it) }
            }
            initialized = true
        }
    }

    private fun loadSavedUri(): Uri? {
        val ctx = appContext ?: return null
        val preferences = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = preferences.getString(URI_KEY, null)
            ?: preferences.getString(LEGACY_URI_KEY, null)
            ?: return null
        return runCatching { Uri.parse(uriString) }
            .getOrNull()
            ?.takeIf { it.scheme == "content" }
    }

    private fun loadBaselineFingerprint(): String? {
        val ctx = appContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(BASELINE_KEY, null)
    }

    private fun saveConfiguration(uri: Uri, fingerprint: String?): Boolean {
        val ctx = appContext ?: return false
        val editor = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(URI_KEY, uri.toString())
        if (fingerprint == null) {
            editor.remove(BASELINE_KEY)
        } else {
            editor.putString(BASELINE_KEY, fingerprint)
        }
        return editor.commit()
    }

    private fun releaseUriPermission(uri: Uri) {
        val ctx = appContext ?: return
        runCatching {
            ctx.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private fun hasPersistedPermission(uri: Uri, requireWrite: Boolean = false): Boolean {
        val ctx = appContext ?: return false
        return ctx.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && (!requireWrite || it.isWritePermission)
        }
    }

    private fun applyRemoteContent(content: RemoteContent) {
        remoteFingerprint = content.fingerprint
        runCatching { parseBackupSnapshot(content.text) }
            .onSuccess { data ->
                lastTimestamp = data.lastSyncedAt
                deviceName = data.syncedByDevice
                lastAccessError = null
            }
            .onFailure { setReadError(it) }
    }

    private fun setReadError(error: Throwable) {
        remoteFingerprint = null
        lastTimestamp = null
        deviceName = null
        lastAccessError = when (error) {
            is IllegalArgumentException, is kotlinx.serialization.SerializationException ->
                "Файл sync.json повреждён или имеет неверный формат"
            else -> "Не удалось прочитать sync.json: ${error.message ?: "ошибка Google Диска"}"
        }
    }

    private fun readContent(uri: Uri): RemoteContent {
        val ctx = appContext ?: throw IllegalStateException("Хранилище не инициализировано")
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val output = ByteArrayOutputStream()
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_FILE_SIZE_BYTES) {
                    throw IllegalArgumentException("Облачный файл слишком большой")
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IllegalStateException("Облачный файл недоступен")
        val text = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
        return RemoteContent(text, bytes, sha256(bytes))
    }

    private fun refreshDisplayName(uri: Uri) {
        displayName = queryDisplayName(uri)
    }

    private fun queryDisplayName(uri: Uri): String? {
        val ctx = appContext ?: return null
        return runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
                }
        }.getOrNull()
    }

    private fun writeContent(uri: Uri, bytes: ByteArray, onWriteStarted: () -> Unit = {}) {
        val ctx = appContext ?: throw IllegalStateException("Хранилище не инициализировано")
        val output = ctx.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Не удалось открыть облачный файл для записи")
        onWriteStarted()
        output.use { stream ->
            stream.write(bytes)
            stream.flush()
            (stream as? java.io.FileOutputStream)?.fd?.sync()
        }
    }

    actual fun verify(content: String): CloudSyncResult<Unit> = synchronized(operationLock) {
        val uri = syncFileUri ?: return@synchronized CloudSyncResult.Failure("Сначала выберите sync.json в Google Диске")
        if (!hasPersistedPermission(uri)) {
            return@synchronized CloudSyncResult.Failure("Нет доступа к sync.json. Выберите файл заново")
        }
        runCatching { readContent(uri) }
            .fold(
                onSuccess = { current ->
                    if (current.fingerprint == remoteFingerprint && current.text == content) {
                        CloudSyncResult.Success(Unit)
                    } else {
                        CloudSyncResult.Failure("Облачный файл изменился на другом устройстве. Повторите загрузку")
                    }
                },
                onFailure = { error ->
                    CloudSyncResult.Failure("Не удалось проверить облачный файл: ${error.message}")
                }
            )
    }

    actual fun accept(content: String): CloudSyncResult<Unit> = synchronized(operationLock) {
        val uri = syncFileUri ?: return@synchronized CloudSyncResult.Failure("Сначала выберите sync.json в Google Диске")
        if (!hasPersistedPermission(uri, requireWrite = true)) {
            return@synchronized CloudSyncResult.Failure("Нет доступа на запись. Выберите sync.json заново")
        }
        runCatching { readContent(uri) }
            .fold(
                onSuccess = { current ->
                    if (current.fingerprint != remoteFingerprint || current.text != content) {
                        CloudSyncResult.Failure("Облачный файл изменился на другом устройстве. Повторите загрузку")
                    } else if (!saveConfiguration(uri, current.fingerprint)) {
                        CloudSyncResult.Failure("Не удалось сохранить состояние синхронизации")
                    } else {
                        baselineFingerprint = current.fingerprint
                        CloudSyncResult.Success(Unit)
                    }
                },
                onFailure = { error ->
                    CloudSyncResult.Failure("Не удалось подтвердить облачный файл: ${error.message}")
                }
            )
    }

    actual fun save(content: String): CloudSyncResult<Unit> = synchronized(operationLock) {
        val ctx = appContext ?: return@synchronized CloudSyncResult.Failure("Хранилище не инициализировано")
        val uri = syncFileUri ?: return@synchronized CloudSyncResult.Failure("Сначала выберите sync.json в Google Диске")
        if (!hasPersistedPermission(uri, requireWrite = true)) {
            return@synchronized CloudSyncResult.Failure("Нет доступа на запись. Выберите sync.json заново")
        }
        if (remoteFingerprint == null || baselineFingerprint != remoteFingerprint) {
            return@synchronized CloudSyncResult.Failure("Сначала загрузите облачные данные и подтвердите восстановление")
        }
        val bytes = runCatching { content.toByteArray(Charsets.UTF_8) }.getOrElse {
            return@synchronized CloudSyncResult.Failure("Не удалось подготовить sync.json: ${it.message}")
        }
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            return@synchronized CloudSyncResult.Failure("Облачный файл слишком большой")
        }
        runCatching { parseBackupSnapshot(content) }.getOrElse { error ->
            return@synchronized CloudSyncResult.Failure("Не удалось проверить sync.json: ${error.message}")
        }

        val previous = runCatching { readContent(uri) }.getOrElse { error ->
            return@synchronized CloudSyncResult.Failure("Не удалось проверить облачный файл: ${error.message}")
        }
        if (previous.fingerprint != remoteFingerprint) {
            return@synchronized CloudSyncResult.Failure("Облачный файл изменился на другом устройстве. Сначала загрузите актуальные данные")
        }

        var writeStarted = false
        val expectedFingerprint = sha256(bytes)
        try {
            writeContent(uri, bytes) { writeStarted = true }
            val saved = readContent(uri)
            if (saved.text != content || !saved.bytes.contentEquals(bytes)) {
                throw IllegalStateException("Google Диск не подтвердил запись файла")
            }
            applyRemoteContent(saved)
            if (!saveConfiguration(uri, saved.fingerprint)) {
                throw IllegalStateException("Не удалось сохранить состояние синхронизации")
            }
            baselineFingerprint = saved.fingerprint
            CloudSyncResult.Success(Unit)
        } catch (error: Exception) {
            val current = runCatching { readContent(uri) }.getOrNull()
            when {
                current?.fingerprint == previous.fingerprint -> {
                    CloudSyncResult.Failure("Не удалось записать sync.json: ${error.message ?: "ошибка Google Диска"}")
                }
                current?.fingerprint == expectedFingerprint -> {
                    applyRemoteContent(current)
                    CloudSyncResult.Failure("Запись выполнена, но состояние синхронизации не сохранено: ${error.message ?: "ошибка Google Диска"}")
                }
                writeStarted && current != null && current.bytes.isPrefixOf(bytes) -> {
                    val restored = runCatching {
                        writeContent(uri, previous.bytes)
                        val restoredContent = readContent(uri)
                        check(restoredContent.fingerprint == previous.fingerprint) { "Контрольная сумма не совпадает" }
                    }.isSuccess
                    if (restored) {
                        applyRemoteContent(previous)
                        CloudSyncResult.Failure("Не удалось записать sync.json: ${error.message ?: "ошибка Google Диска"}. Предыдущий файл восстановлен")
                    } else {
                        setReadError(error)
                        CloudSyncResult.Failure("sync.json мог быть повреждён, а восстановление не выполнено. Синхронизацию остановлено")
                    }
                }
                else -> {
                    if (current == null) setReadError(error) else applyRemoteContent(current)
                    CloudSyncResult.Failure("Облачный файл изменился или повреждён во время записи. Повторите загрузку")
                }
            }
        }
    }

    actual fun load(): CloudSyncResult<CloudBackupSnapshot> = synchronized(operationLock) {
        val uri = syncFileUri ?: return@synchronized CloudSyncResult.Failure("Сначала выберите sync.json в Google Диске")
        if (!hasPersistedPermission(uri)) {
            return@synchronized CloudSyncResult.Failure("Нет доступа к sync.json. Выберите файл заново")
        }
        runCatching {
            val content = readContent(uri)
            val data = parseBackupSnapshot(content.text)
            applyRemoteContent(content)
            CloudSyncResult.Success(CloudBackupSnapshot(content.text, data.patients.size, data.syncedByDevice))
        }.getOrElse { error ->
            setReadError(error)
            CloudSyncResult.Failure(lastAccessError ?: "Не удалось прочитать sync.json")
        }
    }

    actual fun statusText(): String {
        val uri = syncFileUri ?: return "Google Диск: файл sync.json не выбран"
        val name = displayName ?: "sync.json"
        val meta = mutableListOf<String>()
        deviceName?.let { meta.add(it) }
        lastTimestamp?.let { meta.add(formatTimestamp(it)) }
        val access = if (hasPersistedPermission(uri, requireWrite = true)) "" else " — нет доступа, выберите файл заново"
        val readiness = if (canPush) "" else " — сначала загрузите и подтвердите облачные данные"
        val error = lastAccessError?.let { " — $it" } ?: ""
        val status = if (meta.isEmpty()) "$name$access$readiness" else "$name (${meta.joinToString(", ")})$access$readiness"
        return "Google Диск: $status$error"
    }

    actual val isConfigured: Boolean
        get() = syncFileUri?.let { hasPersistedPermission(it, requireWrite = true) } == true

    actual val canPush: Boolean
        get() = isConfigured && remoteFingerprint != null && baselineFingerprint == remoteFingerprint

    actual fun configure(path: String): CloudSyncResult<Unit> = synchronized(operationLock) {
        val ctx = appContext ?: return@synchronized CloudSyncResult.Failure("Хранилище не инициализировано")
        val uri = runCatching { Uri.parse(path) }.getOrNull()
            ?.takeIf { it.scheme == "content" }
            ?: return@synchronized CloudSyncResult.Failure("Выбран недействительный адрес файла")
        if (!hasPersistedPermission(uri, requireWrite = true)) {
            if (uri != syncFileUri) releaseUriPermission(uri)
            return@synchronized CloudSyncResult.Failure("Не удалось получить постоянный доступ на чтение и запись")
        }
        val name = queryDisplayName(uri)
        if (!name.equals(SYNC_FILE_NAME, ignoreCase = true)) {
            if (uri != syncFileUri) releaseUriPermission(uri)
            return@synchronized CloudSyncResult.Failure("Выберите файл sync.json")
        }
        val content = runCatching { readContent(uri) }.getOrElse { error ->
            if (uri != syncFileUri) releaseUriPermission(uri)
            return@synchronized CloudSyncResult.Failure("Выбранный файл нельзя использовать для синхронизации: ${error.message ?: "ошибка Google Диска"}")
        }
        val data = runCatching { parseBackupSnapshot(content.text) }.getOrElse { error ->
            if (uri != syncFileUri) releaseUriPermission(uri)
            return@synchronized CloudSyncResult.Failure("Выбранный файл нельзя использовать для синхронизации: ${error.message ?: "ошибка Google Диска"}")
        }
        if (!saveConfiguration(uri, null)) {
            if (uri != syncFileUri) releaseUriPermission(uri)
            return@synchronized CloudSyncResult.Failure("Не удалось сохранить постоянную настройку sync.json")
        }

        val oldUri = syncFileUri
        syncFileUri = uri
        baselineFingerprint = null
        displayName = name
        remoteFingerprint = content.fingerprint
        lastTimestamp = data.lastSyncedAt
        deviceName = data.syncedByDevice
        lastAccessError = null
        if (oldUri != null && oldUri != uri) releaseUriPermission(oldUri)
        CloudSyncResult.Success(Unit)
    }

    actual fun getLastSyncTimestamp(): Long? = lastTimestamp

    actual fun getDeviceName(): String? = deviceName

    actual fun openFolder(): CloudSyncResult<Unit> {
        val ctx = appContext ?: return CloudSyncResult.Failure("Хранилище не инициализировано")
        val uri = syncFileUri ?: return CloudSyncResult.Failure("Сначала выберите sync.json")
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/json")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            CloudSyncResult.Success(Unit)
        }.getOrElse { error ->
            if (error is ActivityNotFoundException) {
                CloudSyncResult.Failure("Не найдено приложение для открытия Google Диска")
            } else {
                CloudSyncResult.Failure("Не удалось открыть Google Диск")
            }
        }
    }

    private fun formatTimestamp(epochMs: Long): String {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
        return sdf.format(java.util.Date(epochMs))
    }

    private data class RemoteContent(
        val text: String,
        val bytes: ByteArray,
        val fingerprint: String
    )

    companion object {
        private var appContext: Context? = null
        private const val PREFS_NAME = "cloud_sync_prefs"
        private const val URI_KEY = "sync_file_uri_v2"
        private const val LEGACY_URI_KEY = "sync_file_uri"
        private const val BASELINE_KEY = "cloud_baseline_fingerprint_v1"
        private const val SYNC_FILE_NAME = "sync.json"
        private const val MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024

        fun init(context: Context) {
            appContext = context.applicationContext
        }
    }
}

private fun sha256(bytes: ByteArray): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

private fun ByteArray.isPrefixOf(value: ByteArray): Boolean {
    return size <= value.size && value.copyOfRange(0, size).contentEquals(this)
}
