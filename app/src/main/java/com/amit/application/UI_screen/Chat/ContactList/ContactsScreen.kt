package com.amit.application.UI_screen.Chat.ContactList

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.amit.application.UI_screen.Chat.ChatScreen.ChatDateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- DATA MODEL FOR INBOX ---
data class ChatThread(
    val id: String,
    val friendName: String,
    val friendNumber: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isOnline: Boolean,
    var isPinned: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    onBackClick: () -> Unit,
    onUserClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    var chatThreads by remember { mutableStateOf<List<ChatThread>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 1. Permission State checking
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 2. Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            isLoading = false
            Toast.makeText(context, "Contacts permission is required to chat.", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Load Contacts the moment the screen opens (or when permission is granted)
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            chatThreads = fetchDeviceContacts(context)
            isLoading = false
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // Sorting Logic: Pinned first, then alphabetical (since we don't have real timestamps yet)
    val sortedChats = chatThreads.sortedWith(
        compareByDescending<ChatThread> { it.isPinned }
            .thenBy { it.friendName }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00897B))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {
                items(sortedChats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        onClick = { onUserClick(chat.friendNumber, chat.friendName) },
                        onLongClick = {
                            // Toggle Pinned Status for this mock UI
                            chatThreads = chatThreads.map {
                                if (it.id == chat.id) it.copy(isPinned = !it.isPinned) else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: ChatThread,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture & Online Dot
        Box(modifier = Modifier.size(50.dp)) {
            val initial = if (chat.friendName.isNotBlank()) chat.friendName.first().toString().uppercase() else "?"
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFB2DFDB)),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
            }

            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                        .padding(2.dp)
                        .background(Color(0xFF25D366), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name & Snippet
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.friendName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                fontSize = 14.sp,
                color = if (chat.unreadCount > 0) Color.Black else Color.Gray,
                fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Timestamp, Pin & Unread Badge
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = ChatDateFormatter.formatTime(chat.timestamp),
                fontSize = 12.sp,
                color = if (chat.unreadCount > 0) Color(0xFF25D366) else Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier.size(22.dp).background(Color(0xFF25D366), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = chat.unreadCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- HELPER TO EXTRACT REAL CONTACTS FROM DEVICE ---
suspend fun fetchDeviceContacts(context: Context): List<ChatThread> {
    return withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<ChatThread>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            // To prevent duplicated entries (common in Android if they have multiple numbers)
            val addedNumbers = mutableSetOf<String>()

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: "Unknown"
                var number = it.getString(numberIndex) ?: ""

                // Clean up formatting (e.g. "+1 555-1234" -> "+15551234")
                number = number.replace(" ", "").replace("-", "")

                if (number.isNotBlank() && !addedNumbers.contains(number)) {
                    addedNumbers.add(number)
                    contactsList.add(
                        ChatThread(
                            id = number,
                            friendName = name,
                            friendNumber = number,
                            lastMessage = "Tap to start chatting", // Placeholder until Firebase connects
                            timestamp = System.currentTimeMillis(),
                            unreadCount = 0,
                            isOnline = false, // Placeholder
                            isPinned = false
                        )
                    )
                }
            }
        }
        contactsList
    }
}