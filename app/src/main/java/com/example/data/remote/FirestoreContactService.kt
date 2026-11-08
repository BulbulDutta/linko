package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.ContactEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirestoreContactService {
    private const val TAG = "FirestoreContactService"
    private const val COLLECTION_CONTACTS = "user_contacts"

    private fun getFirestore(): FirebaseFirestore? {
        return FirestoreProvider.getInstance()
    }

    private var activeSyncListener: ListenerRegistration? = null

    fun isGeminiAiContact(userId: String?, email: String?, displayName: String? = null): Boolean {
        val idClean = userId?.lowercase()?.trim() ?: ""
        val emailClean = email?.lowercase()?.trim() ?: ""
        val nameClean = displayName?.lowercase()?.trim() ?: ""
        return idClean == "user_gemini" ||
            idClean == "chat_gemini_ai" ||
            idClean == "c_gemini" ||
            emailClean.contains("gemini") ||
            nameClean == "gemini" ||
            nameClean.contains("gemini ai")
    }

    /**
     * Upload or update a single contact to Firebase Firestore.
     * Enforces AI Exception: Gemini AI bot can never be marked as blocked.
     */
    fun uploadContact(contact: ContactEntity) {
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        try {
            val safeBlocked = if (isGeminiAiContact(contact.contactUserId, contact.contactEmail, contact.contactDisplayName)) {
                false
            } else {
                contact.isBlocked
            }
            val contactData = mapOf(
                "id" to contact.id,
                "userEmail" to contact.userEmail.lowercase().trim(),
                "contactUserId" to contact.contactUserId,
                "contactEmail" to contact.contactEmail,
                "contactDisplayName" to contact.contactDisplayName,
                "contactUsername" to contact.contactUsername,
                "contactStatus" to contact.contactStatus,
                "contactAvatarUri" to (contact.contactAvatarUri ?: ""),
                "isBlocked" to safeBlocked,
                "updatedAt" to System.currentTimeMillis()
            )

            // Doc ID format: ownerEmail_contactId
            val docId = "${contact.userEmail.lowercase().trim()}_${contact.id}"
            val path = "$COLLECTION_CONTACTS/$docId"
            db.collection(COLLECTION_CONTACTS)
                .document(docId)
                .set(contactData)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced contact ${contact.contactDisplayName} (isBlocked=$safeBlocked) to Firestore")
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, path)
                    Log.e(TAG, "Error syncing contact to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during uploadContact: ${e.message}")
        }
    }

    /**
     * Immediately syncs a contact's blocked/unblocked status to Cloud Firestore.
     */
    fun updateContactBlockState(contact: ContactEntity) {
        if (isGeminiAiContact(contact.contactUserId, contact.contactEmail, contact.contactDisplayName)) {
            Log.w(TAG, "AI Exception: Gemini AI Assistant cannot be blocked.")
            return
        }
        uploadContact(contact)
    }

    /**
     * Upload a batch of contacts to Firestore (e.g., initial local contacts).
     */
    fun uploadContactsBatch(contacts: List<ContactEntity>) {
        if (!FirestoreProvider.isAuthenticated()) return
        contacts.forEach { uploadContact(it) }
    }

    /**
     * Fetch existing contacts from Firestore for the user and save them locally.
     */
    fun fetchAndSyncContacts(
        ownerEmail: String,
        scope: CoroutineScope,
        onContactsReceived: suspend (List<ContactEntity>) -> Unit
    ) {
        // Enforce real-time snapshot listener instead of one-time .get()
        startRealtimeContactSync(
            ownerEmail = ownerEmail,
            scope = scope,
            onContactsReceived = onContactsReceived
        )
    }

    /**
     * Start real-time Firestore listener to keep contacts synced across devices.
     */
    fun startRealtimeContactSync(
        ownerEmail: String,
        scope: CoroutineScope,
        onContactsReceived: suspend (List<ContactEntity>) -> Unit
    ) {
        activeSyncListener?.remove()
        activeSyncListener = null

        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        val normalizedEmail = ownerEmail.lowercase().trim()
        if (normalizedEmail.isBlank()) return

        try {
            activeSyncListener = db.collection(COLLECTION_CONTACTS)
                .whereEqualTo("userEmail", normalizedEmail)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, COLLECTION_CONTACTS)
                        Log.e(TAG, "Firestore contact listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val contacts = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: return@mapNotNull null
                            val userEmail = doc.getString("userEmail") ?: normalizedEmail
                            val contactUserId = doc.getString("contactUserId") ?: ""
                            val contactEmail = doc.getString("contactEmail") ?: ""
                            val contactDisplayName = doc.getString("contactDisplayName") ?: ""
                            val contactUsername = doc.getString("contactUsername") ?: ""
                            val contactStatus = doc.getString("contactStatus") ?: ""
                            val contactAvatarUri = doc.getString("contactAvatarUri")?.ifBlank { null }
                            val rawBlocked = doc.getBoolean("isBlocked") ?: false
                            val safeBlocked = if (isGeminiAiContact(contactUserId, contactEmail, contactDisplayName)) false else rawBlocked

                            ContactEntity(
                                id = id,
                                userEmail = userEmail,
                                contactUserId = contactUserId,
                                contactEmail = contactEmail,
                                contactDisplayName = contactDisplayName,
                                contactUsername = contactUsername,
                                contactStatus = contactStatus,
                                contactAvatarUri = contactAvatarUri,
                                isBlocked = safeBlocked
                            )
                        }

                        scope.launch(Dispatchers.IO) {
                            onContactsReceived(contacts)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting realtime contact sync: ${e.message}")
        }
    }

    fun stopRealtimeSync() {
        activeSyncListener?.remove()
        activeSyncListener = null
    }
}
