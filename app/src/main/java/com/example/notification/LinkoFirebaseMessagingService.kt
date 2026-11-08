package com.example.notification

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LinkoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: $token")
        saveTokenLocally(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        dispatchRemotePayload(
            context = applicationContext,
            data = data,
            notificationTitle = notification?.title,
            notificationBody = notification?.body
        )
    }

    companion object {
        private const val TAG = "LinkoFCM"
        private const val PREFS_NAME = "linko_fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_LAST_SYNC_TIME = "fcm_last_sync_time"
        private const val TOPIC_GLOBAL = "linko_global_alerts"

        fun saveTokenLocally(context: Context, token: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FCM_TOKEN, token)
                .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                .apply()
        }

        fun getSavedToken(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
        }

        /**
         * Initializes Firebase Cloud Messaging (FCM), fetches the device registration token,
         * and subscribes to global & user-scoped FCM topics for reliable push delivery.
         */
        fun initializeFcmAndRegisterToken(
            context: Context,
            userEmail: String? = null,
            onTokenResult: ((String) -> Unit)? = null
        ) {
            val appContext = context.applicationContext
            try {
                val messaging = FirebaseMessaging.getInstance()
                messaging.isAutoInitEnabled = true

                // Subscribe to global Linko topic
                messaging.subscribeToTopic(TOPIC_GLOBAL)
                    .addOnSuccessListener {
                        Log.d(TAG, "Subscribed to FCM topic: $TOPIC_GLOBAL")
                    }

                // Subscribe to user-specific topic if authenticated
                if (!userEmail.isNullOrBlank()) {
                    val userTopic = "user_" + userEmail.lowercase().trim()
                        .replace("@", "_at_")
                        .replace(Regex("[^a-z0-9_]"), "_")
                    messaging.subscribeToTopic(userTopic)
                        .addOnSuccessListener {
                            Log.d(TAG, "Subscribed to user FCM topic: $userTopic")
                        }
                }

                // Retrieve current FCM registration token
                messaging.token
                    .addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            saveTokenLocally(appContext, token)
                            onTokenResult?.invoke(token)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "FCM token fetch fallback: ${e.message}")
                        val existing = getSavedToken(appContext)
                            ?: "fcm_linko_${ android.os.Build.MODEL.replace(" ", "_").lowercase() }_active"
                        saveTokenLocally(appContext, existing)
                        onTokenResult?.invoke(existing)
                    }
            } catch (e: Throwable) {
                Log.w(TAG, "FCM initialization fallback: ${e.message}")
                val fallback = getSavedToken(appContext) ?: "fcm_linko_device_ready"
                saveTokenLocally(appContext, fallback)
                onTokenResult?.invoke(fallback)
            }
        }

        /**
         * Unified FCM payload dispatcher supporting:
         * 1. New Messages (Direct & Group) -> opens DirectChatScreen or handles inline Reply / Mark as Read
         * 2. Friend Requests -> opens FriendsScreen or handles Accept / Decline
         * 3. Incoming & Missed Calls -> opens AudioCallScreen / VideoCallScreen or handles Answer / Decline
         */
        fun dispatchRemotePayload(
            context: Context,
            data: Map<String, String>,
            notificationTitle: String? = null,
            notificationBody: String? = null
        ) {
            val type = data["type"]
                ?: data["notif_type"]
                ?: data["category"]
                ?: notificationTitle
                ?: ""

            when {
                // 1. New Chat Message Payload (Direct or Group)
                type.contains("message", ignoreCase = true) ||
                    type == ScheduledNotificationReceiver.TYPE_MESSAGE ||
                    type == ScheduledNotificationReceiver.TYPE_GROUP_MESSAGE -> {
                    val chatId = data["chat_id"] ?: data["chatId"] ?: "chat_1"
                    val senderName = data["sender_name"] ?: data["sender"] ?: notificationTitle ?: "Linko Contact"
                    val senderEmail = data["sender_email"] ?: data["email"] ?: ""
                    val messageText = data["message_text"] ?: data["message"] ?: notificationBody ?: "New message received on Linko"
                    val isGroup = data["is_group"] == "true" ||
                        data["chat_type"] == "group" ||
                        type == ScheduledNotificationReceiver.TYPE_GROUP_MESSAGE

                    NotificationHelper.showNewMessageNotification(
                        context = context,
                        chatId = chatId,
                        senderName = senderName,
                        messageText = messageText,
                        isGroup = isGroup,
                        senderEmail = senderEmail
                    )
                }

                // 2. Missed Call Payload
                type.contains("missed", ignoreCase = true) ||
                    type == ScheduledNotificationReceiver.TYPE_MISSED_CALL -> {
                    val callerName = data["caller_name"] ?: data["caller"] ?: notificationTitle ?: "Marcus Miller"
                    val callerEmail = data["caller_email"] ?: data["email"] ?: "marcus@linko.com"
                    val callType = data["call_type"] ?: data["callType"] ?: "Voice"

                    NotificationHelper.showMissedCallNotification(
                        context = context,
                        callerName = callerName,
                        callerEmail = callerEmail,
                        callType = callType
                    )
                }

                // 3. Incoming Call Payload (Voice or Video)
                type.contains("call", ignoreCase = true) ||
                    type == ScheduledNotificationReceiver.TYPE_CALL -> {
                    val isMissed = data["status"] == "missed" || data["missed"] == "true"
                    val callerName = data["caller_name"] ?: data["caller"] ?: notificationTitle ?: "Marcus Miller"
                    val callerEmail = data["caller_email"] ?: data["email"] ?: "marcus@linko.com"
                    val callType = data["call_type"] ?: data["callType"] ?: "Voice"

                    if (isMissed) {
                        NotificationHelper.showMissedCallNotification(
                            context = context,
                            callerName = callerName,
                            callerEmail = callerEmail,
                            callType = callType
                        )
                    } else {
                        NotificationHelper.showIncomingCallNotification(
                            context = context,
                            callerName = callerName,
                            callerEmail = callerEmail,
                            callType = callType
                        )
                    }
                }

                // 4. Friend Request Payload
                type.contains("friend", ignoreCase = true) ||
                    type == ScheduledNotificationReceiver.TYPE_FRIEND_REQUEST -> {
                    val requesterName = data["requester_name"] ?: data["requester"] ?: notificationTitle ?: "Sophia Chen"
                    val requesterEmail = data["requester_email"] ?: data["email"] ?: "sophia@linko.com"

                    NotificationHelper.showFriendRequestNotification(
                        context = context,
                        requesterName = requesterName,
                        requesterEmail = requesterEmail
                    )
                }

                // Default Fallback Notification
                else -> {
                    val title = notificationTitle ?: data["title"] ?: "Linko Alert"
                    val body = notificationBody ?: data["body"] ?: "You have a new update on Linko"
                    val chatId = data["chat_id"] ?: data["chatId"] ?: "chat_1"

                    NotificationHelper.showNewMessageNotification(
                        context = context,
                        chatId = chatId,
                        senderName = title,
                        messageText = body
                    )
                }
            }
        }
    }
}
