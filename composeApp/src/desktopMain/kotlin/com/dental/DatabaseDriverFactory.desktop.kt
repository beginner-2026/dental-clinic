package com.dental.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.dental.data.db.DentalDatabase
import java.io.File
import java.sql.DriverManager

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val userHome = System.getProperty("user.home").replace('\\', '/')
        val dir = File(userHome, ".dental-clinic")
        dir.mkdirs()
        val dbFile = File(dir, "dental.db")
        val path = dbFile.absolutePath.replace('\\', '/')

        // Delete old database file entirely to ensure fresh schema
        dbFile.delete()
        System.gc()
        Thread.sleep(200)
        dbFile.delete()
        File(dir, "dental.db-wal").delete()
        File(dir, "dental.db-shm").delete()

        val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
        DentalDatabase.Schema.create(driver)
        return driver
    }
}
