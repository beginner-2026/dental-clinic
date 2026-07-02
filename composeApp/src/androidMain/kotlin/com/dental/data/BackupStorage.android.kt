package com.dental.data.sync

import android.content.Context
import java.io.File

actual object BackupStorage {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getBackupDir(): File? {
        val ctx = appContext ?: return null
        val dir = File(ctx.filesDir, "backups")
        dir.mkdirs()
        return dir
    }

    actual fun save(json: String): Boolean {
        return try {
            val dir = getBackupDir() ?: return false
            val timestamp = System.currentTimeMillis()
            val file = File(dir, "backup_$timestamp.json")
            file.writeText(json)

            val latest = File(dir, "backup_latest.json")
            latest.writeText(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun load(): String? {
        return try {
            val dir = getBackupDir() ?: return null
            val latest = File(dir, "backup_latest.json")
            if (latest.exists()) latest.readText() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
