package com.amit.application.AppUtils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording(): File? {
        try {
            // Create a temporary cache file for the voice note
            val tempDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            currentFile = File.createTempFile("VOICE_${System.currentTimeMillis()}_", ".m4a", tempDir)

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // .m4a format (Standard for chat apps)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentFile?.absolutePath)
                prepare()
                start()
            }
            return currentFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // This catches the "stop called too fast" exception
            currentFile?.delete() // Delete the corrupted/empty file
            e.printStackTrace()
        } finally {
            recorder = null
        }
    }
}