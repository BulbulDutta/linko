package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.data.crypto.EncryptionManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.remote.FirestoreMessageService
import com.example.util.LinkoCallSignalingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appContext = context.applicationContext

        when (action) {
            NotificationHelper.ACTION_REPLY_MESSAGE -> {
                val results = RemoteInput.getResultsFromIntent(intent)
                val replyText = results?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString()?.trim()
                val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID) ?: ""
                val senderName = intent.getStringExtra(NotificationHelper.EXTRA_SENDER_NAME)
                    ?: intent.getStringExtra(NotificationHelper.EXTRA_CALLER_NAME)
                    ?: "Chat"
                val senderEmail = intent.getStringExtra(NotificationHelper.EXTRA_SENDER_EMAIL) ?: ""
                val isGroup = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_GROUP, false)

                if (!replyText.isNullOrBlank() && chatId.isNotBlank()) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getInstance(appContext)
                            val me = db.userDao().getCurrentUser()
                            val senderId = me?.id ?: "user_local"
                            val myName = me?.displayName ?: "You"
                            val myEmail = me?.email ?: "me@linko.com"
                            val now = System.currentTimeMillis()

                            val targetChatId = if (!isGroup && chatId != "chat_gemini_ai" && senderEmail.isNotBlank()) {
                                com.example.data.repository.LinkoRepository.generateDirectChatId(
                                    user1Id = senderId,
                                    user1Email = myEmail,
                                    user2Id = com.example.data.repository.LinkoRepository.canonicalUserIdFromEmail(senderEmail),
                                    user2Email = senderEmail
                                )
                            } else {
                                chatId
                            }

                            // Ensure chat exists in local database so user can view it
                            val existingChat = db.chatDao().getChatById(targetChatId) ?: db.chatDao().getChatById(chatId)
                            val finalChatId = existingChat?.id ?: targetChatId
                            if (existingChat == null) {
                                val peerCanonical = com.example.data.repository.LinkoRepository.canonicalUserIdFromEmail(senderEmail)
                                db.chatDao().insertOrUpdateChat(
                                    ChatEntity(
                                        id = finalChatId,
                                        isGroup = isGroup,
                                        name = senderName,
                                        participantIdsJson = "[\"$senderId\",\"$peerCanonical\"]",
                                        lastMessageText = replyText,
                                        lastMessageTimestamp = now
                                    )
                                )
                            } else {
                                db.chatDao().updateLastMessage(finalChatId, replyText, now)
                                db.chatDao().clearUnreadCount(finalChatId)
                            }

                            val msgId = "m_${UUID.randomUUID().toString().take(8)}"
                            val encryptedText = EncryptionManager.encrypt(replyText, finalChatId)
                            val replyEntity = MessageEntity(
                                id = msgId,
                                chatId = finalChatId,
                                senderId = senderId,
                                senderName = myName,
                                senderEmail = myEmail,
                                text = encryptedText,
                                timestamp = now,
                                status = MessageStatus.SENT,
                                isEncrypted = true,
                                encryptionAlgorithm = "AES-256-GCM"
                            )
                            db.messageDao().insertMessage(replyEntity)
                            val participants = if (senderEmail.isNotBlank()) listOf(senderId, myEmail, senderEmail) else listOf(senderId, myEmail)
                            FirestoreMessageService.uploadMessage(replyEntity, participants)
                        } catch (_: Exception) {
                        } finally {
                            pendingResult.finish()
                        }
                    }

                    Toast.makeText(context, "Reply sent to $senderName", Toast.LENGTH_SHORT).show()

                    // Update notification with inline reply confirmation
                    val confirmationNotif = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID_MESSAGES)
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setContentTitle("Replied to $senderName")
                        .setContentText("You: $replyText")
                        .setAutoCancel(true)
                        .setTimeoutAfter(2500)
                        .build()

                    try {
                        NotificationManagerCompat.from(context).notify(chatId.hashCode(), confirmationNotif)
                    } catch (_: SecurityException) {}
                }
            }

            NotificationHelper.ACTION_MARK_READ -> {
                val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID) ?: ""
                NotificationHelper.cancelNotification(context, chatId.hashCode())
                if (chatId.isNotBlank()) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getInstance(appContext)
                            val me = db.userDao().getCurrentUser()
                            val now = System.currentTimeMillis()
                            db.chatDao().clearUnreadCount(chatId)
                            if (me != null) {
                                val unreadIds = db.messageDao().getUnreadMessagesForChat(chatId).map { it.id }
                                db.messageDao().markUnreadMessagesAsRead(chatId, me.id, now)
                                FirestoreMessageService.markChatMessagesRead(
                                    chatId = chatId,
                                    currentUserId = me.id,
                                    currentUserEmail = me.email,
                                    messageIds = unreadIds
                                )
                            }
                        } catch (_: Exception) {
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                Toast.makeText(context, "Marked conversation as read", Toast.LENGTH_SHORT).show()
            }

            NotificationHelper.ACTION_ACCEPT_FRIEND_REQUEST -> {
                val requesterName = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_NAME) ?: "Friend"
                val requesterEmail = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_EMAIL) ?: "friend@linko.com"

                NotificationHelper.cancelNotification(context, NotificationHelper.NOTIFICATION_ID_FRIEND_REQUEST)
                Toast.makeText(context, "Accepted friend request from $requesterName!", Toast.LENGTH_LONG).show()

                // Launch MainActivity to complete contact addition and navigate to Friends screen
                val forwardIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("extra_accept_friend_email", requesterEmail)
                    putExtra("extra_accept_friend_name", requesterName)
                }
                context.startActivity(forwardIntent)
            }

            NotificationHelper.ACTION_DECLINE_FRIEND_REQUEST -> {
                val requesterName = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_NAME) ?: "User"
                NotificationHelper.cancelNotification(context, NotificationHelper.NOTIFICATION_ID_FRIEND_REQUEST)
                Toast.makeText(context, "Declined friend request from $requesterName", Toast.LENGTH_SHORT).show()
            }

            NotificationHelper.ACTION_DECLINE_CALL -> {
                val callerName = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_NAME) ?: "Caller"
                val callerEmail = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_EMAIL) ?: "caller@linko.com"
                val callTypeStr = intent.getStringExtra(NotificationHelper.EXTRA_CALL_TYPE) ?: "Voice"

                NotificationHelper.cancelNotification(context, NotificationHelper.NOTIFICATION_ID_INCOMING_CALL)
                LinkoCallSignalingEngine.getInstance(appContext).endCall("Declined from notification")

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(appContext)
                        db.callLogDao().insertCallLog(
                            CallLogEntity(
                                id = "call_${UUID.randomUUID().toString().take(8)}",
                                contactName = callerName,
                                contactEmail = callerEmail,
                                contactAvatarUri = null,
                                callType = if (callTypeStr.equals("Video", ignoreCase = true)) "VIDEO" else "AUDIO",
                                isIncoming = true,
                                isMissed = true,
                                timestamp = System.currentTimeMillis(),
                                durationSeconds = 0
                            )
                        )
                    } catch (_: Exception) {
                    } finally {
                        pendingResult.finish()
                    }
                }

                Toast.makeText(context, "Declined $callTypeStr call from $callerName", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
