package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.UserEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

/**
 * Service managing Cloud Firestore synchronization for Group Chats, Broadcast Channels,
 * and multi-contact group invitations.
 */
object FirestoreGroupService {
    private const val TAG = "FirestoreGroupService"
    private const val COLLECTION_CHATS = "chats"
    private const val COLLECTION_INVITATIONS = "group_invitations"

    private var activeGroupsListener: ListenerRegistration? = null
    private var activeInvitationsListener: ListenerRegistration? = null

    private fun getFirestore(): FirebaseFirestore? {
        return FirestoreProvider.getInstance()
    }

    /**
     * Persist any chat (group, channel, or direct chat) to Cloud Firestore `/chats/{chatId}`.
     */
    fun uploadChat(
        chat: ChatEntity,
        participantEmails: List<String> = emptyList(),
        creatorId: String = "",
        channelType: String = if (chat.isGroup) "GROUP" else "DIRECT"
    ) {
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        try {
            val participantIds = parseJsonList(chat.participantIdsJson)
            val adminIds = parseJsonList(chat.adminIdsJson)
            val normalizedEmails = participantEmails.map { it.lowercase().trim() }.filter { it.isNotBlank() }.distinct()
            val inviteCode = if (chat.isGroup) "LNK-${chat.id.takeLast(6).uppercase()}" else null
            val path = "$COLLECTION_CHATS/${chat.id}"

            val payload = hashMapOf<String, Any?>(
                "id" to chat.id,
                "name" to chat.name.ifBlank { if (chat.isGroup) "Group Conversation" else "Chat" },
                "isGroup" to chat.isGroup,
                "channelType" to channelType,
                "groupDescription" to (chat.groupDescription ?: ""),
                "groupIconUri" to (chat.groupIconUri ?: ""),
                "creatorId" to creatorId.ifBlank { adminIds.firstOrNull() ?: "" },
                "participantIds" to participantIds,
                "participantEmails" to normalizedEmails,
                "adminIds" to adminIds,
                "inviteCode" to inviteCode,
                "lastMessageText" to chat.lastMessageText,
                "lastMessageTimestamp" to chat.lastMessageTimestamp,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection(COLLECTION_CHATS)
                .document(chat.id)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Chat ${chat.id} (${chat.name}) synced to Firestore in real time")
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, path)
                    Log.w(TAG, "Failed to sync chat ${chat.id} to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in uploadChat: ${e.message}")
        }
    }

    /**
     * Persist a newly created or updated group chat / channel to Firestore `/chats/{chatId}`.
     */
    fun uploadGroupChat(
        chat: ChatEntity,
        participantEmails: List<String> = emptyList(),
        creatorId: String = "",
        channelType: String = "GROUP"
    ) {
        uploadChat(chat, participantEmails, creatorId, channelType)
    }

    /**
     * Invite multiple contacts to an existing or newly created group/channel conversation.
     * Creates individual invitation documents in `/group_invitations` and updates `/chats/{chatId}` participants.
     */
    fun inviteContactsToGroup(
        chat: ChatEntity,
        inviter: UserEntity,
        invitedContacts: List<ContactEntity>,
        channelType: String = "GROUP"
    ) {
        if (invitedContacts.isEmpty()) return
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        try {
            val newIds = invitedContacts.map { it.contactUserId }.filter { it.isNotBlank() }
            val newEmails = invitedContacts.map { it.contactEmail.lowercase().trim() }.filter { it.isNotBlank() }

            // 1. Update group participants in /chats/{chatId}
            val chatUpdates = hashMapOf<String, Any>(
                "id" to chat.id,
                "name" to chat.name.ifBlank { "Group Conversation" },
                "isGroup" to chat.isGroup,
                "participantIds" to parseJsonList(chat.participantIdsJson).apply { addAll(newIds) }.distinct(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastMessageText" to chat.lastMessageText,
                "lastMessageTimestamp" to chat.lastMessageTimestamp
            )
            if (newEmails.isNotEmpty()) {
                chatUpdates["participantEmails"] = FieldValue.arrayUnion(*newEmails.toTypedArray())
            }

            db.collection(COLLECTION_CHATS)
                .document(chat.id)
                .set(chatUpdates, SetOptions.merge())
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_CHATS/${chat.id}")
                }

            // 2. Write invitation records in /group_invitations/{invitationId}
            val batch = db.batch()
            invitedContacts.forEach { contact ->
                val inviteId = "inv_${chat.id}_${contact.contactUserId.ifBlank { UUID.randomUUID().toString().take(6) }}"
                val docRef = db.collection(COLLECTION_INVITATIONS).document(inviteId)
                val invitePayload = mapOf(
                    "id" to inviteId,
                    "chatId" to chat.id,
                    "groupName" to chat.name,
                    "channelType" to channelType,
                    "inviterId" to inviter.id,
                    "inviterName" to inviter.displayName,
                    "inviteeUserId" to contact.contactUserId,
                    "inviteeEmail" to contact.contactEmail.lowercase().trim(),
                    "status" to "JOINED",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, invitePayload, SetOptions.merge())
            }

            batch.commit()
                .addOnSuccessListener {
                    Log.d(TAG, "Sent ${invitedContacts.size} group invitations for ${chat.name}")
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, COLLECTION_INVITATIONS)
                    Log.w(TAG, "Failed committing group invitations: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in inviteContactsToGroup: ${e.message}")
        }
    }

