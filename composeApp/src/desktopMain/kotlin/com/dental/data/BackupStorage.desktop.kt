package com.dental.data.sync

import java.io.File
import java.io.FileOutputStream
import java.util.UUID

actual object BackupStorage {
    private val backupDir: File by lazy {
        val userHome = System.getProperty("user.home").replace('\\', '/')
        val dir = File(userHome, ".dental-clinic/backups")
        if (!dir.isDirectory && !dir.mkdirs()) throw IllegalStateException("Не удалось создать папку резервных копий")
        dir
    }

    actual fun save(json: String): Boolean {
        return try {
            val bytes = json.toByteArray(Charsets.UTF_8)
            val timestamped = File(backupDir, "backup_${System.currentTimeMillis()}_${UUID.randomUUID()}.json")
            writeAtomic(timestamped, bytes)
            writeAtomic(File(backupDir, LATEST_FILE), bytes)
            pruneBackups()
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun load(): String? {
        return try {
            readValid(File(backupDir, LATEST_FILE)) ?: backupDir.listFiles()
                .orEmpty()
                .filter { it.name.startsWith("backup_") && it.name.endsWith(".json") && it.name != LATEST_FILE }
                .sortedByDescending { it.lastModified() }
                .firstNotNullOfOrNull(::readValid)
        } catch (_: Exception) {
            null
        }
    }

    private fun pruneBackups() {
        backupDir.listFiles()
            .orEmpty()
            .filter { it.name.startsWith("backup_") && it.name.endsWith(".json") }
            .sortedByDescending { it.lastModified() }
            .drop(MAX_BACKUP_HISTORY)
            .forEach { it.delete() }
    }

    private fun readValid(file: File): String? {
        if (!file.isFile || file.length() > MAX_FILE_SIZE_BYTES) return null
        return runCatching {
            val text = file.readText(Charsets.UTF_8)
            parseBackupSnapshot(text)
            text
        }.getOrNull()
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val temp = File.createTempFile(".backup-", ".tmp", backupDir)
        try {
            FileOutputStream(temp).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            moveFileIntoPlace(temp, target)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private const val LATEST_FILE = "backup_latest.json"
    private const val MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L
    private const val MAX_BACKUP_HISTORY = 20
}
