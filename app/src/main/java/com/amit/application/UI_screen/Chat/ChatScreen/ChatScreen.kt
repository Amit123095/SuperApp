package com.amit.application.UI_screen.Chat.ChatScreen

import android.Manifest
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.amit.application.AppUtils.CameraManager
import com.amit.application.AppUtils.LocationManager
import com.amit.application.AppUtils.SuperAppFileManager
import kotlinx.coroutines.launch
import java.io.File
import com.amit.application.AppUtils.AudioRecorderManager
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSendMessage: (String, AttachmentType, String?) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Initialize our hardware managers locally
    val cameraManager = remember { CameraManager(context) }
    val locationManager = remember { LocationManager(context) }

    // State to track custom file paths for camera captures
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Add Audio Manager and Recording State
    val audioRecorder = remember { AudioRecorderManager(context) }
    var isRecording by remember { mutableStateOf(false) }
    var tempAudioFile by remember { mutableStateOf<File?>(null) }

    // --- FUNCTIONAL LAUNCHERS ---

    // 1. Document/File Picker Launcher (< 200MB validation)
    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sourceUri ->
                val fileDetails = getFileDetails(context, sourceUri)
                val sizeInMb = fileDetails.second / (1024 * 1024)

                if (sizeInMb > 200) {
                    Toast.makeText(
                        context,
                        "File exceeds 200MB! Upload to Google Drive and paste the link.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    coroutineScope.launch {
                        val savedUri = SuperAppFileManager.saveFileFromUri(
                            context,
                            sourceUri,
                            fileDetails.first,
                            SuperAppFileManager.Category.DOCUMENT
                        )
                        savedUri?.let {
                            onSendMessage(
                                "Shared a document: ${fileDetails.first}",
                                AttachmentType.DOCUMENT,
                                it.toString()
                            )
                        }
                    }
                }
            }
        }

    // 2. Gallery Picker Launcher (Images/Videos)
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sourceUri ->
                coroutineScope.launch {
                    val savedUri = SuperAppFileManager.saveFileFromUri(
                        context,
                        sourceUri,
                        "IMG_${System.currentTimeMillis()}.jpg",
                        SuperAppFileManager.Category.IMAGE
                    )
                    savedUri?.let {
                        onSendMessage(
                            "Sent a photo",
                            AttachmentType.IMAGE,
                            it.toString()
                        )
                    }
                }
            }
        }

    // 3. Camera Intent Launcher
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempCameraUri != null) {
                onSendMessage("Captured Photo", AttachmentType.IMAGE, tempCameraUri.toString())
            }
        }

    // Permission check request for Camera
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                val (file, uri) = cameraManager.createTempImageUri()
                tempCameraFile = file
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // 4. GPS Location Extraction Engine
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineGranted || coarseGranted) {
                coroutineScope.launch {
                    val loc = locationManager.getCurrentLocation()
                    if (loc != null) {
                        val locMessage =
                            "📍 Current Location: https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                        onSendMessage(locMessage, AttachmentType.LOCATION, null)
                    } else {
                        Toast.makeText(
                            context,
                            "Unable to pinpoint exact location.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    context,
                    "Location permission required to share location.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // 5. Add Audio Permission Launcher
    val audioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    context,
                    "Microphone permission is required for voice notes.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Grouping strategy
    val groupedMessages = messages.groupBy { ChatDateFormatter.formatDateDivider(it.timestamp) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friend's Chat") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            ChatBottomBar(
                text = messageText,
                isRecording = isRecording,
                onTextChange = { messageText = it },
                onAttachClick = { showAttachmentMenu = true },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText, AttachmentType.NONE, null)
                        messageText = ""
                    }
                },
                // Pass Mic Hold logic
                onStartRecording = {
                    // Check if we ALREADY have permission
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        // Safe to record immediately
                        tempAudioFile = audioRecorder.startRecording()
                        if (tempAudioFile != null) isRecording = true
                    } else {
                        // Ask for permission first (won't record this tap, user must tap again after granting)
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                // Pass Mic Release logic
                onStopRecording = {
                    if (isRecording) {
                        audioRecorder.stopRecording()
                        isRecording = false

                        // Save and Send the Audio File
                        tempAudioFile?.let { file ->
                            coroutineScope.launch {
                                val savedUri = SuperAppFileManager.saveFileFromUri(
                                    context,
                                    Uri.fromFile(file),
                                    file.name,
                                    SuperAppFileManager.Category.AUDIO
                                )
                                savedUri?.let {
                                    onSendMessage("Voice Note", AttachmentType.AUDIO, it.toString())
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFEFE6DD)),
            reverseLayout = true,
            contentPadding = PaddingValues(16.dp)
        ) {
            groupedMessages.forEach { (dateString, messagesForDate) ->
                items(messagesForDate) { msg ->
                    MessageBubble(msg)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    DateDivider(dateString)
                }
            }
        }

        if (showAttachmentMenu) {
            AttachmentBottomSheet(
                onDismiss = { showAttachmentMenu = false },
                onDocumentClick = { fileLauncher.launch("*/*") },
                onCameraClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                onGalleryClick = { galleryLauncher.launch("image/*") },
                onLocationClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
    }
}

// --- SUB-COMPOSABLES & COMPONENT VIEWS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onDocumentClick: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onLocationClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentIcon(
                    Icons.Default.InsertDriveFile,
                    "Document",
                    Color(0xFF5F66CD)
                ) { onDismiss(); onDocumentClick() }
                AttachmentIcon(
                    Icons.Default.CameraAlt,
                    "Camera",
                    Color(0xFFD3396D)
                ) { onDismiss(); onCameraClick() }
                AttachmentIcon(
                    Icons.Default.Photo,
                    "Gallery",
                    Color(0xFFAC44CF)
                ) { onDismiss(); onGalleryClick() }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentIcon(
                    Icons.Default.LocationOn,
                    "Location",
                    Color(0xFF1DAA61)
                ) { onDismiss(); onLocationClick() }
                AttachmentIcon(
                    Icons.Default.Headset,
                    "Audio",
                    Color(0xFFE9592A)
                ) { /* Audio logic */ }
                AttachmentIcon(
                    Icons.Default.Person,
                    "Contact",
                    Color(0xFF00A5F4)
                ) { /* Contact logic */ }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isFromMe) Color(0xFFE7FFDB) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(min = 80.dp, max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {

                // --- 1. DYNAMIC MEDIA RENDERER ---
                when (message.attachmentType) {
                    AttachmentType.IMAGE -> {
                        message.attachmentUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Attached Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(bottom = 4.dp)
                            )
                        }
                    }

                    AttachmentType.DOCUMENT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.Black.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                tint = Color(0xFF5F66CD),
                                contentDescription = "Document",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Document Attached",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    }

                    AttachmentType.LOCATION -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE7F6EC), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                tint = Color(0xFF1DAA61),
                                contentDescription = "Location",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Location Pinned",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1DAA61)
                            )
                        }
                    }

                    AttachmentType.AUDIO -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFFFE0B2),
                                    RoundedCornerShape(8.dp)
                                ) // Light Orange background
                                .padding(8.dp)
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                tint = Color(0xFFE65100),
                                contentDescription = "Voice Note",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Voice Note",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            // In the future, you can add a Play/Pause button and an ExoPlayer slider here!
                        }
                    }

                    else -> {} // Do nothing for NONE
                }

                // --- 2. TEXT RENDERING (Captions or Standard Text) ---
                // We only show the text if it's a normal message, or a custom typed caption.
                // We hide the ugly auto-generated texts like "Sent a photo"
                val hideAutoText = message.text in listOf(
                    "Captured Photo",
                    "Sent a photo"
                ) || message.text.startsWith("📍 Current Location") || message.text.startsWith("Shared a document")

                if (message.text.isNotBlank() && !hideAutoText) {
                    Text(
                        text = message.text,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // --- 3. TIME & STATUS ROW ---
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ChatDateFormatter.formatTime(message.timestamp),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (message.isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val (icon, tint) = when (message.status) {
                            MessageStatus.PENDING -> Icons.Default.CheckCircle to Color.LightGray
                            MessageStatus.SENT -> Icons.Default.Check to Color.Gray
                            MessageStatus.DELIVERED -> Icons.Default.CheckCircle to Color.Gray
                            MessageStatus.READ -> Icons.Default.CheckCircle to Color(0xFF34B7F1) // WhatsApp Blue
                        }
                        Icon(
                            icon,
                            contentDescription = "Status",
                            tint = tint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateDivider(dateString: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(color = Color(0xFFD6EAF8), shape = RoundedCornerShape(8.dp)) {
            Text(
                text = dateString,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun ChatBottomBar(
    text: String,
    isRecording: Boolean,
    onTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp), verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.weight(1f),
            shadowElevation = 2.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAttachClick) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.Gray)
                }
                // Show "Recording..." if holding mic, otherwise show TextField
                if (isRecording) {
                    Text(
                        text = "🎙️ Recording... Release to send",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    )
                } else {
                    TextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Dynamic Button: Send Icon if text exists, Mic Icon if empty
        val isTextEmpty = text.isBlank()

        Box(
            modifier = Modifier
                .size(50.dp) // Standard FAB size
                .background(Color(0xFF00897B), CircleShape)
                .pointerInput(isTextEmpty) { // Re-evaluate pointer input if text state changes
                    if (isTextEmpty) {
                        detectTapGestures(
                            onPress = {
                                onStartRecording()
                                tryAwaitRelease() // Waits until the user lifts their finger
                                onStopRecording()
                            }
                        )
                    }
                }
                .clickable(enabled = !isTextEmpty) {
                    // Only clickable traditionally if it's the Send button
                    if (!isTextEmpty) onSendClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTextEmpty) Icons.Default.Mic else Icons.Default.Send,
                contentDescription = if (isTextEmpty) "Hold to Record" else "Send",
                tint = Color.White
            )
        }
    }
}

// --- EXTENSION HELPERS ---
private fun getFileDetails(context: Context, uri: Uri): Pair<String, Long> {
    var name = "Unknown_File"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) name = cursor.getString(nameIndex)
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }
    return Pair(name, size)
}