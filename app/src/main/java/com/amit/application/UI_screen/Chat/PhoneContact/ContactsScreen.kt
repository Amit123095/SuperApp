package com.amit.application.UI_screen.Chat.PhoneContact

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amit.application.UI_screen.Profile.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onUserClick: (String, String) -> Unit// Pass the phone number to open the chat!
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Setup the Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.syncContacts(context)
        } else {
            // In a real app, handle permission denial gracefully here
        }
    }

    // Request permission immediately when the screen opens
    LaunchedEffect(Unit) {
        if (uiState is ContactsUiState.Initial) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Contact") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            when (val state = uiState) {
                is ContactsUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Syncing contacts with Firebase...")
                    }
                }

                is ContactsUiState.Success -> {
                    if (state.friends.isEmpty()) {
                        /*Text(
                            "None of your contacts are using SuperApp yet.",
                            modifier = Modifier.align(Alignment.Center)
                        )*/
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No contacts found.")
                            Spacer(modifier = Modifier.height(16.dp))
                            // Add a manual trigger button!
                            Button(onClick = { viewModel.syncContacts(context) }) {
                                Text("Force Sync Contacts")
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.friends, key = { it.phoneNumber }) { friend ->
                                ContactItem(
                                    user = friend,
                                    // Change this to pass both variables!
                                    onClick = { onUserClick(friend.phoneNumber, friend.name) }
                                )
                            }
                            /*items(state.friends, key = { it.phoneNumber }) { friend ->
                                ContactItem(user = friend, onClick = { onUserClick(friend.phoneNumber) })
                            }*/
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ContactItem(user: AppUser, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(user.name) },
        supportingContent = { Text(user.phoneNumber) },
        leadingContent = {
            // Reuse the awesome UserAvatar you built earlier!
            UserAvatar(name = user.name, imageUrl = user.profileImageUrl)
        },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}