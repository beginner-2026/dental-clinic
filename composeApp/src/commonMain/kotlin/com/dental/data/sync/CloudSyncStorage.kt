package com.dental.data.sync

sealed interface CloudSyncResult<out T> {
    data class Success<T>(val value: T) : CloudSyncResult<T>
    data class Failure(val message: String) : CloudSyncResult<Nothing>
}

data class CloudBackupSnapshot(
    val json: String,
    val patientCount: Int,
    val deviceName: String?
)

expect class CloudSyncStorage() {
    suspend fun initialize()
    fun save(content: String): CloudSyncResult<Unit>
    fun verify(content: String): CloudSyncResult<Unit>
    fun accept(content: String): CloudSyncResult<Unit>
    fun load(): CloudSyncResult<CloudBackupSnapshot>
    fun statusText(): String
    val isConfigured: Boolean
    val canPush: Boolean
    fun configure(path: String): CloudSyncResult<Unit>
    fun getLastSyncTimestamp(): Long?
    fun getDeviceName(): String?
    fun openFolder(): CloudSyncResult<Unit>
}
