package com.dental.data.sync

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

@Serializable
data class SyncResult(
    val success: Boolean,
    val message: String = ""
)

class SyncClient(private val serverUrl: String) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun pull(): BackupData {
        val response = client.post("$serverUrl/api/sync/pull")
        return response.body()
    }

    suspend fun push(data: BackupData): SyncResult {
        val response = client.post("$serverUrl/api/sync/push") {
            contentType(ContentType.Application.Json)
            setBody(data)
        }
        return response.body()
    }

    suspend fun ping(): Boolean {
        return try {
            val response = client.get("$serverUrl/api/sync/ping")
            response.body<SyncResult>().success
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        client.close()
    }
}
