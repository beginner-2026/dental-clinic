package com.dental.data.sync

import java.io.File

actual class CloudSyncStorage {
    private var syncFile: File? = null

    init {
        detectCloudFolder()
    }

    private fun detectCloudFolder() {
        val home = System.getProperty("user.home").replace('\\', '/')
        val candidates = listOf(
            "$home/Google Drive",
            "$home/My Drive",
            "$home/Yandex.Disk",
            "$home/YandexDisk",
            "$home/OneDrive",
            "$home/Dropbox"
        )

        val existing = candidates.firstOrNull { File(it).exists() }

        val baseDir = if (existing != null) {
            File(existing, "DentalClinic")
        } else {
            File(home, ".dental-clinic/cloud-sync")
        }

        baseDir.mkdirs()
        syncFile = File(baseDir, "sync.json")
    }

    actual fun save(json: String): Boolean {
        val file = syncFile ?: return false
        return try {
            file.parentFile?.mkdirs()
            file.writeText(json)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun load(): String? {
        val file = syncFile ?: return null
        return try {
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun statusText(): String {
        val file = syncFile ?: return "Не настроено"
        return file.absolutePath
    }

    actual val isConfigured: Boolean get() = syncFile != null

    actual fun configure(path: String) {
        val dir = File(path)
        if (dir.isDirectory() || dir.mkdirs()) {
            syncFile = File(dir, "sync.json")
        }
    }
}
