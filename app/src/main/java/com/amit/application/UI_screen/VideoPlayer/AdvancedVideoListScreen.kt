package com.amit.application.UI_screen.VideoPlayer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

// IMPORTANT: Notice we removed the VideoFrameDecoder import here!

// --- 1. SHIMMER EFFECT MODIFIER ---
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition()
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE0E0E0),
                Color(0xFFF5F5F5),
                Color(0xFFE0E0E0),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
}

// --- 2. THE UI SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedVideoListScreen(
    viewModel: LocalVideoViewModel = hiltViewModel(),
    onVideoClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasPermission = isGranted }

    LaunchedEffect(Unit) { permissionLauncher.launch(permissionToRequest) }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text("Gallery")
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }
    )
    { paddingValues ->
        if (hasPermission) {

            // Collect the Paged Data
            val pagedVideos = viewModel.getPagedVideos(context).collectAsLazyPagingItems()

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. SHOW SHIMMER WHILE INITIALLY LOADING
                if (pagedVideos.loadState.refresh is LoadState.Loading) {
                    items(24) { // Show 24 fake shimmer blocks
                        Box(modifier = Modifier
                            .aspectRatio(1f)
                            .shimmerEffect())
                    }
                }

                // 2. SHOW ACTUAL VIDEOS
                items(count = pagedVideos.itemCount) { index ->
                    val video = pagedVideos[index]
                    if (video != null) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onVideoClick(video.uri.toString()) }) {
                            AsyncImage(
                                // Notice how clean this is. Coil will automatically ask Android for the
                                // cached MediaStore thumbnail instead of extracting the video frame!
                                model = ImageRequest.Builder(context)
                                    .data(video.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = video.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // 3. SHOW BOTTOM LOADER WHEN SCROLLING DOWN (Appending more data)
                if (pagedVideos.loadState.append is LoadState.Loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Storage permission is required to view videos.")
            }
        }
    }
}