package com.crozzers.postboxgo

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.serialization.encodeToString

class SaveFile(private val context: Context) {
    private var data: SaveData
    private var fileName = "save.json"

    init {
        val file = File(context.filesDir, fileName)
        data = if (file.exists()) {
            try {
                Json.decodeFromString<SaveData>(file.readText())
            } catch (e: SerializationException) {
                SaveData(1, mutableListOf<Postbox>())
            }
        } else {
            SaveData(1, mutableListOf<Postbox>())
        }
    }

    fun save() {
        val file = File(context.filesDir, fileName)
        file.writeText(Json.encodeToString(data))
    }

    fun getPostboxes(): List<Postbox> {
        return data.postboxes
    }

    fun getPostbox(id: String): Postbox? {
        return data.postboxes.find { it.id == id }
    }

    fun addPostbox(postbox: Postbox) {
        data.postboxes.add(postbox)
        save()
    }

    fun removePostbox(postbox: Postbox) {
        if (data.postboxes.remove(postbox)) {
            save()
        }
    }
}

@Serializable
data class SaveData(val version: Int, val postboxes: MutableList<Postbox>)