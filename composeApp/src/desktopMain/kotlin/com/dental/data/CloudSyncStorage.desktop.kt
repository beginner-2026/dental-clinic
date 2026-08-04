package com.dental.data.sync

import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.File

actual class CloudSyncStorage {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
    private var syncFile: File? = null
    private var lastTimestamp: Long? = null
    private var deviceName: String? = null

    init {
        detectCloudFolder()
        parseMetadata()
    }

    private fun syncDir(parent: File) = File(File(parent, "Sync"), "OpenCode - DentalClinic")

    private fun detectCloudFolder() {
        val home = System.getProperty("user.home").replace('\\', '/')

        for (dir in listOf("$home/Google Drive", "$home/My Drive")) {
            val f = File(dir)
            if (f.exists() && f.canWrite()) {
                useDir(syncDir(f))
                return
            }
        }

        val driveFromRegistry = queryGoogleDriveDriveLetter()
        if (driveFromRegistry != null) {
            val root = File("$driveFromRegistry/")
            if (root.exists()) {
                for (sub in listOf("Мой диск", "My Drive", "Google Drive", "")) {
                    val dir = if (sub.isEmpty()) root else File(root, sub)
                    if (dir.exists() && dir.canWrite()) {
                        useDir(syncDir(dir))
                        return
                    }
                }
            }
        }

        for (letter in listOf("G:", "H:", "I:", "J:", "K:")) {
            for (name in listOf("Мой диск", "My Drive", "Google Drive", "")) {
                val dir = if (name.isEmpty()) File("$letter/") else File("$letter/$name")
                if (dir.exists() && dir.canWrite()) {
                    useDir(syncDir(dir))
                    return
                }
            }
        }

        val fallback = File(home, ".dental-clinic/cloud-sync")
        useDir(fallback)
    }

    private fun queryGoogleDriveDriveLetter(): String? {
        return try {
            val proc = Runtime.getRuntime().exec(
                arrayOf("reg", "query", "HKCU\\Software\\Google\\DriveFS\\Share", "/v", "SyncTargets")
            )
            val output = proc.inputStream.bufferedReader().readText()
            Regex("""([A-Z]):""").find(output)?.groupValues?.get(1)
        } catch (_: Exception) { null }
    }

    private fun useDir(dir: File) {
        dir.mkdirs()
        syncFile = File(dir, "sync.json")
    }

    private fun parseMetadata() {
        val text = try { syncFile?.takeIf { it.exists() }?.readText() } catch (_: Exception) { null }
        if (text != null) {
            try {
                val data = json.decodeFromString<BackupData>(text)
                lastTimestamp = data.lastSyncedAt
                deviceName = data.syncedByDevice
            } catch (_: Exception) { }
        }
    }

    actual fun save(json: String): Boolean {
        val file = syncFile ?: return false
        return try {
            file.parentFile?.mkdirs()
            file.writeText(json)
            parseMetadata()
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
        val parent = file.parentFile?.absolutePath ?: "?"
        return if (file.exists()) {
            val meta = if (lastTimestamp != null || deviceName != null) {
                val parts = mutableListOf<String>()
                deviceName?.let { parts.add(it) }
                lastTimestamp?.let { parts.add(formatTimestamp(it)) }
                " (${parts.joinToString(", ") { it }})"
            } else ""
            "$parent$meta"
        } else {
            parent
        }
    }

    actual val isConfigured: Boolean get() = syncFile != null

    actual fun configure(path: String) {
        val dir = File(path)
        if (dir.isDirectory() || dir.mkdirs()) {
            syncFile = File(dir, "sync.json")
            parseMetadata()
        }
    }

    actual fun getLastSyncTimestamp(): Long? = lastTimestamp

    actual fun getDeviceName(): String? = deviceName

    actual fun openFolder() {
        val file = syncFile?.parentFile ?: return
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file)
            } catch (_: Exception) { }
        }
    }

    private fun formatTimestamp(epochMs: Long): String {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("ru"))
        return sdf.format(java.util.Date(epochMs))
    }
}
