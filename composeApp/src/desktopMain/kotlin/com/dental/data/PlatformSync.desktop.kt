package com.dental.data.sync

import com.dental.data.SyncServer

actual fun platformStartSyncServer(backupManager: BackupManager): String? {
    return try {
        val server = SyncServer(backupManager)
        if (server.start()) "http://localhost:${server.actualPort}" else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
