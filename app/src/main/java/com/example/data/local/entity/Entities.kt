package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUri: String? = null,
    val qrPayload: String,
    val statusMessage: String = "Hey there! I am using Linko.",
    val isCurrentUser: Boolean = false
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val userEmail: String, // owner email
    val contactUserId: String,
    val contactEmail: String,
    val contactDisplayName: String,
    val contactUsername: String,
    val contactAvatarUri: String? = null,
    val contactStatus: String = "Available",
    val addedTimestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val isGroup: Boolean = false,
    val name: String,
    val groupDescription: String? = null,
    val groupIconUri: String? = null,
    val participantIdsJson: String, // e.g. ["id1", "id2"]
    val adminIdsJson: String = "[]", // e.g. ["id1"]
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

object MessageStatus {
    const val SENT = "sent"
    const val DELIVERED = "delivered"
    const val READ = "read"

    fun normalize(raw: String?): String {
        return when (raw?.trim()?.lowercase()) {
            "read" -> READ
            "delivered" -> DELIVERED
            else -> SENT
        }
    }

    fun rank(status: String?): Int {
        return when (normalize(status)) {
            READ -> 3
            DELIVERED -> 2
            else -> 1
        }
    }
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderEmail: String,
    val text: String,
    val mediaUri: String? = null,
    val mediaType: String? = null, // "IMAGE", "AUDIO", "DOCUMENT", "CALL"
    val callType: String? = null, // "AUDIO", "VIDEO"
    val formattedRichText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = MessageStatus.SENT, // "sent", "delivered", "read"
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val emailTransportStatus: String = "DISPATCHED_SMTP", // "PENDING", "DISPATCHED_SMTP", "DELIVERED_INBOX"
    val emailSubject: String? = null,
    val emailMessageId: String? = null,
    val isEncrypted: Boolean = true,
    val encryptionAlgorithm: String = "AES-256-GCM",
    val reactionsJson: String = "{}"
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactEmail: String,
    val contactAvatarUri: String? = null,
    val callType: String, // "AUDIO", "VIDEO"
    val isIncoming: Boolean,
    val isMissed: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userDisplayName: String,
    val userEmail: String,
    val userAvatarUri: String? = null,
    val type: String, // "TEXT", "IMAGE", "VIDEO"
    val textContent: String? = null,
    val mediaUri: String? = null,
    val backgroundColorHex: String = "#10B981",
    val fontStyle: String = "DEFAULT",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val expiresTimestamp: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
    val isViewed: Boolean = false
)

