package com.amit.application.UI_screen.Chat.ChatScreen

import android.media.AudioManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amit.application.AppUtils.SuperAppFileManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


data class Message(
    val id: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle, // Automatically extracts navigation arguments!
    private val fileManager: SuperAppFileManager,
    private val audioManager: AudioManager
) : ViewModel() {

    // Extract the arguments passed from the Contact List
    val friendNumber: String = savedStateHandle.get<String>("friendNumber") ?: ""
    val friendName: String = savedStateHandle.get<String>("friendName") ?: "Unknown"

    // Get the current logged-in user's phone number
    private val myNumber: String = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    init {
        // Start fetching Firestore messages the moment the chat opens
        if (myNumber.isNotEmpty() && friendNumber.isNotEmpty()) {
            observeMessages()
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.observeChatHistory(myNumber, friendNumber).collect { history ->
                _messages.value = history
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                myNumber = myNumber,
                friendNumber = friendNumber,
                text = text
            )
        }
    }
}
