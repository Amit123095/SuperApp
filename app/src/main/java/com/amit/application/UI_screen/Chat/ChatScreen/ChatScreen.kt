package com.amit.application.UI_screen.Chat.ChatScreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.amit.application.AppUtils.AudioRecorderManager
import com.amit.application.AppUtils.CameraManager
import com.amit.application.AppUtils.LocationManager
import com.amit.application.AppUtils.SuperAppFileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

val DarkTopBar = Color(0xFF1F2C34)
val DarkBackground = Color(0xFF121B22)
val BubbleMe = Color(0xFF005C4B)
val BubbleFriend = Color(0xFF202C33)
val DarkInputBackground = Color(0xFF2A3942)
val WhatsAppGreen = Color(0xFF00A884)
val CheckBlue = Color(0xFF53BDEB)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    friendName: String,
    friendNumber: String,
    messages: List<ChatMessage>,
    isFriendTyping: Boolean = false,
    onTypingStateChange: (Boolean) -> Unit = {},
    onSendMessage: (String, AttachmentType, String?, String?, String?) -> Unit,
    onBackClick: () -> Unit,
    onWalletClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showCameraDialog by remember { mutableStateOf(false) }

    var isTyping by remember { mutableStateOf(false) }
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            if (!isTyping) {
                isTyping = true
                onTypingStateChange(true)
            }
            delay(2000)
            isTyping = false
            onTypingStateChange(false)
        } else {
            if (isTyping) {
                isTyping = false
                onTypingStateChange(false)
            }
        }
    }

    val cameraManager = remember { CameraManager(context) }
    val locationManager = remember { LocationManager(context) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val audioRecorder = remember { AudioRecorderManager(context) }
    var isRecording by remember { mutableStateOf(false) }
    var tempAudioFile by remember { mutableStateOf<File?>(null) }

    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var contextMenuMessageId by remember { mutableStateOf<String?>(null) }

    // State to handle permanent permission denials
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    // --- LAUNCHERS ---
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Document Logic
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Gallery Logic
    }

    // PHOTO Launcher
    val cameraPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            onSendMessage("Captured Photo", AttachmentType.IMAGE, tempCameraUri.toString(), null, null)
        }
    }
    // VIDEO Launcher
    val cameraVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && tempCameraUri != null) {
            onSendMessage("Recorded Video", AttachmentType.VIDEO, tempCameraUri.toString(), null, null)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showCameraDialog = true
        else permissionDeniedMessage = "Camera access is needed to capture photos and videos."
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) permissionDeniedMessage = "Microphone access is needed to record voice notes."
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.values.any { it }
        if (!granted) permissionDeniedMessage = "Location access is needed to share your current location."
    }

    val groupedMessages = messages.groupBy { ChatDateFormatter.formatDateDivider(it.timestamp) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkTopBar, titleContentColor = Color.White, actionIconContentColor = Color.White, navigationIconContentColor = Color.White),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val initials = remember(friendName) {
                            if (friendName.isBlank()) "?" else {
                                val parts = friendName.trim().split("\\s+".toRegex())
                                if (parts.size >= 2) "${parts[0].first()}${parts[1].first()}".uppercase()
                                else parts[0].take(2).uppercase()
                            }
                        }
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF5A3546)),
                            contentAlignment = Alignment.Center
                        ) { Text(initials, color = Color(0xFFF48FB1), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(friendName, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (isFriendTyping) Text("typing...", fontSize = 13.sp, color = WhatsAppGreen, fontStyle = FontStyle.Italic)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    // TOP BAR ICONS ACTION
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$friendNumber"))
                        context.startActivity(intent)
                    }) { Icon(Icons.Default.Videocam, "Video Call") }

                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$friendNumber"))
                        context.startActivity(intent)
                    }) { Icon(Icons.Default.Call, "Call") }

                    IconButton(onClick = {
                        Toast.makeText(context, "Will come soon", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.MoreVert, "More") }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
                // Reply Dock
                replyingToMessage?.let { replySource ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .background(DarkInputBackground, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .drawBehind { drawLine(color = WhatsAppGreen, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 12f) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = if(replySource.isFromMe) "You" else friendName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                            Text(text = replySource.text, fontSize = 14.sp, color = Color.LightGray, maxLines = 1)
                        }
                        IconButton(onClick = { replyingToMessage = null }) { Icon(Icons.Default.Close, contentDescription = "Cancel Reply", tint = Color.Gray) }
                    }
                }

                ChatBottomBar(
                    text = messageText,
                    isRecording = isRecording,
                    onTextChange = { messageText = it },
                    onAttachClick = { showAttachmentMenu = true },
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText, AttachmentType.NONE, null, replyingToMessage?.id, replyingToMessage?.text)
                            messageText = ""
                            replyingToMessage = null
                        }
                    },
                    onStartRecording = {
                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            tempAudioFile = audioRecorder.startRecording()
                            if (tempAudioFile != null) isRecording = true
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopRecording = {
                        if (isRecording) {
                            audioRecorder.stopRecording()
                            isRecording = false
                            tempAudioFile?.let { file ->
                                coroutineScope.launch {
                                    val savedUri = SuperAppFileManager.saveFileFromUri(context, Uri.fromFile(file), file.name, SuperAppFileManager.Category.AUDIO)
                                    savedUri?.let { onSendMessage("Voice Note", AttachmentType.AUDIO, it.toString(), null, null) }
                                }
                            }
                        }
                    },
                    onWalletClick = onWalletClick,
                    onCameraClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) showCameraDialog = true else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1534081333815-ae5019106622?q=80&w=2000&auto=format&fit=crop",
                contentDescription = "Chat Background", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.5f
            )

            LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
                groupedMessages.forEach { (dateString, messagesForDate) ->
                    items(messagesForDate) { msg ->
                        MessageBubble(
                            message = msg, friendName = friendName, showMenu = contextMenuMessageId == msg.id,
                            onMenuToggle = { show -> contextMenuMessageId = if (show) msg.id else null },
                            onSwipeToReply = { selectedMessage -> replyingToMessage = selectedMessage }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    item { DateDivider(dateString) }
                }
            }
        }

        if (showAttachmentMenu) {
            AttachmentBottomSheet(
                onDismiss = { showAttachmentMenu = false },
                onDocumentClick = { fileLauncher.launch("*/*") },
                onCameraClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) showCameraDialog = true else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onGalleryClick = { galleryLauncher.launch("image/*") },
                onLocationClick = { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
            )
        }

        // --- CAMERA SELECTION DIALOG ---
        if (showCameraDialog) {
            AlertDialog(
                onDismissRequest = { showCameraDialog = false },
                title = { Text("Camera") },
                text = { Text("Do you want to take a photo or record a video?") },
                confirmButton = {
                    TextButton(onClick = {
                        showCameraDialog = false
                        val (file, uri) = cameraManager.createTempImageUri()
                        tempCameraFile = file
                        tempCameraUri = uri
                        cameraPhotoLauncher.launch(uri)
                    }) { Text("Photo", color = WhatsAppGreen) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCameraDialog = false
                        val (file, uri) = cameraManager.createTempImageUri() // Works for video temp files too
                        tempCameraFile = file
                        tempCameraUri = uri
                        cameraVideoLauncher.launch(uri)
                    }) { Text("Video", color = WhatsAppGreen) }
                },
                containerColor = DarkTopBar,
                titleContentColor = Color.White,
                textContentColor = Color.LightGray
            )
        }

        // --- PERMISSION RECOVERY DIALOG ---
        permissionDeniedMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { permissionDeniedMessage = null },
                title = { Text("Permission Required") },
                text = { Text("$message Please enable it in App Settings to use this feature.") },
                confirmButton = {
                    TextButton(onClick = {
                        permissionDeniedMessage = null
                        // Safely launch Android App Settings
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) { Text("Open Settings", color = WhatsAppGreen) }
                },
                dismissButton = {
                    TextButton(onClick = { permissionDeniedMessage = null }) { Text("Cancel", color = Color.Gray) }
                },
                containerColor = DarkTopBar,
                titleContentColor = Color.White,
                textContentColor = Color.LightGray
            )
        }
    }
}

