package com.amit.application.UI_screen.Login

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() {

    // Hilt will automatically provide the Firestore instance if you set up a module,
    // or you can just instantiate it directly for now:
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun saveUserToDatabase(phoneNumber: String) {
        // We only set the baseline fields. We don't want to overwrite
        // the "name" field if they already set it up previously!
        val userData = hashMapOf(
            "phoneNumber" to phoneNumber,
            "lastSeen" to System.currentTimeMillis()
        )

        try {
            // SetOptions.merge() is the magic trick here!
            firestore.collection("users")
                .document(phoneNumber) // Use the phone number as the document ID
                .set(userData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
            // In a production app, you might want to handle this error
        }
    }
}