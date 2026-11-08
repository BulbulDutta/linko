package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ScheduledNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_NOTIF_TYPE) ?: TYPE_MESSAGE
        val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID) ?: "chat_1"
        val senderName = intent.getStringExtra(EXTRA_SENDER_NAME) ?: "Elena Vance"
        val messageText = intent.getStringExtra(EXTRA_MESSAGE_TEXT)
            ?: "Hey! Here is a direct message update from Linko 🌟"
        val callerName = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_NAME) ?: "Marcus Miller"
        val callerEmail = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_EMAIL) ?: "marcus@linko.com"
        val callType = intent.getStringExtra(NotificationHelper.EXTRA_CALL_TYPE) ?: "Voice"
        val requesterName = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_NAME) ?: "Sophia Chen"
        val requesterEmail = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_EMAIL) ?: "sophia@linko.com"

        val fcmPayload = mapOf(
            "type" to type,
            "chat_id" to chatId,
            "sender_name" to senderName,
            "message_text" to messageText,
            "is_group" to (type == TYPE_GROUP_MESSAGE).toString(),
            "caller_name" to callerName,
            "caller_email" to callerEmail,
            "call_type" to callType,
            "requester_name" to requesterName,
            "requester_email" to requesterEmail
        )

        LinkoFirebaseMessagingService.dispatchRemotePayload(
            context = context,
            data = fcmPayload
        )
    }

    companion object {
        const val EXTRA_NOTIF_TYPE = "extra_notif_type"
        const val EXTRA_SENDER_NAME = "extra_sender_name"
        const val EXTRA_MESSAGE_TEXT = "extra_message_text"

        const val TYPE_MESSAGE = "TYPE_MESSAGE"
        const val TYPE_GROUP_MESSAGE = "TYPE_GROUP_MESSAGE"
        const val TYPE_CALL = "TYPE_CALL"
        const val TYPE_MISSED_CALL = "TYPE_MISSED_CALL"
        const val TYPE_FRIEND_REQUEST = "TYPE_FRIEND_REQUEST"

        fun scheduleBackgroundPush(
            context: Context,
            type: String,
            delaySeconds: Long = 5,
            chatId: String = "chat_1",
            senderOrCallerName: String? = null,
            messageText: String? = null,
            isVideoCall: Boolean = false
        ) {
            val intent = Intent(context, ScheduledNotificationReceiver::class.java).apply {
                putExtra(EXTRA_NOTIF_TYPE, type)
                putExtra(NotificationHelper.EXTRA_CHAT_ID, chatId)
                if (type == TYPE_MESSAGE) {
                    putExtra(EXTRA_SENDER_NAME, senderOrCallerName ?: "Elena Vance")
                    putExtra(EXTRA_MESSAGE_TEXT, messageText ?: "Direct message notification from Linko! 💬")
                } else if (type == TYPE_GROUP_MESSAGE) {
                    putExtra(EXTRA_SENDER_NAME, senderOrCallerName ?: "Design Team (Alex)")
                    putExtra(EXTRA_MESSAGE_TEXT, messageText ?: "Group discussion message from Linko! 👥")
                } else if (type == TYPE_CALL) {
                    putExtra(NotificationHelper.EXTRA_CALLER_NAME, senderOrCallerName ?: "Marcus Miller")
                    putExtra(NotificationHelper.EXTRA_CALLER_EMAIL, "marcus@linko.com")
                    putExtra(NotificationHelper.EXTRA_CALL_TYPE, if (isVideoCall) "Video" else "Voice")
                } else if (type == TYPE_FRIEND_REQUEST) {
                    putExtra(NotificationHelper.EXTRA_REQUESTER_NAME, senderOrCallerName ?: "Sophia Chen")
                    putExtra(NotificationHelper.EXTRA_REQUESTER_EMAIL, "sophia@linko.com")
                }
            }

            val requestCode = (System.currentTimeMillis() % 10000).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAtMs = System.currentTimeMillis() + (delaySeconds * 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
        }
    }
}
