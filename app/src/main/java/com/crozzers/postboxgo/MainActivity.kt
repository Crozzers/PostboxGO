package com.crozzers.postboxgo

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowHeightSizeClass
import com.crozzers.postboxgo.ui.components.BottomBar
import com.crozzers.postboxgo.ui.components.NavRail
import com.crozzers.postboxgo.ui.components.PostboxMap
import com.crozzers.postboxgo.ui.components.TopBar
import com.crozzers.postboxgo.ui.theme.PostboxGOTheme
import com.crozzers.postboxgo.ui.views.AddPostbox
import com.crozzers.postboxgo.ui.views.DetailsView
import com.crozzers.postboxgo.ui.views.EditPostbox
import com.crozzers.postboxgo.ui.views.ListView
import com.crozzers.postboxgo.ui.views.SettingsView
import com.crozzers.postboxgo.ui.views.StatisticsView
import com.crozzers.postboxgo.utils.UpdateCheck
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

        createNotificationChannel()

        enableEdgeToEdge()
        setContent {
            PostboxGOTheme {
                val navController = rememberNavController()
                val compact =
                    currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopBar(navController) },
                    bottomBar = { if (!compact) BottomBar(navController) }
                ) { innerPadding ->
                    Row {
                        if (compact) {
                            Row(
                                Modifier.padding(
                                    // use top, left and right padding to make sure we don't put
                                    // anything behind the top bar, but ignore bottom padding to
                                    // extend into the gesture zone. Nav rail items are padded
                                    // extra anyway
                                    top = innerPadding.calculateTopPadding(),
                                    start = innerPadding.calculateLeftPadding(LayoutDirection.Ltr),
                                    end = innerPadding.calculateRightPadding(LayoutDirection.Ltr)
                                )
                            ) {
                                NavRail(navController)
                            }
                        }
                        NavHost(
                            navController = navController,
                            startDestination = Routes.ListView.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
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
                                    onPostboxClick = { p -> navController.navigate("${Routes.ViewPostbox.route}/${p.id}") },
                                    locationClient = locationClient
                                )
                            }
                            composable(Routes.AddPostbox.route) {
                                AddPostbox(
                                    locationClient,
                                    (applicationContext as App).saveFile,
                                    { p ->
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
                                    saveFile,
                                    postbox,
                                ) {
                                    saveFile.save()
                                    navController.navigate(Routes.ListView.route)
                                }
                            }
                            composable(Routes.Settings.route) {
                                SettingsView((applicationContext as App).saveFile)
                            }
                            composable(Routes.Stats.route) {
                                StatisticsView((applicationContext as App).saveFile)
                            }
                        }
                    }
                }
                UpdateCheck()
            }
        }
    }

    fun createNotificationChannel() {
        val name = "PostboxGO App Updates"
        val descriptionText = "Notifies if an app update is available"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("PBG_APP_UPDATES", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

enum class Routes(val route: String, val displayName: String, val icon: ImageVector) {
    ListView("list_view", "List View", Icons.AutoMirrored.Filled.List),
    MapView("map_view", "Map View", Icons.Filled.LocationOn),
    Stats("stats_view", "Statistics", Icons.Filled.Info),
    Settings("settings", "Settings", Icons.Filled.Settings),
    AddPostbox("add_postbox", "Register", Icons.Filled.Add),
    ViewPostbox("view_postbox", "Postbox Details", Icons.Filled.Info),
    EditPostbox("edit_postbox", "Edit Postbox", Icons.Filled.Edit),
}