    /**
     * Sync updated participant list and admin list (after promoting/demoting admin or removing member).
     */
    fun syncGroupMembership(chat: ChatEntity) {
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        try {
            val participantIds = parseJsonList(chat.participantIdsJson)
            val adminIds = parseJsonList(chat.adminIdsJson)
            val updates = mapOf(
                "id" to chat.id,
                "name" to chat.name.ifBlank { "Group Conversation" },
                "isGroup" to chat.isGroup,
                "participantIds" to participantIds,
                "adminIds" to adminIds,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection(COLLECTION_CHATS)
                .document(chat.id)
                .set(updates, SetOptions.merge())
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_CHATS/${chat.id}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in syncGroupMembership: ${e.message}")
        }
    }

    /**
     * Update latest message snippet on the group document in Firestore.
     */
    fun updateGroupLastMessage(chatId: String, lastMessageText: String, timestamp: Long) {
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        try {
            val updates = mapOf(
                "id" to chatId,
                "lastMessageText" to lastMessageText,
                "lastMessageTimestamp" to timestamp,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection(COLLECTION_CHATS)
                .document(chatId)
                .set(updates, SetOptions.merge())
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_CHATS/$chatId")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not update group last message for $chatId: ${e.message}")
        }
    }

    /**
     * Listen in real-time to all chats (direct and group) in Firestore where the current user is a participant.
     */
    fun startRealtimeGroupSync(
        userId: String,
        userEmail: String,
        scope: CoroutineScope,
        onGroupsUpdated: suspend (List<ChatEntity>) -> Unit
    ) {
        activeGroupsListener?.remove()
        activeGroupsListener = null
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        val normalizedEmail = userEmail.lowercase().trim()

        try {
            activeGroupsListener = db.collection(COLLECTION_CHATS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, COLLECTION_CHATS)
                        Log.w(TAG, "Chat sync listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener

                    val syncedChats = mutableListOf<ChatEntity>()
                    for (doc in snapshot.documents) {
                        val id = doc.getString("id") ?: doc.id
                        val name = doc.getString("name") ?: "Chat"
                        val isGroup = doc.getBoolean("isGroup") ?: (id.startsWith("group_") || id.startsWith("channel_"))
                        val desc = doc.getString("groupDescription")
                        val iconUri = doc.getString("groupIconUri")?.ifBlank { null }
                        val lastMsg = doc.getString("lastMessageText") ?: "Conversation active"
                        val lastTime = doc.getLong("lastMessageTimestamp") ?: System.currentTimeMillis()

                        val rawParticipantIds = (doc.get("participantIds") as? List<*>)
                            ?.mapNotNull { it?.toString() }
                            ?: emptyList()
                        val rawParticipantEmails = (doc.get("participantEmails") as? List<*>)
                            ?.mapNotNull { it?.toString()?.lowercase()?.trim() }
                            ?: emptyList()
                        val rawAdminIds = (doc.get("adminIds") as? List<*>)
                            ?.mapNotNull { it?.toString() }
                            ?: emptyList()

                        val isParticipant = userId in rawParticipantIds ||
                                (normalizedEmail.isNotBlank() && normalizedEmail in rawParticipantEmails) ||
                                id.contains(userId)

                        if (isParticipant) {
                            val finalParticipants = if (userId.isNotBlank() && userId !in rawParticipantIds) {
                                rawParticipantIds + userId
                            } else {
                                rawParticipantIds
                            }

                            syncedChats.add(
                                ChatEntity(
                                    id = id,
                                    isGroup = isGroup,
                                    name = name,
                                    groupDescription = desc,
                                    groupIconUri = iconUri,
                                    participantIdsJson = toJsonArray(finalParticipants),
                                    adminIdsJson = toJsonArray(rawAdminIds),
                                    lastMessageText = lastMsg,
                                    lastMessageTimestamp = lastTime
                                )
                            )
                        }
                    }

                    if (syncedChats.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            onGroupsUpdated(syncedChats)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting realtime group sync: ${e.message}")
        }
    }

    fun stopRealtimeGroupSync() {
        activeGroupsListener?.remove()
        activeGroupsListener = null
        activeInvitationsListener?.remove()
        activeInvitationsListener = null
    }

    private fun parseJsonList(jsonStr: String): MutableList<String> {
        if (jsonStr.isBlank() || jsonStr == "[]") return mutableListOf()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (item.isNotBlank()) list.add(item)
            }
            list
        } catch (_: Exception) {
            jsonStr.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
                .toMutableList()
        }
    }

    private fun toJsonArray(list: List<String>): String {
        if (list.isEmpty()) return "[]"
        return list.distinct().joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"")
    }
}