// --- SUB-COMPOSABLES & COMPONENT VIEWS ---

@Composable
fun MessageBubble(message: ChatMessage, friendName: String, showMenu: Boolean, onMenuToggle: (Boolean) -> Unit, onSwipeToReply: (ChatMessage) -> Unit) {
    val isMe = message.isFromMe
    val context = LocalContext.current
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isMe) BubbleMe else BubbleFriend
    val bubbleShape = if (isMe) RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
    else RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)

    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipeThreshold = with(density) { 60.dp.toPx() }

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX.value > swipeThreshold) onSwipeToReply(message)
                        scope.launch { offsetX.animateTo(0f) }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 0 || offsetX.value > 0) scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceAtLeast(0f)) }
                    }
                )
            },
        contentAlignment = alignment
    ) {
        if (offsetX.value > 10f) {
            Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply", tint = Color.LightGray, modifier = Modifier.size(24.dp).background(Color.Black.copy(0.3f), CircleShape).padding(4.dp))
            }
        }

        Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value }) {
            Surface(
                color = backgroundColor, shape = bubbleShape, shadowElevation = 1.dp,
                modifier = Modifier.widthIn(min = 80.dp, max = 320.dp).pointerInput(Unit) { detectTapGestures(onLongPress = { onMenuToggle(true) }) }
            ) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {

                    if (message.repliedToText != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .drawBehind { drawLine(color = Color(0xFFB066FF), start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 10f) }
                                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
                        ) {
                            Column {
                                Text(text = if (isMe) friendName else "You", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB066FF))
                                Text(text = message.repliedToText, fontSize = 14.sp, color = Color.White.copy(0.8f), maxLines = 2)
                            }
                        }
                    }

                    // --- ATTACHMENTS (Images & Audio Player) ---
                    if (message.attachmentType == AttachmentType.IMAGE && message.attachmentUri != null) {
                        AsyncImage(
                            model = message.attachmentUri, contentDescription = "Attached Image", contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).padding(bottom = 4.dp)
                        )
                    } else if (message.attachmentType == AttachmentType.AUDIO && message.attachmentUri != null) {
                        // NATIVE AUDIO PLAYER UI
                        var isPlaying by remember { mutableStateOf(false) }
                        val context = LocalContext.current
                        val mediaPlayer = remember { MediaPlayer() }

                        DisposableEffect(Unit) { onDispose { mediaPlayer.release() } }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            IconButton(onClick = {
                                if (isPlaying) {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    try {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(context, Uri.parse(message.attachmentUri))
                                        mediaPlayer.prepare()
                                        mediaPlayer.start()
                                        isPlaying = true
                                        mediaPlayer.setOnCompletionListener { isPlaying = false }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Audio file missing", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // A simple mockup waveform
                            Box(modifier = Modifier.height(2.dp).weight(1f).background(Color.Gray)) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(if(isPlaying) 0.5f else 0f).background(WhatsAppGreen))
                            }
                        }
                    }

                    // Text Content
                    val hideAutoText = message.text in listOf("Captured Photo", "Sent a photo", "Voice Note", "Recorded Video")
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.wrapContentWidth()) {
                        if (!hideAutoText && message.text.isNotBlank()) {
                            Text(text = message.text, fontSize = 16.sp, color = Color.White, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
                        } else {
                            Spacer(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text(text = ChatDateFormatter.formatTime(message.timestamp), fontSize = 11.sp, color = Color.White.copy(0.6f))
                            if (isMe) {
                                Spacer(modifier = Modifier.width(4.dp))
                                val (icon, tint) = when (message.status) {
                                    MessageStatus.PENDING -> Icons.Default.Check to Color.Gray
                                    MessageStatus.SENT -> Icons.Default.Check to Color.Gray
                                    MessageStatus.DELIVERED -> Icons.Default.CheckCircle to Color.Gray
                                    MessageStatus.READ -> Icons.Default.CheckCircle to CheckBlue
                                }
                                Icon(icon, contentDescription = "Status", tint = tint, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { onMenuToggle(false) }, modifier = Modifier.background(DarkTopBar, RoundedCornerShape(8.dp))) {
                DropdownMenuItem(text = { Text("Info", color = Color.White) }, onClick = { onMenuToggle(false) })
                DropdownMenuItem(text = { Text("Copy", color = Color.White) }, onClick = { onMenuToggle(false) })
                DropdownMenuItem(text = { Text("Edit", color = Color.White) }, onClick = { onMenuToggle(false) })
                DropdownMenuItem(text = { Text("Pin", color = Color.White) }, onClick = { onMenuToggle(false) })
                DropdownMenuItem(text = { Text("Translate", color = Color.White) }, onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                    onMenuToggle(false) })
            }
        }
    }
}

@Composable
fun DateDivider(dateString: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(color = DarkInputBackground, shape = RoundedCornerShape(8.dp)) {
            Text(text = dateString, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun ChatBottomBar(
    text: String, isRecording: Boolean, onTextChange: (String) -> Unit, onAttachClick: () -> Unit,
    onSendClick: () -> Unit, onStartRecording: () -> Unit, onStopRecording: () -> Unit,
    onWalletClick: () -> Unit, onCameraClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
        Surface(shape = RoundedCornerShape(24.dp), color = DarkInputBackground, modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = { /* Emoji */ }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.EmojiEmotions, "Emoji", tint = Color.Gray) }

                TextField(
                    value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f),
                    placeholder = { Text("Message", color = Color.Gray, fontSize = 16.sp) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = WhatsAppGreen)
                )

                IconButton(onClick = onAttachClick, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.AttachFile, "Attach", tint = Color.Gray, modifier = Modifier.graphicsLayer { rotationZ = -45f }) }
                IconButton(onClick = onWalletClick, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.CurrencyRupee, "Pay", tint = Color.Gray) }
                IconButton(onClick = onCameraClick, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.CameraAlt, "Camera", tint = Color.Gray) }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        val isTextEmpty = text.isBlank()
        Box(
            modifier = Modifier.size(48.dp).background(WhatsAppGreen, CircleShape)
                .pointerInput(isTextEmpty) {
                    detectTapGestures(
                        onPress = { if (isTextEmpty) { onStartRecording(); tryAwaitRelease(); onStopRecording() } },
                        onTap = { if (!isTextEmpty) onSendClick() }
                    )
                },
            contentAlignment = Alignment.Center
        ) { Icon(if (isTextEmpty) Icons.Default.Mic else Icons.Default.Send, "Action", tint = Color.Black) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit, onDocumentClick: () -> Unit, onCameraClick: () -> Unit, onGalleryClick: () -> Unit, onLocationClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkTopBar) {
        Column(modifier = Modifier.padding(24.dp)) {
            // RESTORED ICONS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentIcon(Icons.Default.InsertDriveFile, "Document", Color(0xFF5F66CD)) { onDismiss(); onDocumentClick() }
                AttachmentIcon(Icons.Default.CameraAlt, "Camera", Color(0xFFD3396D)) { onDismiss(); onCameraClick() }
                AttachmentIcon(Icons.Default.Photo, "Gallery", Color(0xFFAC44CF)) { onDismiss(); onGalleryClick() }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentIcon(Icons.Default.LocationOn, "Location", Color(0xFF1DAA61)) { onDismiss(); onLocationClick() }
                AttachmentIcon(Icons.Default.Headset, "Audio", Color(0xFFE9592A)) { onDismiss() /* Audio picker */ }
                AttachmentIcon(Icons.Default.Person, "Contact", Color(0xFF00A5F4)) { onDismiss() /* Contact logic */ }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

