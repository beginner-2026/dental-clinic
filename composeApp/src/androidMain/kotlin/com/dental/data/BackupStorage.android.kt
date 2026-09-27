package com.dental.data.sync

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

actual object BackupStorage {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getBackupDir(): File? {
        val ctx = appContext ?: return null
        val dir = File(ctx.filesDir, "backups")
        if (!dir.isDirectory && !dir.mkdirs()) return null
        return dir
    }

    actual fun save(json: String): Boolean {
        return try {
            val dir = getBackupDir() ?: return false
            val bytes = json.toByteArray(Charsets.UTF_8)
            val timestamped = File(dir, "backup_${System.currentTimeMillis()}_${UUID.randomUUID()}.json")
            writeAtomic(timestamped, bytes)
            writeAtomic(File(dir, LATEST_FILE), bytes)
            pruneBackups(dir)
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun load(): String? {
        return try {
            val dir = getBackupDir() ?: return null
            readValid(File(dir, LATEST_FILE)) ?: dir.listFiles()
                .orEmpty()
                .filter { it.name.startsWith("backup_") && it.name.endsWith(".json") && it.name != LATEST_FILE }
                .sortedByDescending { it.lastModified() }
                .firstNotNullOfOrNull(::readValid)
        } catch (_: Exception) {
            null
        }
    }

    private fun pruneBackups(directory: File) {
        directory.listFiles()
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
        val parent = target.parentFile ?: throw IllegalStateException("У резервной копии нет папки")
        val temp = File.createTempFile(".backup-", ".tmp", parent)
        try {
            FileOutputStream(temp).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            try {
                Files.move(
                    temp.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (error: IOException) {
                if (!temp.exists()) throw error
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private const val LATEST_FILE = "backup_latest.json"
    private const val MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L
    private const val MAX_BACKUP_HISTORY = 20
}
