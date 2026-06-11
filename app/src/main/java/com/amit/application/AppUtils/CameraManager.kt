package com.amit.application.AppUtils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class CameraManager(private val context: Context) {

    // Generates a temp local file provider URI for the camera intent to write raw images into
    fun createTempImageUri(): Pair<File, Uri> {
        val tempDirectory = File(context.cacheDir, "camera_captures").apply { mkdirs() }
        val file = File.createTempFile("CAM_${System.currentTimeMillis()}_", ".jpg", tempDirectory)

        // Match the authority with your AndroidManifest.xml provider entry
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        return Pair(file, uri)
    }
}