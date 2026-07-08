package com.dental.data.sync

import android.content.Context
import android.net.Uri

actual class CloudSyncStorage {
    private var syncFileUri: Uri? = null

    init {
        syncFileUri = loadSavedUri()
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

    actual fun save(json: String): Boolean {
        val ctx = appContext ?: return false
        val uri = syncFileUri ?: return false
        return try {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun load(): String? {
        val ctx = appContext ?: return null
        val uri = syncFileUri ?: return null
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { ins ->
                ins.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun statusText(): String {
        val uri = syncFileUri ?: return "Не выбрано"
        return uri.lastPathSegment ?: uri.toString()
    }

    actual val isConfigured: Boolean get() = syncFileUri != null

    actual fun configure(path: String) {
        val uri = try { Uri.parse(path) } catch (_: Exception) { null }
        if (uri != null) {
            syncFileUri = uri
            saveUri(uri)
        }
    }

    companion object {
        private var appContext: Context? = null
        private const val PREFS_NAME = "cloud_sync_prefs"
        private const val URI_KEY = "sync_file_uri"

        fun init(context: Context) {
            appContext = context.applicationContext
        }
    }
}
