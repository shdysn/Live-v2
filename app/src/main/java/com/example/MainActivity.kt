package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import pk.livecaster.app.LiveCasterApp
import pk.livecaster.app.core.di.AppContainer

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            appContainer = AppContainer(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize AppContainer", e)
            appContainer = AppContainer(applicationContext)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LiveCasterApp(appContainer = appContainer)
                }
            }
        }
    }
}
