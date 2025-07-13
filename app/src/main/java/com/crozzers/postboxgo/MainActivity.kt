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
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crozzers.postboxgo.ui.theme.PostboxGOTheme
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(), topBar = {
                        MenuBar(navController)
                    }) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavigationItem.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavigationItem.Home.route) {
                            PostboxListScreen(
                                (applicationContext as App).saveFile,
                                Modifier.padding(innerPadding),
                                { p -> navController.navigate("${NavigationItem.ViewPostbox.route}/${p.id}") }
                            )
                        }
                        composable(NavigationItem.AddPostbox.route) {
                            AddPostbox(locationClient, { p ->
                                (applicationContext as App).saveFile.addPostbox(p);
                                navController.navigate(NavigationItem.Home.route)
                            })
                        }
                        composable("${NavigationItem.ViewPostbox.route}/{id}") {
                            val postbox = (applicationContext as App).saveFile.getPostbox(
                                it.arguments?.getString("id") ?: ""
                            )
                            if (postbox == null) {
                                navController.navigate(NavigationItem.Home.route)
                                return@composable
                            }
                            ViewPostbox(
                                postbox,
                                (applicationContext as App).saveFile
                            ) { navController.navigate(NavigationItem.Home.route) }
                        }
                    }
                }
            }
        }
    }
}


enum class Screen {
    HOME, ADDPOSTBOX, VIEWPOSTBOX
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(Screen.HOME.name)
    object AddPostbox : NavigationItem(Screen.ADDPOSTBOX.name)
    object ViewPostbox : NavigationItem(Screen.VIEWPOSTBOX.name)
}