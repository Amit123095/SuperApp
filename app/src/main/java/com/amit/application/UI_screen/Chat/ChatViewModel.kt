package com.amit.application.UI_screen.Chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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


/*
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


data class Message(
    val id: String,
    val text: String,
    val isFromMe: Boolean, // Determines if the bubble is green (right) or gray (left)
    val timestamp: String
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository // Injected via Hilt!
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        // Start listening to the WebSocket as soon as the ViewModel is created
        observeMessages()
    }

    private fun observeMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.observeIncomingMessages().collect { incomingMessage ->
                // When a new message arrives from the server, add it to the top of our list
                _messages.update { currentList ->
                    listOf(incomingMessage) + currentList
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Optimistically show the message on our UI immediately
        val myMessage = Message(
            id = System.currentTimeMillis().toString(),
            text = text,
            isFromMe = true,
            timestamp = "Sending..."
        )

        _messages.update { currentList -> listOf(myMessage) + currentList }

        // 2. Actually send it to the backend via WebSocket
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.sendMessage(text)
        }
    }
}
*/

/*
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    // Initial dummy data. Notice the list is ordered newest-first
    // because our LazyColumn will be reversed!
    private val _messages = MutableStateFlow(
        listOf(
            Message("3", "I am good, just working on a Compose app!", true, "10:24 AM"),
            Message("2", "Hey! How are you doing?", false, "10:23 AM"),
            Message("1", "Hello!", false, "10:22 AM")
        )
    )

    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val newMessage = Message(
            id = System.currentTimeMillis().toString(),
            text = text,
            isFromMe = true,
            timestamp = "Just now" // In a real app, format the actual time
        )

        // Add the new message to the TOP of the list
        _messages.update { currentList ->
            listOf(newMessage) + currentList
        }

        // TODO: Here you would also trigger your WebSocket/Network call to send it to the server
    }
}*/
