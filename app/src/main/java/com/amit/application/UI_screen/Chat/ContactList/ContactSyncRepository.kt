package com.amit.application.UI_screen.Chat.ContactList

import android.content.Context
import android.provider.ContactsContract
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

data class AppUser(
    val phoneNumber: String,
    val name: String,
    val profileImageUrl: String?
)

@Singleton
class ContactSyncRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    // 1. Read and Normalize Contacts from the Device
    private fun getLocalContacts(context: Context): List<String> {
        val contacts = mutableSetOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val rawNumber = it.getString(numberIndex)
                val normalized = normalizePhoneNumber(rawNumber)
                if (normalized != null) {
                    contacts.add(normalized)
                }
            }
        }
        return contacts.toList()
    }

    // Standardize numbers so they exactly match Firestore Document IDs
    private fun normalizePhoneNumber(number: String): String? {
        // Strip out spaces, dashes, and parentheses
        var clean = number.replace(Regex("[^0-9+]"), "")

        // If it's a 10-digit Indian number without the country code, add +91
        if (clean.length == 10 && !clean.startsWith("+")) {
            clean = "+91$clean"
        }

        // Return valid numbers, discard short/invalid ones
        return if (clean.startsWith("+") && clean.length > 10) clean else null
    }

    // 2. Query Firestore Safely (Chunking by 10)
    suspend fun findFriendsOnApp(context: Context): List<AppUser> {
        val localNumbers = getLocalContacts(context)

        // DEBUG LOG 1: See what the app actually read from your phone
        Log.d("ContactSync", "Total contacts found on device: ${localNumbers.size}")
        Log.d("ContactSync", "Normalized Numbers: $localNumbers")

        if (localNumbers.isEmpty()) return emptyList()

        val matchedUsers = mutableListOf<AppUser>()

        // Firestore 'whereIn' limits to 10 items per batch!
        val chunks = localNumbers.chunked(10)

        for (chunk in chunks) {
            try {
                val snapshot = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), chunk) // Search by Document ID (Phone Number)
                    .get()
                    .await()

                // DEBUG LOG 2: See how many matched Firestore
                Log.d("ContactSync", "Found ${snapshot.documents.size} matches in this chunk")

                for (doc in snapshot.documents) {
                    matchedUsers.add(
                        AppUser(
                            phoneNumber = doc.id,
                            name = doc.getString("name") ?: "New User",
                            profileImageUrl = doc.getString("profileImageUrl")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ContactSync", "Firestore Query Failed", e)
            }
        }

        return matchedUsers
    }
}