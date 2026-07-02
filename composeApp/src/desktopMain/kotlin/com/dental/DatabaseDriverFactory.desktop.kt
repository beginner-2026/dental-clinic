package com.dental.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.dental.data.db.DentalDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val userHome = System.getProperty("user.home").replace('\\', '/')
        val dir = File(userHome, ".dental-clinic")
        dir.mkdirs()
        val dbFile = File(dir, "dental.db")
        val path = dbFile.absolutePath.replace('\\', '/')

        val isNew = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:$path")

        if (isNew) {
            DentalDatabase.Schema.create(driver)
        }

        return driver
    }
}