package com.amit.application.AppUtils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object SuperAppFileManager {

    enum class Category(val folderName: String) {
        SCREENSHOT("Screenshots"),
        IMAGE("Images"),
        VIDEO("Videos"),
        DOCUMENT("Documents"),
        AUDIO("Audio")
    }

    // Save UI Bitmaps (Screenshots, Player Snapshots)
    suspend fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String, category: Category): Uri? = withContext(Dispatchers.IO) {
        val relativeLocation = "${Environment.DIRECTORY_DOWNLOADS}/SuperApp/${category.folderName}"
        var uri: Uri? = null
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Downloads.RELATIVE_PATH, relativeLocation)
                }
                val resolver = context.contentResolver
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { outputStream = resolver.openOutputStream(it) }
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(publicDir, "SuperApp/${category.folderName}").apply { mkdirs() }
                val file = File(appDir, fileName)
                outputStream = FileOutputStream(file)
                uri = Uri.fromFile(file)
            }

            outputStream?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Copy any generic attachment file (Documents, Audio) into the safe SuperApp structure
    suspend fun saveFileFromUri(context: Context, sourceUri: Uri, fileName: String, category: Category): Uri? = withContext(Dispatchers.IO) {
        val relativeLocation = "${Environment.DIRECTORY_DOWNLOADS}/SuperApp/${category.folderName}"
        var targetUri: Uri? = null
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourceUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.RELATIVE_PATH, relativeLocation)
                }
                targetUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                targetUri?.let { outputStream = context.contentResolver.openOutputStream(it) }
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(publicDir, "SuperApp/${category.folderName}").apply { mkdirs() }
                val file = File(appDir, fileName)
                outputStream = FileOutputStream(file)
                targetUri = Uri.fromFile(file)
            }

            if (inputStream != null && outputStream != null) {
                inputStream.copyTo(outputStream!!)
            }
            targetUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }
}