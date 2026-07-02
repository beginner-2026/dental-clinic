package com.dental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dental.data.DatabaseDriverFactory
import com.dental.data.sync.BackupStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BackupStorage.init(applicationContext)
        setContent {
            App(driverFactory = DatabaseDriverFactory(applicationContext))
        }
    }
}
