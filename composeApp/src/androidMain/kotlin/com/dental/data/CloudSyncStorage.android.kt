package com.dental.data.sync

import android.content.Context
import android.net.Uri
import kotlinx.serialization.json.Json

actual class CloudSyncStorage {
    private var syncFileUri: Uri? = null
    private var lastTimestamp: Long? = null
    private var deviceName: String? = null

    init {
        syncFileUri = loadSavedUri()
        parseMetadata()
    }

    private fun loadSavedUri(): Uri? {
        val ctx = appContext ?: return null
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(URI_KEY, null) ?: return null
        return try {
            Uri.parse(uriString)
        } catch (_: Exception) { null }
    }

    private fun saveUri(uri: Uri) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(URI_KEY, uri.toString())
            .apply()
    }

    private fun parseMetadata() {
        val text = loadRaw()
        if (text != null) {
            try {
                val data = json.decodeFromString<BackupData>(text)
                lastTimestamp = data.lastSyncedAt
                deviceName = data.syncedByDevice
            } catch (_: Exception) { }
        }
    }

    private fun loadRaw(): String? {
        val ctx = appContext ?: return null
        val uri = syncFileUri ?: return null
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { ins ->
                ins.readBytes().toString(Charsets.UTF_8)
            }
        } catch (_: Exception) { null }
    }

    actual fun save(json: String): Boolean {
        val ctx = appContext ?: return false
        val uri = syncFileUri ?: return false
        return try {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }
            parseMetadata()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun load(): String? {
        val text = loadRaw()
        if (text != null) {
            try {
                val data = json.decodeFromString<BackupData>(text)
                lastTimestamp = data.lastSyncedAt
                deviceName = data.syncedByDevice
            } catch (_: Exception) { }
        }
        return text
    }

    actual fun statusText(): String {
        val uri = syncFileUri ?: return "Не выбрано"
        val name = uri.lastPathSegment ?: uri.toString()
        val meta = mutableListOf<String>()
        deviceName?.let { meta.add(it) }
        lastTimestamp?.let { meta.add(formatTimestamp(it)) }
        return if (meta.isNotEmpty()) "$name (${meta.joinToString(", ")})" else name
    }

    actual val isConfigured: Boolean get() = syncFileUri != null

    actual fun configure(path: String) {
        val uri = try { Uri.parse(path) } catch (_: Exception) { null }
        if (uri != null) {
            syncFileUri = uri
            saveUri(uri)
            parseMetadata()
        }
    }

    actual fun getLastSyncTimestamp(): Long? = lastTimestamp

    actual fun getDeviceName(): String? = deviceName

    actual fun openFolder() {
        // SAF не поддерживает открытие папки — no-op на Android
    }

    private fun formatTimestamp(epochMs: Long): String {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("ru"))
        return sdf.format(java.util.Date(epochMs))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private var appContext: Context? = null
        private const val PREFS_NAME = "cloud_sync_prefs"
        private const val URI_KEY = "sync_file_uri"

        fun init(context: Context) {
            appContext = context.applicationContext
        }
    }
}
