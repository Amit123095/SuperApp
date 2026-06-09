package com.amit.application.UI_screen.VideoPlayer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.CircleShape

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun AdvancedVideoPlayerScreen(
    videoUriString: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope() // Required for the snapshot button

    // --- 1. EXO-PLAYER ENGINE SETUP ---
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true // Handles Audio Focus (Pauses when headphones disconnect)
            )
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUriString))

                // Advanced Configurations
                skipSilenceEnabled = true
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioLanguage("en")
                    .build()

                prepare()
                playWhenReady = true
            }
    }

    // --- 2. LIFECYCLE & PiP (PICTURE-IN-PICTURE) ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Enter PiP automatically if user leaves the app while playing
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
                        exoPlayer.isPlaying
                    ) {

                        val params = PictureInPictureParams.Builder().build()
                        activity?.enterPictureInPictureMode(params)
                    } else {
                        exoPlayer.pause()
                    }
                }

                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release() // CRITICAL: Prevents memory leaks
        }
    }

    // --- 3. SYSTEM SERVICES FOR GESTURES ---
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val window = activity?.window

    var currentVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var currentBrightness by remember {
        mutableFloatStateOf(
            window?.attributes?.screenBrightness ?: 0.5f
        )
    }

    var showVolumeUI by remember { mutableStateOf(false) }
    var showBrightnessUI by remember { mutableStateOf(false) }

    // Auto-hide the Volume/Brightness UI after 1.5 seconds
    LaunchedEffect(showVolumeUI, showBrightnessUI) {
        if (showVolumeUI || showBrightnessUI) {
            delay(1500)
            showVolumeUI = false
            showBrightnessUI = false
        }
    }

    // --- 4. RENDER UI WITH GESTURE OVERLAY ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                var isLeftHalf = false

                detectDragGestures(
                    onDragStart = { offset ->
                        // Check if drag started on left (brightness) or right (volume)
                        isLeftHalf = offset.x < size.width / 2f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dragSensitivity = 0.01f

                        if (isLeftHalf) {
                            // --- BRIGHTNESS (Left Side) ---
                            showBrightnessUI = true
                            showVolumeUI = false

                            // Negative dragAmount.y means swiping UP
                            currentBrightness = min(
                                1f,
                                max(0f, currentBrightness - (dragAmount.y * dragSensitivity / 100f))
                            )

                            window?.attributes = window?.attributes?.apply {
                                screenBrightness = currentBrightness
                            }
                        } else {
                            // --- VOLUME (Right Side) ---
                            showVolumeUI = true
                            showBrightnessUI = false

                            val volumeChange = -(dragAmount.y * dragSensitivity)
                            val newVolume =
                                min(maxVolume, max(0, currentVolume + volumeChange.toInt()))

                            if (newVolume != currentVolume) {
                                currentVolume = newVolume
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    currentVolume,
                                    0
                                )
                            }
                        }
                    }
                )
            }
    ) {

        // --- 4a. THE VIDEO PLAYER ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowFastForwardButton(true) // Double-tap to seek native support
                    setShowRewindButton(true)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- 4b. BRIGHTNESS OVERLAY INDICATOR ---
        if (showBrightnessUI) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.BrightnessMedium,
                    contentDescription = "Brightness",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${(currentBrightness * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // --- 4c. VOLUME OVERLAY INDICATOR ---
        if (showVolumeUI) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${(currentVolume * 100 / maxVolume)}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // --- 4d. SNAPSHOT CAMERA BUTTON ---
        FloatingActionButton(
            onClick = {
                val currentPositionMs = exoPlayer.currentPosition
                val videoUri = Uri.parse(videoUriString)
                coroutineScope.launch {
                    captureAndSaveFrame(context, videoUri, currentPositionMs)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", tint = Color.White)
        }

        // --- 4e. BACK BUTTON OVERLAY ---
        androidx.compose.material3.IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart) // Top Left
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

// --- 5. THE SNAPSHOT CAPTURE LOGIC ---
suspend fun captureAndSaveFrame(context: Context, videoUri: Uri, positionMs: Long) {
    withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)

            // Extract frame at exact microsecond
            val bitmap = retriever.getFrameAtTime(
                positionMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            retriever.release()

            if (bitmap != null) {
                // Save to Android Gallery
                val contentValues = ContentValues().apply {
                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        "Snapshot_${System.currentTimeMillis()}.jpg"
                    )
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SuperApp")
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Snapshot saved to gallery!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to capture snapshot", Toast.LENGTH_SHORT).show()
            }
        }
    }
}