package com.dental.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

actual class CloudSyncStorage {
    private var syncFile: File? = null
    private var cloudRoot: File? = null
    private var usingLocalFallback = false
    private var lastTimestamp: Long? = null
    private var deviceName: String? = null
    private var lastAccessError: String? = null
    private var remoteFingerprint: String? = null
    private var initialized = false
    private val initializationMutex = Mutex()
    private val operationLock = Any()

    actual suspend fun initialize() {
        if (initialized) return
        initializationMutex.withLock {
            if (initialized) return
            val savedTarget = loadSavedTarget()
            if (savedTarget != null && inferCloudRoot(savedTarget).isDirectory) {
                useTarget(savedTarget)
            } else {
                detectCloudRoot()
            }
            refreshMetadata()
            cleanupStagingFiles()
            initialized = true
        }
    }

    private fun canonicalFile(root: File): File {
        val syncDir = if (root.name.equals(SYNC_DIR_NAME, ignoreCase = true)) {
            root
        } else {
            File(root, SYNC_DIR_NAME)
        }
        return File(syncDir, SYNC_FILE_NAME)
    }

    private fun findSyncFile(root: File): File {
        val canonical = canonicalFile(root)
        if (canonical.isFile) return canonical
        val legacyFiles = listOf(
            File(root, SYNC_FILE_NAME),
            File(File(root, "Sync"), "OpenCode - DentalClinic/$SYNC_FILE_NAME")
        )
        return legacyFiles.firstOrNull { it.isFile } ?: canonical
    }

    private fun detectCloudRoot() {
        val home = File(System.getProperty("user.home"))
        val candidates = mutableListOf<File>()
        fun addRoot(path: File) {
            val root = path.absoluteFile
            if (root.isDirectory && root.canWrite()) candidates.add(root)
        }

        addRoot(File(home, "Google Drive/My Drive"))
        addRoot(File(home, "Google Drive/Мой диск"))
        addRoot(File(home, "My Drive"))
        addRoot(File(home, "Мой диск"))
        addRoot(File(home, "Google Drive"))

        registryMountPoints().forEach { letter ->
            addRoot(File("$letter/My Drive"))
            addRoot(File("$letter/Мой диск"))
            addRoot(File("$letter/Google Drive"))
        }

        File.listRoots().forEach { root ->
            if (root.isDirectory) {
                addRoot(File(root, "My Drive"))
                addRoot(File(root, "Мой диск"))
                addRoot(File(root, "Google Drive"))
            }
        }

        addRoot(File(home, "OneDrive"))
        addRoot(File(home, "Dropbox"))
        addRoot(File(home, "Yandex.Disk"))
        addRoot(File(home, "Yandex Диск"))

        val fallback = File(home, ".dental-clinic/cloud-sync")
        val root = candidates.distinctBy { it.absolutePath.lowercase(Locale.ROOT) }.firstOrNull()
            ?: fallback.also { it.mkdirs() }
        usingLocalFallback = root.absolutePath.equals(fallback.absolutePath, ignoreCase = true)
        useTarget(canonicalFile(root))
    }

    private fun registryMountPoints(): List<String> {
        return listOf("MountPoint", "SyncTargets").mapNotNull { valueName ->
            runCatching {
                val process = ProcessBuilder(
                    "reg.exe",
                    "query",
                    "HKCU\\Software\\Google\\DriveFS\\Share",
                    "/v",
                    valueName
                ).redirectErrorStream(true).start()
                val outputFuture = CompletableFuture.supplyAsync {
                    process.inputStream.bufferedReader().use { it.readText() }
                }
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return@runCatching null
                }
                val output = outputFuture.get(2, TimeUnit.SECONDS)
                Regex("""(?m)^\s*(?:MountPoint|SyncTargets)\s+REG_SZ\s+([A-Za-z]):""")
                    .find(output)
                    ?.groupValues
                    ?.get(1)
                    ?.uppercase(Locale.ROOT)
            }.getOrNull()
        }.distinct()
    }

    private fun useTarget(target: File) {
        val normalized = target.absoluteFile.toPath().normalize().toFile()
        syncFile = normalized
        cloudRoot = inferCloudRoot(normalized)
    }

    private fun inferCloudRoot(target: File): File {
        val directory = target.parentFile ?: return target
        if (directory.name.equals(SYNC_DIR_NAME, ignoreCase = true)) {
            return directory.parentFile ?: directory
        }
        return directory
    }

    private fun prepareTarget(directory: File): File {
        return if (directory.name.equals(SYNC_DIR_NAME, ignoreCase = true)) {
            File(directory, SYNC_FILE_NAME)
        } else {
            val existing = findSyncFile(directory)
            if (existing.parentFile?.isDirectory == true) existing else canonicalFile(directory)
        }
    }

    private fun configFile(): File = File(File(System.getProperty("user.home"), ".dental-clinic"), "cloud-sync.properties")

    private fun loadSavedTarget(): File? {
        val file = configFile()
        if (!file.isFile) return null
        return runCatching {
            val properties = Properties()
            file.inputStream().use { properties.load(it) }
            properties.getProperty("targetPath")?.let(::File)?.absoluteFile
        }.getOrNull()
    }

    private fun saveTarget(target: File) {
        val file = configFile()
        val parent = file.parentFile
            ?: throw IllegalStateException("Не удалось определить папку конфигурации")
        if (!parent.isDirectory && !parent.mkdirs()) {
            throw IllegalStateException("Не удалось создать папку конфигурации")
        }
        val properties = Properties()
        if (file.isFile) file.inputStream().use { properties.load(it) }
        properties.setProperty("targetPath", target.absolutePath)
        val temp = File.createTempFile("cloud-sync-", ".tmp", parent)
        try {
            FileOutputStream(temp).use { output ->
                properties.store(output, "DentalClinic cloud sync")
                output.fd.sync()
            }
            moveFileIntoPlace(temp, file)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun refreshMetadata() {
        lastTimestamp = null
        deviceName = null
        lastAccessError = null
        remoteFingerprint = null
        val content = runCatching { readCurrentContent() }.getOrNull() ?: return
        inspectContent(content)
    }

    private fun inspectContent(content: RemoteContent) {
        remoteFingerprint = content.fingerprint
        runCatching { parseBackupSnapshot(content.text) }
            .onSuccess { data ->
                lastTimestamp = data.lastSyncedAt
                deviceName = data.syncedByDevice
                lastAccessError = null
            }
            .onFailure { error ->
                lastTimestamp = null
                deviceName = null
                lastAccessError = when (error) {
                    is IllegalArgumentException, is kotlinx.serialization.SerializationException ->
                        "Файл sync.json повреждён или имеет неверный формат"
                    else -> "Не удалось прочитать sync.json: ${error.message ?: "ошибка файловой системы"}"
                }
            }
    }

    private fun readCurrentContent(): RemoteContent? {
        val file = syncFile ?: return null
        return if (file.isFile) readContent(file) else null
    }

    private fun readContent(file: File): RemoteContent {
        if (!file.isFile) throw IllegalStateException("В Google Диске нет файла sync.json")
        if (file.length() > MAX_FILE_SIZE_BYTES) throw IllegalArgumentException("Облачный файл слишком большой")
        val bytes = Files.readAllBytes(file.toPath())
        if (bytes.size > MAX_FILE_SIZE_BYTES) throw IllegalArgumentException("Облачный файл слишком большой")
        val text = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
        return RemoteContent(text, bytes, sha256(bytes))
    }

    private fun cleanupStagingFiles() {
        val parent = syncFile?.parentFile ?: return
        parent.listFiles()
            ?.filter { it.name.startsWith(".sync-") && it.name.endsWith(".tmp") }
            ?.forEach { it.delete() }
    }

    actual fun verify(content: String): CloudSyncResult<Unit> = synchronized(operationLock) {
        runCatching { readCurrentContent() }
            .fold(
                onSuccess = { current ->
                    if (current?.fingerprint == remoteFingerprint && current?.text == content) {
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

    actual fun accept(content: String): CloudSyncResult<Unit> = verify(content)

    actual fun save(content: String): CloudSyncResult<Unit> = synchronized(operationLock) {
        val file = syncFile ?: return@synchronized CloudSyncResult.Failure("Сначала выберите папку Google Диска")
        val bytes = runCatching { content.toByteArray(Charsets.UTF_8) }.getOrElse {
            return@synchronized CloudSyncResult.Failure("Не удалось подготовить sync.json: ${it.message}")
        }
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            return@synchronized CloudSyncResult.Failure("Облачный файл слишком большой")
        }
        val data = runCatching { parseBackupSnapshot(content) }.getOrElse { error ->
            return@synchronized CloudSyncResult.Failure("Не удалось проверить sync.json: ${error.message}")
        }

        val previous = runCatching { readCurrentContent() }.getOrElse { error ->
            return@synchronized CloudSyncResult.Failure("Не удалось проверить облачный файл: ${error.message}")
        }
        val currentFingerprint = previous?.fingerprint
        if (currentFingerprint != remoteFingerprint) {
            return@synchronized CloudSyncResult.Failure("Облачный файл изменился на другом устройстве. Сначала загрузите актуальные данные")
        }

        val parent = file.parentFile ?: return@synchronized CloudSyncResult.Failure("У папки нет родительского каталога")
        if (!parent.isDirectory && !parent.mkdirs()) {
            return@synchronized CloudSyncResult.Failure("Не удалось создать папку ${parent.absolutePath}")
        }
        if (!parent.canWrite()) return@synchronized CloudSyncResult.Failure("Папка Google Диска недоступна для записи")

        val temp = File.createTempFile(".sync-", ".tmp", parent)
        var replacementStarted = false
        try {
            FileOutputStream(temp).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            if (!temp.readBytes().contentEquals(bytes)) {
                throw IllegalStateException("Локальная проверка записи не пройдена")
            }
            replacementStarted = true
            moveFileIntoPlace(temp, file)
            val saved = readContent(file)
            if (!saved.bytes.contentEquals(bytes) || saved.text != content) {
                throw IllegalStateException("Google Диск не подтвердил запись файла")
            }
            remoteFingerprint = saved.fingerprint
            lastTimestamp = data.lastSyncedAt
            deviceName = data.syncedByDevice
            lastAccessError = null
            CloudSyncResult.Success(Unit)
        } catch (error: Exception) {
            if (replacementStarted) {
                val restored = runCatching { restorePrevious(file, previous) }.isSuccess
                if (restored) {
                    if (previous == null) {
                        remoteFingerprint = null
                        lastTimestamp = null
                        deviceName = null
                        lastAccessError = null
                    } else {
                        inspectContent(previous)
                    }
                    CloudSyncResult.Failure("Не удалось записать sync.json: ${error.message ?: "ошибка файловой системы"}. Предыдущий файл восстановлен")
                } else {
                    remoteFingerprint = null
                    lastTimestamp = null
                    deviceName = null
                    lastAccessError = "Предыдущий облачный файл не удалось восстановить"
                    CloudSyncResult.Failure("sync.json мог быть повреждён, а восстановление не выполнено. Синхронизацию остановлено")
                }
            } else {
                CloudSyncResult.Failure("Не удалось записать sync.json: ${error.message ?: "ошибка файловой системы"}")
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun restorePrevious(target: File, previous: RemoteContent?) {
        if (previous == null) {
            if (target.exists() && !target.delete()) throw IllegalStateException("Не удалось удалить повреждённый sync.json")
            if (target.exists()) throw IllegalStateException("Повреждённый sync.json не удалён")
            return
        }
        val parent = target.parentFile ?: throw IllegalStateException("У файла нет родительской папки")
        val restore = File.createTempFile(".sync-restore-", ".tmp", parent)
        try {
            FileOutputStream(restore).use { output ->
                output.write(previous.bytes)
                output.fd.sync()
            }
            moveFileIntoPlace(restore, target)
            val restored = readContent(target)
            if (!restored.bytes.contentEquals(previous.bytes)) {
                throw IllegalStateException("Контрольная сумма восстановленного файла не совпадает")
            }
        } finally {
            if (restore.exists()) restore.delete()
        }
    }

    actual fun load(): CloudSyncResult<CloudBackupSnapshot> = synchronized(operationLock) {
        val file = syncFile ?: return@synchronized CloudSyncResult.Failure("Сначала выберите папку Google Диска")
        if (!file.isFile) return@synchronized CloudSyncResult.Failure("В Google Диске нет файла sync.json")
        runCatching {
            val content = readContent(file)
            val data = parseBackupSnapshot(content.text)
            inspectContent(content)
            if (lastAccessError != null) throw IllegalStateException(lastAccessError)
            CloudSyncResult.Success(CloudBackupSnapshot(content.text, data.patients.size, data.syncedByDevice))
        }.getOrElse { error ->
            val message = when (error) {
                is IllegalArgumentException, is kotlinx.serialization.SerializationException ->
                    "Файл sync.json повреждён или имеет неверный формат"
                else -> "Не удалось прочитать sync.json: ${error.message ?: "ошибка Google Диска"}"
            }
            lastAccessError = message
            CloudSyncResult.Failure(message)
        }
    }

    actual fun statusText(): String {
        val file = syncFile ?: return "Google Диск не найден. Выберите папку My Drive"
        val prefix = if (usingLocalFallback) "Локальный fallback: $file" else file.absolutePath
        val meta = mutableListOf<String>()
        deviceName?.let { meta.add(it) }
        lastTimestamp?.let { meta.add(formatTimestamp(it)) }
        val availability = if (file.isFile) "" else " — файл ещё не создан"
        val error = lastAccessError?.let { " — $it" } ?: ""
        return if (meta.isEmpty()) {
            "$prefix$availability$error"
        } else {
            "$prefix (${meta.joinToString(", ")})$availability$error"
        }
    }

    actual val isConfigured: Boolean
        get() = syncFile != null && cloudRoot?.isDirectory == true && cloudRoot?.canWrite() == true

    actual val canPush: Boolean
        get() = isConfigured

    actual fun configure(path: String): CloudSyncResult<Unit> = synchronized(operationLock) {
        val selected = File(path).absoluteFile
        if (!selected.isDirectory) return@synchronized CloudSyncResult.Failure("Выберите папку Google Диска")
        if (!selected.canWrite()) return@synchronized CloudSyncResult.Failure("Выбранная папка недоступна для записи")
        runCatching {
            val target = prepareTarget(selected)
            if (target.isFile) readContent(target)
            saveTarget(target)
            usingLocalFallback = false
            useTarget(target)
            refreshMetadata()
            CloudSyncResult.Success(Unit)
        }.getOrElse { error ->
            CloudSyncResult.Failure("Не удалось подключить папку: ${error.message ?: "ошибка файловой системы"}")
        }
    }

    actual fun getLastSyncTimestamp(): Long? = lastTimestamp

    actual fun getDeviceName(): String? = deviceName

    actual fun openFolder(): CloudSyncResult<Unit> {
        val folder = cloudRoot ?: return CloudSyncResult.Failure("Сначала выберите папку Google Диска")
        if (!folder.isDirectory) return CloudSyncResult.Failure("Папка Google Диска сейчас недоступна")
        if (!Desktop.isDesktopSupported()) return CloudSyncResult.Failure("Открытие папки не поддерживается")
        return runCatching {
            Desktop.getDesktop().open(folder)
            CloudSyncResult.Success(Unit)
        }.getOrElse {
            CloudSyncResult.Failure("Не удалось открыть папку Google Диска")
        }
    }

    private fun formatTimestamp(epochMs: Long): String {
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(epochMs))
    }

    private data class RemoteContent(
        val text: String,
        val bytes: ByteArray,
        val fingerprint: String
    )

    companion object {
        private const val SYNC_FILE_NAME = "sync.json"
        private const val SYNC_DIR_NAME = "DentalClinic"
        private const val MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L
    }
}

private fun sha256(bytes: ByteArray): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

internal fun moveFileIntoPlace(source: File, target: File) {
    try {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
    } catch (error: IOException) {
        if (!source.exists()) throw error
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
