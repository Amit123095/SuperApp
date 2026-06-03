package com.amit.application.UI_screen.Login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// 1. The States (Same as before)
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class OtpSent(val verificationId: String) : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    // 2. The Firebase Callback Listener
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // MAGIC: Android auto-read the SMS! Skip the OTP input screen entirely.
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // Triggered if the number is invalid, or if the device fails the SafetyNet/Play Integrity check
            _authState.value = AuthState.Error(e.localizedMessage ?: "Verification failed")
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // SMS successfully sent. Update the UI to show the OTP input box.
            _authState.value = AuthState.OtpSent(verificationId)
        }
    }

    // 3. Trigger the SMS
    fun sendOtp(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Number must include country code! e.g., +919876543210
            .setTimeout(60L, TimeUnit.SECONDS) // Time before user can request a new SMS
            .setActivity(activity)             // Required for Google's anti-spam reCAPTCHA
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // 4. Manual OTP Verification (User types it in)
    fun verifyOtp(verificationId: String, code: String) {
        _authState.value = AuthState.Loading
        // Combine the SMS code with the session ID to create a credential
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    // 5. The Final Sign-In Execution
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                // Wait for Firebase to authenticate the user
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null && user.phoneNumber != null) {

                    // 3. Save them to Firestore immediately!
                    userRepository.saveUserToDatabase(user.phoneNumber!!)

                    // 4. NOW we tell the UI to navigate to the Dashboard
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error("Authentication succeeded, but user is null.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Invalid Code")
            }
        }
    }

    fun resetError() {
        _authState.value = AuthState.Idle
    }
}

