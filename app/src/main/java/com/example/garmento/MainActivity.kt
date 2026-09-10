package com.example.garmento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.garmento.core.util.Constants
import com.example.garmento.navigation.NavGraph
import com.example.garmento.ui.theme.GarmentoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by dataStore.data.collectAsStateWithLifecycle(initialValue = null)
            val darkMode = prefs?.get(Constants.DARK_MODE_KEY) ?: false

            GarmentoTheme(darkModeEnabled = darkMode) {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}