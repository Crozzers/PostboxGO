package com.crozzers.postboxgo

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crozzers.postboxgo.ui.components.BottomBar
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.ui.components.TopBar
import com.crozzers.postboxgo.ui.theme.PostboxGOTheme
import com.crozzers.postboxgo.ui.views.AddPostbox
import com.crozzers.postboxgo.ui.views.DetailsView
import com.crozzers.postboxgo.ui.views.ListView
import com.crozzers.postboxgo.ui.views.SettingsView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class App : Application() {
    lateinit var saveFile: SaveFile
        private set

    override fun onCreate() {
        super.onCreate()
        saveFile = SaveFile(this)

    }
}

class MainActivity : ComponentActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // quickly request location permission
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST
            )
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        enableEdgeToEdge()
        setContent {
            PostboxGOTheme {
                val navController = rememberNavController()
                var visible by remember { mutableStateOf(true) }
                var listViewSelected by remember { mutableStateOf(true) }

                navController.addOnDestinationChangedListener { _, destination, _ ->
                    visible =
                        destination.route == NavigationItem.ListView.route || destination.route == NavigationItem.MapView.route
                    listViewSelected = destination.route == NavigationItem.ListView.route
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(), topBar = {
                        TopBar(navController)
                    },
                    bottomBar = { BottomBar(navController, visible, listViewSelected) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavigationItem.ListView.route,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        composable(NavigationItem.ListView.route) {
                            ListView(
                                (applicationContext as App).saveFile.getPostboxes(),
                                { p -> navController.navigate("${NavigationItem.ViewPostbox.route}/${p.id}") }
                            )
                        }
                        composable(NavigationItem.MapView.route) {
                            PostboxMap(
                                (applicationContext as App).saveFile.getPostboxes().values,
                                Modifier.fillMaxSize(),
                                onPostboxClick = { p -> navController.navigate("${NavigationItem.ViewPostbox.route}/${p.id}") }
                            )
                        }
                        composable(NavigationItem.AddPostbox.route) {
                            AddPostbox(locationClient,(applicationContext as App).saveFile, { p ->
                                (applicationContext as App).saveFile.addPostbox(p);
                                navController.navigate(NavigationItem.ListView.route)
                            })
                        }
                        composable("${NavigationItem.ViewPostbox.route}/{id}") {
                            val postbox = (applicationContext as App).saveFile.getPostbox(
                                it.arguments?.getString("id") ?: ""
                            )
                            if (postbox == null) {
                                navController.navigate(NavigationItem.ListView.route)
                                return@composable
                            }
                            DetailsView(
                                postbox,
                                (applicationContext as App).saveFile
                            ) { navController.navigate(NavigationItem.ListView.route) }
                        }
                        composable(NavigationItem.Settings.route) {
                            SettingsView((applicationContext as App).saveFile)
                        }
                    }
                }
            }
        }
    }
}


enum class Screen {
    ListView, MAPVIEW, ADDPOSTBOX, VIEWPOSTBOX, SETTINGS
}

sealed class NavigationItem(val route: String) {
    object ListView : NavigationItem(Screen.ListView.name)
    object MapView : NavigationItem(Screen.MAPVIEW.name)
    object AddPostbox : NavigationItem(Screen.ADDPOSTBOX.name)
    object ViewPostbox : NavigationItem(Screen.VIEWPOSTBOX.name)
    object Settings : NavigationItem(Screen.SETTINGS.name)
}