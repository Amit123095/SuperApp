package com.amit.application.UI_screen.Chat.ChatScreen

enum class MessageStatus {
    PENDING,   // Grey dot (Offline/Local storage)
    SENT,      // Single black tick (Sent to server)
    DELIVERED, // Double black dot/tick (Delivered to friend)
    READ       // Green dot/tick (Read by friend)
}

enum class AttachmentType {
    NONE, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION
}

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: Long, // Epoch milliseconds
    val isFromMe: Boolean,
    var status: MessageStatus = MessageStatus.PENDING,
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val attachmentUri: String? = null
)