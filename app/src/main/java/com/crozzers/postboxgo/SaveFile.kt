package com.crozzers.postboxgo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val LOG_TAG = "SaveFile"

class SaveFile(private val context: Context) {
    private var data: SaveDataV2
    private var fileName = "save.json"

    init {
        val file = File(context.filesDir, fileName)
        data = if (file.exists()) {
            try {
                decode(file.readText())
            } catch (e: SerializationException) {
                SaveDataV2(2, mutableMapOf<String, Postbox>())
            }
        } else {
            SaveDataV2(2, mutableMapOf<String, Postbox>())
        }
    }

    fun save() {
        val file = File(context.filesDir, fileName)
        file.writeText(Json.encodeToString(data))
    }

    fun getPostboxes(): MutableMap<String, Postbox> {
        return data.postboxes
    }

    fun getPostbox(id: String): Postbox? {
        return data.postboxes[id]
    }

    fun addPostbox(postbox: Postbox) {
        data.postboxes[postbox.id] = postbox
        save()
    }

    fun removePostbox(postbox: Postbox) {
        if (data.postboxes.remove(postbox.id) != null) {
            save()
        }
    }

    /**
     * Export save file to downloads
     */
    fun export() {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"))
        val filename = "postboxes_$now.json"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            if (uri != null) {
                try {
                    val outputStream = contentResolver.openOutputStream(uri)
                    outputStream?.write(Json.encodeToString(data).toByteArray())
                    outputStream?.close()
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Failed to export savefile (${Build.VERSION.SDK_INT})", e)
                    return
                }
            }
        } else {
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            try {
                file.writeText(Json.encodeToString(data))
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to export savefile (${Build.VERSION.SDK_INT})", e)
                return
            }
        }

        Toast.makeText(context, "Saved to Downloads folder", Toast.LENGTH_SHORT).show()
    }

    /**
     * Import and overwrite save file
     */
    fun import(uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val contents = inputStream?.bufferedReader()?.readText()
        if (contents == null) {
            Toast.makeText(
                context,
                "Imported savefile is empty. Ignoring...",
                Toast.LENGTH_SHORT
            ).show()
            Log.w(LOG_TAG, "Failed to import savefile - empty file")
        } else {
            try {
                data = decode(contents)
                save()  // save after to make sure changes apply
                Log.i(LOG_TAG, "Imported savefile $uri - ${getPostboxes().size}")
                Toast.makeText(context, "Imported savefile", Toast.LENGTH_SHORT).show()
            } catch (e: SerializationException) {
                Log.e(LOG_TAG, "Failed to import savefile", e)
                Toast.makeText(context, "Failed to import savefile", Toast.LENGTH_SHORT)
                    .show()
            }

        }
    }

    fun decode(contents: String): SaveDataV2 {
        val data = Json.decodeFromString<SaveData>(contents)
        return if (data.version == 1) {
            SaveDataV2(
                2,
                Json.decodeFromJsonElement<MutableList<Postbox>>(data.postboxes)
                    .associateBy { it.id }.toMutableMap()
            )
        } else {
            SaveDataV2(
                data.version,
                Json.decodeFromJsonElement<MutableMap<String, Postbox>>(data.postboxes)
            )
        }
    }
}

@Serializable
data class SaveData(val version: Int, val postboxes: JsonElement)

@Serializable
data class SaveDataV2(val version: Int, val postboxes: MutableMap<String, Postbox>)

@Serializable
data class SaveDataV1(val version: Int, val postboxes: MutableList<Postbox>)
