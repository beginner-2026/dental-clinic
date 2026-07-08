package com.dental.data.sync

expect class CloudSyncStorage() {
    fun save(json: String): Boolean
    fun load(): String?
    fun statusText(): String
    val isConfigured: Boolean
    fun configure(path: String)
}
