package com.amit.application.UI_screen.Chat

import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


@Singleton
class ChatRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    // Generates a unique ID for the chat room between two users
    private fun getChatRoomId(myNumber: String, friendNumber: String): String {
        return listOf(myNumber, friendNumber).sorted().joinToString("_")
    }

    // THIS IS THE MISSING FUNCTION!
    fun observeChatHistory(myNumber: String, friendNumber: String): Flow<List<Message>> = callbackFlow {
        val chatId = getChatRoomId(myNumber, friendNumber)

        val listener = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Order oldest to newest
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        Message(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            isFromMe = doc.getString("senderId") == myNumber,
                            timestamp = doc.getLong("timestamp")?.toString() ?: "Just now"
                        )
                    }
                    trySend(messages)
                }
            }

        // Cleanup listener when the user leaves the chat screen
        awaitClose { listener.remove() }
    }

    // Ensure you also have the sendMessage function!
    fun sendMessage(myNumber: String, friendNumber: String, text: String) {
        val chatId = getChatRoomId(myNumber, friendNumber)

        val messageMap = hashMapOf(
            "text" to text,
            "senderId" to myNumber,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("chats").document(chatId)
            .collection("messages")
            .add(messageMap)
    }
}



/*
@Singleton
class ChatRepository @Inject constructor() {

    // Initialize Ktor Client with WebSocket support
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null

    // 1. Establish connection and expose incoming messages as a continuous Flow
    fun observeIncomingMessages(): Flow<Message> = flow {
        try {
            // Replace with your actual WebSocket backend URL
            client.webSocket("wss://echo.websocket.events") {
                session = this

                // Keep listening as long as the connection is active
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        // In a real app, parse the JSON string into a Message object via Gson/Kotlinx-Serialization
                        val receivedMessage = Message(
                            id = System.currentTimeMillis().toString(),
                            text = text,
                            isFromMe = false,
                            timestamp = "Just now"
                        )
                        emit(receivedMessage) // Send it to the ViewModel
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle reconnect logic or emit an error state here
        } finally {
            session = null
        }
    }

    // 2. Send a message to the server
    suspend fun sendMessage(text: String) {
        session?.let {
            if (it.isActive) {
                it.send(Frame.Text(text))
            }
        }
    }
}*/
