package com.example.data.model

import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserEntity

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val user: UserEntity) : AuthState
}

enum class CallType { AUDIO, VIDEO }

data class ActiveCallState(
    val callId: String = "",
    val contactName: String = "",
    val contactEmail: String = "",
    val contactAvatarUri: String? = null,
    val callType: CallType = CallType.AUDIO,
    val isIncoming: Boolean = false,
    val isRinging: Boolean = false,
    val isConnected: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val is1080pEnabled: Boolean = false,
    val isHdAudioActive: Boolean = true,
    val isNoiseCancellationActive: Boolean = true,
    val isRemoteVideoAvailable: Boolean = false,
    val videoResolutionLabel: String = "HD 720p • 30fps",
    val iceConnectionState: String = "NEW",
    val callDurationSeconds: Int = 0,
    val statusText: String = "Connecting..." // "Calling...", "Ringing...", "Connected", "Ended"
)

enum class AttachmentType { IMAGE, AUDIO_NOTE, DOCUMENT }

data class PendingAttachment(
    val uri: String,
    val type: AttachmentType,
    val fileName: String
)

data class EmailHeaderInfo(
    val from: String,
    val to: String,
    val subject: String,
    val messageId: String,
    val smtpRoute: String = "smtp.linko.net (TLS 1.3 encrypted)",
    val timestampFormatted: String,
    val status: String
)

data class UserStatusGroup(
    val userId: String,
    val userDisplayName: String,
    val userEmail: String,
    val userAvatarUri: String? = null,
    val isCurrentUser: Boolean = false,
    val statuses: List<com.example.data.local.entity.StatusEntity> = emptyList(),
    val latestTimestamp: Long = System.currentTimeMillis(),
    val hasUnviewed: Boolean = true
)

