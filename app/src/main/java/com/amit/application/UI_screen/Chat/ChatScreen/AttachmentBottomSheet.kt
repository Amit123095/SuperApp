package com.amit.application.UI_screen.Chat.ChatScreen

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onFileSelected: (Uri, AttachmentType) -> Unit
) {
    val context = LocalContext.current

    // Launcher for large documents/files
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Check file size (200MB limit)
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                val sizeInBytes = cursor.getLong(sizeIndex)
                val sizeInMb = sizeInBytes / (1024 * 1024)

                if (sizeInMb > 200) {
                    Toast.makeText(context, "File is over 200MB. Please use Google Drive to share the link.", Toast.LENGTH_LONG).show()
                } else {
                    onFileSelected(it, AttachmentType.DOCUMENT)
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentIcon(Icons.Default.InsertDriveFile, "Document", Color(0xFF5F66CD)) { fileLauncher.launch("*/*") }
                AttachmentIcon(Icons.Default.CameraAlt, "Camera", Color(0xFFD3396D)) { /* Launch Camera */ }
                AttachmentIcon(Icons.Default.Photo, "Gallery", Color(0xFFAC44CF)) { /* Launch Gallery */ }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentIcon(Icons.Default.Headset, "Audio", Color(0xFFE9592A)) { /* Launch Audio Picker */ }
                AttachmentIcon(Icons.Default.LocationOn, "Location", Color(0xFF1DAA61)) { /* Launch Location Sharing */ }
                AttachmentIcon(Icons.Default.Person, "Contact", Color(0xFF00A5F4)) { /* Launch Contact Picker */ }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AttachmentIcon(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}