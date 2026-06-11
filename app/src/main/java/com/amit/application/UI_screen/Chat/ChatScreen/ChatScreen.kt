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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amit.application.UI_screen.Chat.ChatScreen.AttachmentType
import com.amit.application.UI_screen.Chat.ChatScreen.ChatMessage
import com.amit.application.UI_screen.Chat.ChatScreen.MessageStatus
import com.amit.application.AppUtils.CameraManager
import com.amit.application.AppUtils.LocationManager
import com.amit.application.AppUtils.SuperAppFileManager
import com.amit.application.UI_screen.Chat.ChatScreen.ChatDateFormatter
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSendMessage: (String, AttachmentType, String?) -> Unit,
    onBackClick: () -> Unit
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

    // --- FUNCTIONAL LAUNCHERS ---

    // 1. Document/File Picker Launcher (< 200MB validation)
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sourceUri ->
            val fileDetails = getFileDetails(context, sourceUri)
            val sizeInMb = fileDetails.second / (1024 * 1024)

            if (sizeInMb > 200) {
                Toast.makeText(context, "File exceeds 200MB! Upload to Google Drive and paste the link.", Toast.LENGTH_LONG).show()
            } else {
                coroutineScope.launch {
                    val savedUri = SuperAppFileManager.saveFileFromUri(
                        context, sourceUri, fileDetails.first, SuperAppFileManager.Category.DOCUMENT
                    )
                    savedUri?.let { onSendMessage("Shared a document: ${fileDetails.first}", AttachmentType.DOCUMENT, it.toString()) }
                }
            }
        }
    }

    // 2. Gallery Picker Launcher (Images/Videos)
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch {
                val savedUri = SuperAppFileManager.saveFileFromUri(
                    context, sourceUri, "IMG_${System.currentTimeMillis()}.jpg", SuperAppFileManager.Category.IMAGE
                )
                savedUri?.let { onSendMessage("Sent a photo", AttachmentType.IMAGE, it.toString()) }
            }
        }
    }

    // 3. Camera Intent Launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            onSendMessage("Captured Photo", AttachmentType.IMAGE, tempCameraUri.toString())
        }
    }

    // Permission check request for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            coroutineScope.launch {
                val loc = locationManager.getCurrentLocation()
                if (loc != null) {
                    val locMessage = "📍 Current Location: https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                    onSendMessage(locMessage, AttachmentType.LOCATION, null)
                } else {
                    Toast.makeText(context, "Unable to pinpoint exact location.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Location permission required to share location.", Toast.LENGTH_SHORT).show()
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
                onTextChange = { messageText = it },
                onAttachClick = { showAttachmentMenu = true },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText, AttachmentType.NONE, null)
                        messageText = ""
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
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
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
    onLocationClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentIcon(Icons.Default.InsertDriveFile, "Document", Color(0xFF5F66CD)) { onDismiss(); onDocumentClick() }
                AttachmentIcon(Icons.Default.CameraAlt, "Camera", Color(0xFFD3396D)) { onDismiss(); onCameraClick() }
                AttachmentIcon(Icons.Default.Photo, "Gallery", Color(0xFFAC44CF)) { onDismiss(); onGalleryClick() }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentIcon(Icons.Default.LocationOn, "Location", Color(0xFF1DAA61)) { onDismiss(); onLocationClick() }
                AttachmentIcon(Icons.Default.Headset, "Audio", Color(0xFFE9592A)) { /* Audio logic */ }
                AttachmentIcon(Icons.Default.Person, "Contact", Color(0xFF00A5F4)) { /* Contact logic */ }
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
                                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.InsertDriveFile, tint = Color(0xFF5F66CD), contentDescription = "Document", modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Document Attached", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
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
                            Icon(Icons.Default.LocationOn, tint = Color(0xFF1DAA61), contentDescription = "Location", modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Location Pinned", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1DAA61))
                        }
                    }
                    else -> {} // Do nothing for NONE
                }

                // --- 2. TEXT RENDERING (Captions or Standard Text) ---
                // We only show the text if it's a normal message, or a custom typed caption.
                // We hide the ugly auto-generated texts like "Sent a photo"
                val hideAutoText = message.text in listOf("Captured Photo", "Sent a photo") || message.text.startsWith("📍 Current Location") || message.text.startsWith("Shared a document")

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
                    Text(text = ChatDateFormatter.formatTime(message.timestamp), fontSize = 11.sp, color = Color.Gray)

                    if (message.isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val (icon, tint) = when (message.status) {
                            MessageStatus.PENDING -> Icons.Default.CheckCircle to Color.LightGray
                            MessageStatus.SENT -> Icons.Default.Check to Color.Gray
                            MessageStatus.DELIVERED -> Icons.Default.CheckCircle to Color.Gray
                            MessageStatus.READ -> Icons.Default.CheckCircle to Color(0xFF34B7F1) // WhatsApp Blue
                        }
                        Icon(icon, contentDescription = "Status", tint = tint, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DateDivider(dateString: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Surface(color = Color(0xFFD6EAF8), shape = RoundedCornerShape(8.dp)) {
            Text(text = dateString, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun ChatBottomBar(text: String, onTextChange: (String) -> Unit, onAttachClick: () -> Unit, onSendClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.weight(1f), shadowElevation = 2.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAttachClick) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.Gray)
                }
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
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(onClick = onSendClick, containerColor = Color(0xFF00897B), shape = CircleShape) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
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