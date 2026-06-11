package com.amit.application.UI_screen.Login

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToDashboard: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current as Activity // Needed for Firebase Phone Auth

    // Observe success state to trigger navigation
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToDashboard()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to SuperApp", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        when (val state = authState) {
            is AuthState.Loading -> {
                CircularProgressIndicator()
                Text("Authenticating...", modifier = Modifier.padding(top = 16.dp))
            }

            is AuthState.OtpSent -> {
                // Show OTP Input UI
                OtpInputSection(
                    onVerifyClick = { code ->
                        viewModel.verifyOtp(state.verificationId, code)
                    }
                )
            }

            else -> {
                // Show Default Login UI (Phone + Google)
                if (state is AuthState.Error) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PhoneInputSection(
                    onSendOtpClick = { phone -> viewModel.sendOtp(phone, context) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("--- OR ---", color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        // Trigger Google Sign In Flow
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Continue with Google")
                }
            }
        }
    }
}

@Composable
fun PhoneInputSection(onSendOtpClick: (String) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }

    OutlinedTextField(
        value = phoneNumber,
        onValueChange = {
            // Only allow exactly 10 digits and ignore non-numeric characters
            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                phoneNumber = it
            }
        },
        label = { Text("Phone Number") },
        // This locks "+91" inside the text field visually
        prefix = { Text("+91 ") },
        placeholder = { Text(" - Enter your phone number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            // Concatenate the country code before sending it to the ViewModel/Firebase
            onSendOtpClick("+91$phoneNumber")
        },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        // Only enable the button if exactly 10 digits are entered
        enabled = phoneNumber.length == 10
    ) {
        Text("Send OTP")
    }
}

@Composable
fun OtpInputSection(onVerifyClick: (String) -> Unit) {
    var otpCode by remember { mutableStateOf("") }

    Text("Enter the 6-digit code sent to your phone.")
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = otpCode,
        onValueChange = { if (it.length <= 6) otpCode = it },
        label = { Text("OTP Code") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { onVerifyClick(otpCode) },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = otpCode.length == 6
    ) {
        Text("Verify & Sign In")
    }
}