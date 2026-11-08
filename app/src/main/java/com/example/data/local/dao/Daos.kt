package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.StatusEntity
import com.example.data.local.entity.UserEntity

import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE qrPayload = :qrPayload LIMIT 1")
    suspend fun getUserByQrPayload(qrPayload: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUser()

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE userEmail = :ownerEmail ORDER BY contactDisplayName ASC")
    fun getContactsFlow(ownerEmail: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE userEmail = :ownerEmail ORDER BY contactDisplayName ASC")
    suspend fun getContactsList(ownerEmail: String): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContact(contactId: String)

    @Query("SELECT * FROM contacts WHERE userEmail = :ownerEmail AND contactEmail = :email LIMIT 1")
    suspend fun getContactByEmail(ownerEmail: String, email: String): ContactEntity?

    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE id = :contactId")
    suspend fun updateContactBlockedById(contactId: String, isBlocked: Boolean)

    @Query("""
        UPDATE contacts 
        SET isBlocked = :isBlocked 
        WHERE LOWER(userEmail) = LOWER(:ownerEmail) 
          AND (LOWER(contactEmail) = LOWER(:peerEmail) OR LOWER(contactUserId) = LOWER(:peerUserId))
    """)
    suspend fun updateContactBlockedByPeer(
        ownerEmail: String,
        peerEmail: String,
        peerUserId: String,
        isBlocked: Boolean
    )
}

@Dao
interface ChatDao {
    @Query("""
        SELECT 
            c.id AS id,
            c.isGroup AS isGroup,
            c.name AS name,
            c.groupDescription AS groupDescription,
            c.groupIconUri AS groupIconUri,
            c.participantIdsJson AS participantIdsJson,
            c.adminIdsJson AS adminIdsJson,
            c.lastMessageText AS lastMessageText,
            MAX(c.lastMessageTimestamp, COALESCE((SELECT MAX(m.timestamp) FROM messages m WHERE m.chatId = c.id), 0)) AS lastMessageTimestamp,
            c.unreadCount AS unreadCount
        FROM chats c
        GROUP BY c.id
        ORDER BY lastMessageTimestamp DESC
    """)
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats ORDER BY lastMessageTimestamp DESC")
    suspend fun getAllChatsList(): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)

    @Query("UPDATE chats SET lastMessageText = :text, lastMessageTimestamp = :timestamp WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, text: String, timestamp: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun clearUnreadCount(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatList(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND LOWER(status) != 'read'")
    suspend fun getUnreadMessagesForChat(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("""
        UPDATE messages 
        SET status = CASE 
            WHEN LOWER(status) = 'read' OR LOWER(:status) = 'read' THEN 'read'
            WHEN LOWER(status) = 'delivered' OR LOWER(:status) = 'delivered' THEN 'delivered'
            ELSE 'sent'
        END,
        deliveredAt = COALESCE(:deliveredAt, deliveredAt),
        readAt = COALESCE(:readAt, readAt)
        WHERE id = :messageId
    """)
    suspend fun updateMessageReceipt(messageId: String, status: String, deliveredAt: Long?, readAt: Long?)

    @Query("""
        UPDATE messages 
        SET status = 'delivered', 
            deliveredAt = COALESCE(deliveredAt, :deliveredAt) 
        WHERE id = :messageId AND LOWER(status) != 'read'
    """)
    suspend fun markMessageDelivered(messageId: String, deliveredAt: Long)

    @Query("UPDATE messages SET emailTransportStatus = :emailStatus WHERE id = :messageId")
    suspend fun updateEmailTransportStatus(messageId: String, emailStatus: String)

    @Query("UPDATE messages SET reactionsJson = :reactionsJson WHERE id = :messageId")
    suspend fun updateMessageReactions(messageId: String, reactionsJson: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("UPDATE messages SET text = :deletedText, mediaUri = NULL, mediaType = NULL, formattedRichText = NULL WHERE id = :messageId")
    suspend fun markMessageAsDeletedForEveryone(messageId: String, deletedText: String = "🚫 This message was deleted")

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("""
        UPDATE messages 
        SET status = 'read', 
            deliveredAt = COALESCE(deliveredAt, :readAt), 
            readAt = COALESCE(readAt, :readAt) 
        WHERE chatId = :chatId AND senderId != :currentUserId AND LOWER(status) != 'read'
    """)
    suspend fun markUnreadMessagesAsRead(chatId: String, currentUserId: String, readAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE messages 
        SET status = 'read', 
            deliveredAt = COALESCE(deliveredAt, :readAt), 
            readAt = COALESCE(readAt, :readAt) 
        WHERE id IN (:messageIds) AND LOWER(status) != 'read'
    """)
    suspend fun markMessagesAsReadByIds(messageIds: List<String>, readAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE messages 
        SET status = 'read', 
            deliveredAt = COALESCE(deliveredAt, :readAt), 
            readAt = COALESCE(readAt, :readAt) 
        WHERE id = :messageId
    """)
    suspend fun markSingleMessageAsRead(messageId: String, readAt: Long = System.currentTimeMillis())
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogsFlow(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses WHERE expiresTimestamp > :nowTimestamp ORDER BY createdTimestamp DESC")
    fun getActiveStatusesFlow(nowTimestamp: Long): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE expiresTimestamp > :nowTimestamp ORDER BY createdTimestamp DESC")
    suspend fun getActiveStatusesList(nowTimestamp: Long): List<StatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    @Query("UPDATE statuses SET isViewed = 1 WHERE id = :statusId")
    suspend fun markStatusAsViewed(statusId: String)

    @Query("DELETE FROM statuses WHERE id = :statusId")
    suspend fun deleteStatusById(statusId: String)

    @Query("DELETE FROM statuses WHERE expiresTimestamp <= :nowTimestamp")
    suspend fun deleteExpiredStatuses(nowTimestamp: Long)
}

