package com.crozzers.postboxgo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Setting {
    val COLOUR_SCHEME = stringPreferencesKey("colour_scheme")
}

fun <T> setSetting(settings: DataStore<Preferences>, key: Preferences.Key<T>): (v: T) -> Unit {
    return { value ->
        CoroutineScope(Dispatchers.IO).launch {
            settings.edit { preferences ->
                preferences[key] = value
            }
        }
    }
}

val Context.settings by preferencesDataStore("settings")