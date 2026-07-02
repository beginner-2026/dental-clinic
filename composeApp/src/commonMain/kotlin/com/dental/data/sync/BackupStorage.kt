package com.dental.data.sync

expect object BackupStorage {
    fun save(json: String): Boolean
    fun load(): String?
}
