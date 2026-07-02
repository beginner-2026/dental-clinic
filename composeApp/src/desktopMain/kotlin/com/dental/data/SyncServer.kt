package com.dental.data

import com.dental.data.sync.BackupManager
import com.dental.data.sync.BackupData
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SyncResponse(val success: Boolean, val message: String = "")

class SyncServer(
    private val backupManager: BackupManager,
    private val port: Int = 9876
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    fun start(): Boolean {
        return try {
            server = embeddedServer(Netty, port = port) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        ignoreUnknownKeys = true
                    })
                }
                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.respond(HttpStatusCode.InternalServerError, SyncResponse(false, cause.message ?: "Unknown error"))
                    }
                }
                routing {
                    get("/api/sync/ping") {
                        call.respond(SyncResponse(true, "pong"))
                    }
                    post("/api/sync/pull") {
                        val data = backupManager.exportAll()
                        call.respond(data)
                    }
                    post("/api/sync/push") {
                        val data = call.receive<BackupData>()
                        backupManager.importFromData(data)
                        call.respond(SyncResponse(true, "Data imported successfully"))
                    }
                }
            }
            server?.start(wait = false)
            true
        } catch (e: Exception) {
            println("SyncServer failed to start on port $port: ${e.message}")
            false
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    val isRunning: Boolean get() = server != null
    val actualPort: Int get() = port
}
