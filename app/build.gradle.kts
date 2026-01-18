plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.10"
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.crozzers.postboxgo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.crozzers.postboxgo"
        minSdk = 26
        targetSdk = 36
        versionCode = 61
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    // keep nav-compose on 2.7.7 to fix LazyCol not recomposing correctly after deleting items
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation(libs.androidx.adaptive)
    val mapsComposeVersion = "6.6.0"
    implementation("com.google.maps.android:maps-compose:$mapsComposeVersion")
    implementation("com.google.maps.android:maps-compose-utils:$mapsComposeVersion")
    implementation("com.google.maps.android:maps-compose-widgets:$mapsComposeVersion")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.patrykandpatrick.vico:compose:2.2.0")
    implementation("com.patrykandpatrick.vico:compose-m3:2.2.0")


    // tests and dev
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(libs.androidx.rules)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}
