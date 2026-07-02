package com.dental.data.sync

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual object BackupStorage {
    private val backupDir: File by lazy {
        val userHome = System.getProperty("user.home").replace('\\', '/')
        val dir = File(userHome, ".dental-clinic/backups")
        dir.mkdirs()
        dir
    }

    actual fun save(json: String): Boolean {
        return try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val file = File(backupDir, "backup_$timestamp.json")
            file.writeText(json)

            val latest = File(backupDir, "backup_latest.json")
            latest.writeText(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun load(): String? {
        return try {
            val latest = File(backupDir, "backup_latest.json")
            if (latest.exists()) latest.readText() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listBackups(): List<File> {
        return backupDir.listFiles()
            ?.filter { it.name.startsWith("backup_") && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
