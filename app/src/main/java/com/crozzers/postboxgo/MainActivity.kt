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
import com.crozzers.postboxgo.ui.views.EditPostbox
import com.crozzers.postboxgo.ui.views.ListView
import com.crozzers.postboxgo.ui.views.SettingsView
import com.crozzers.postboxgo.utils.removeStaleCachedPostboxData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    lateinit var saveFile: SaveFile
        private set

    override fun onCreate() {
        super.onCreate()
        saveFile = SaveFile(this)

        CoroutineScope(Dispatchers.IO).launch {
            removeStaleCachedPostboxData(applicationContext)
        }
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
                        destination.route == Routes.ListView.route || destination.route == Routes.MapView.route
                    listViewSelected = destination.route == Routes.ListView.route
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(), topBar = {
                        TopBar(navController)
                    },
                    bottomBar = { BottomBar(navController, visible, listViewSelected) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.ListView.route,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        composable(Routes.ListView.route) {
                            ListView(
                                (applicationContext as App).saveFile.getPostboxes()
                            ) { p -> navController.navigate("${Routes.ViewPostbox.route}/${p.id}") }
                        }
                        composable(Routes.MapView.route) {
                            PostboxMap(
                                (applicationContext as App).saveFile.getPostboxes().values,
                                Modifier.fillMaxSize(),
                                onPostboxClick = { p -> navController.navigate("${Routes.ViewPostbox.route}/${p.id}") }
                            )
                        }
                        composable(Routes.AddPostbox.route) {
                            AddPostbox(locationClient, (applicationContext as App).saveFile, { p ->
                                (applicationContext as App).saveFile.addPostbox(p)
                                navController.navigate(Routes.ListView.route)
                            })
                        }
                        composable("${Routes.ViewPostbox.route}/{id}") {
                            val postbox = (applicationContext as App).saveFile.getPostbox(
                                it.arguments?.getString("id") ?: ""
                            )
                            if (postbox == null) {
                                navController.navigate(Routes.ListView.route)
                                return@composable
                            }
                            DetailsView(
                                postbox,
                                (applicationContext as App).saveFile
                            ) { navController.navigate(Routes.ListView.route) }
                        }
                        composable("${Routes.EditPostbox.route}/{id}") {
                            val saveFile = (applicationContext as App).saveFile
                            val postbox = saveFile.getPostbox(
                                it.arguments?.getString("id") ?: ""
                            )
                            if (postbox == null) {
                                navController.navigate(Routes.ListView.route)
                                return@composable
                            }
                            EditPostbox(
                                locationClient,
                                postbox,
                            ) {
                                saveFile.save()
                                navController.navigate(Routes.ListView.route)
                            }
                        }
                        composable(Routes.Settings.route) {
                            SettingsView((applicationContext as App).saveFile)
                        }
                    }
                }
            }
        }
    }
}

enum class Routes(val route: String, val displayName: String) {
    ListView("list_view", "List View"),
    MapView("map_view", "Map View"),
    AddPostbox("add_postbox", "Add Postbox"),
    ViewPostbox("view_postbox", "Postbox Details"),
    EditPostbox("edit_postbox", "Edit Postbox"),
    Settings("settings", "Settings"),
}