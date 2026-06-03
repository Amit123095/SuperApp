package com.amit.application.UI_screen.Profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(name = "Amit", imageUrl = null, size = 100.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Amit", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "+91 98765 43210", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }

            HorizontalDivider()

            // 2. Main Options
            ProfileListItem(icon = Icons.Default.Person, title = "Name", subtitle = "Amit")
            ProfileListItem(icon = Icons.Default.Email, title = "Email", subtitle = "amit@example.com")
            ProfileListItem(icon = Icons.Default.Phone, title = "Phone Number", subtitle = "+91 9876543210")
            ProfileListItem(icon = Icons.Default.LocationOn, title = "Default Address", subtitle = "123 Tech Park, Kolkata")
            ProfileListItem(icon = Icons.Default.ShoppingCart, title = "Payment Details", subtitle = "Visa ending in 4321")

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 3. Bottom Section (About & Logout)
            ProfileListItem(icon = Icons.Default.Info, title = "About SuperApp", subtitle = "Terms, Privacy Policy")

            ListItem(
                headlineContent = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onLogoutClick() }
            )

            // App Version at the very bottom
            Text(
                text = "App Version 1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}

// Reusable list item component
@Composable
fun ProfileListItem(icon: ImageVector, title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable { /* Handle edit clicks later */ }
    )
}