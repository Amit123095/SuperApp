package com.amit.application.UI_screen.VideoPlayer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.core.net.toUri

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUriString: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val videoUri = videoUriString.toUri()

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            // 1. Set the seek increments ON THE BUILDER first
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build() // 2. Build the player
            .apply { // 3. Then configure the actual player instance
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
            if (event == Lifecycle.Event.ON_RESUME) exoPlayer.play()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {

        // 1. The Media3 Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    // Shows the 10-second skip buttons on the UI
                    setShowFastForwardButton(true)
                    setShowRewindButton(true)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. The Custom Snapshot Overlay Button
        FloatingActionButton(
            onClick = {
                val currentPositionMs = exoPlayer.currentPosition
                coroutineScope.launch {
                    captureAndSaveFrame(context, videoUri, currentPositionMs)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp), // Kept away from the top edges
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", tint = Color.White)
        }
        // 3. ADD A FLOATING BACK BUTTON ON TOP OF THE VIDEO
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
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

// 3. The Core Snapshot Logic (Runs safely on a background thread)
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


/*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
@Composable
fun VideoPlayerScreen() {

    // State to hold the URI of the selected video
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

    // Modern Photo/Video Picker API (No permissions required!)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedVideoUri = uri
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                // Launch the picker filtering for videos only
                mediaPickerLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.VideoOnly
                    )
                )
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Select Video from Device")
        }
        // If a video is selected, render the player. Otherwise, show a placeholder.
        if (selectedVideoUri != null) {
            ExoPlayerView(videoUri = selectedVideoUri!!)
        } else {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No video selected")
            }
        }
    }
}
@Composable
fun ExoPlayerView(videoUri: Uri) {
    val context = LocalContext.current

    // 1. Initialize ExoPlayer safely
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true // Auto-play when ready
        }
    }

    // 2. Manage Lifecycle (CRUCIAL!)
    // DisposableEffect runs cleanup when this Composable leaves the composition (e.g., user hits back)
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 3. Render the legacy View inside Compose
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true // Show play/pause/timeline controls
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f) // Keep standard video aspect ratio
            .padding(16.dp)
    )
}*/
