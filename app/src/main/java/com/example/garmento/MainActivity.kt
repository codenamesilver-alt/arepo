package com.example.garmento

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.garmento.core.util.Constants
import com.example.garmento.navigation.NavGraph
import com.example.garmento.navigation.Screen
import com.example.garmento.ui.theme.GarmentoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private var navControllerRef: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by dataStore.data.collectAsStateWithLifecycle(initialValue = null)
            val darkMode = prefs?.get(Constants.DARK_MODE_KEY) ?: false

            GarmentoTheme(darkModeEnabled = darkMode) {
                val navController = rememberNavController()
                navControllerRef = navController
                LaunchedEffect(Unit) {
                    handleDeepLink(intent, navController)
                }
                NavGraph(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navControllerRef?.let { handleDeepLink(intent, it) }
    }

    private fun handleDeepLink(intent: Intent?, navController: NavHostController) {
        val uri = intent?.data ?: return
        if (uri.scheme == "garmento" && uri.host == "invite") {
            val token = uri.getQueryParameter("token") ?: return
            navController.navigate(Screen.InviteAccept.createRoute(token))        }
    }
}