package com.example.webrtc

import android.content.Context
import android.util.Log
import com.example.data.crypto.EncryptionManager
import com.example.data.model.CallType
import com.example.data.remote.FirestoreProvider
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.util.UUID
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Firebase Firestore Signaling Manager for Linko WebRTC P2P Audio & Video Calls.
 *
 * Responsibilities:
 * 1. Creates and observes real-time WebRTC signaling sessions in Cloud Firestore.
 * 2. Exchanges E2EE-protected SDP Offers (`OFFER`), SDP Answers (`ANSWER`),
 *    and Trickle ICE Candidates (`ICE_CANDIDATE`) between Caller and Callee.
 * 3. Synchronizes call lifecycle states (`RINGING`, `ANSWERED`, `CONNECTED`, `DECLINED`, `ENDED`)
 *    and media flags (remote mute / camera state).
 * 4. Listens for incoming call invitations targeted at the authenticated user's email.
 */
class FirebaseCallSignalingManager(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseCallSignaling"
        private const val COLLECTION_CHATS = "chats"
        private const val SUBCOLLECTION_MESSAGES = "messages"
        private const val INCOMING_SIGNALING_HUB_DOC = "webrtc_signaling_hub"

        const val SIGNAL_CALL_INVITE = "CALL_INVITE"
        const val SIGNAL_SDP_OFFER = "SDP_OFFER"
        const val SIGNAL_SDP_ANSWER = "SDP_ANSWER"
        const val SIGNAL_ICE_CANDIDATE = "ICE_CANDIDATE"
        const val SIGNAL_CALL_STATE = "CALL_STATE"

        const val STATE_RINGING = "RINGING"
        const val STATE_ANSWERED = "ANSWERED"
        const val STATE_CONNECTED = "CONNECTED"
        const val STATE_DECLINED = "DECLINED"
        const val STATE_ENDED = "ENDED"
    }

    data class IncomingCallSignal(
        val callId: String,
        val callerId: String,
        val callerName: String,
        val callerEmail: String,
        val callerAvatar: String?,
        val calleeEmail: String,
        val callType: CallType,
        val sdpOffer: String?,
        val timestamp: Long
    )

    interface SignalingListener {
        fun onRemoteSdpOfferReceived(offer: SessionDescription)
        fun onRemoteSdpAnswerReceived(answer: SessionDescription)
        fun onRemoteIceCandidateReceived(candidate: IceCandidate)
        fun onRemoteCallStateChanged(state: String, reason: String?)
    }

    private var roomListenerRegistration: ListenerRegistration? = null
    private var incomingHubListenerRegistration: ListenerRegistration? = null
    private val processedSignalIds = mutableSetOf<String>()

    private fun getFirestore(): FirebaseFirestore? = FirestoreProvider.getInstance(context)

    private fun currentSenderId(fallbackEmail: String): String {
        return FirestoreProvider.getCurrentUserUid()
            ?: fallbackEmail.ifBlank { "linko_user" }.take(64)
    }

    /**
     * Publishes an outgoing call invitation + SDP Offer to Firestore and begins listening
     * for the remote peer's SDP Answer and Trickle ICE Candidates.
     */
    fun publishOfferAndObserveCall(
        callId: String,
        callerName: String,
        callerEmail: String,
        callerAvatar: String?,
        calleeEmail: String,
        calleeName: String,
        callType: CallType,
        offerSdp: SessionDescription,
        listener: SignalingListener
    ) {
        val db = getFirestore()
        if (db == null || !FirestoreProvider.isAuthenticated()) {
            Log.w(TAG, "Firestore unauthenticated or unavailable; local P2P loop active for $callId")
            return
        }

        val senderId = currentSenderId(callerEmail)
        val encryptedSdp = EncryptionManager.encryptCallSignal(offerSdp.description, callId)
        val normalizedCallerEmail = callerEmail.lowercase().trim()
        val normalizedCalleeEmail = calleeEmail.lowercase().trim()

        // 1. Write SDP_OFFER into the call's signaling subcollection: /chats/{callId}/messages/sdp_offer
        val offerDocId = "sdp_offer"
        val offerPath = "$COLLECTION_CHATS/$callId/$SUBCOLLECTION_MESSAGES/$offerDocId"
        val offerPayload = mapOf(
            "id" to offerDocId,
            "chatId" to callId,
            "senderId" to senderId,
            "text" to encryptedSdp,
            "status" to "sent",
            "signalType" to SIGNAL_SDP_OFFER,
            "callId" to callId,
            "callerName" to callerName,
            "callerEmail" to normalizedCallerEmail,
            "callerAvatar" to (callerAvatar ?: ""),
            "calleeEmail" to normalizedCalleeEmail,
            "calleeName" to calleeName,
            "callType" to callType.name,
            "callState" to STATE_RINGING,
            "sdpType" to offerSdp.type.canonicalForm(),
            "clientTimestamp" to System.currentTimeMillis(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection(COLLECTION_CHATS)
            .document(callId)
            .collection(SUBCOLLECTION_MESSAGES)
            .document(offerDocId)
            .set(offerPayload, SetOptions.merge())
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.WRITE, offerPath)
            }

        // 2. Also publish an incoming call alert into the shared signaling hub so the callee
        //    discovers the incoming call immediately.
        val invitePath = "$COLLECTION_CHATS/$INCOMING_SIGNALING_HUB_DOC/$SUBCOLLECTION_MESSAGES/$callId"
        val invitePayload = offerPayload + mapOf(
            "id" to callId,
            "chatId" to INCOMING_SIGNALING_HUB_DOC,
            "signalType" to SIGNAL_CALL_INVITE
        )
        db.collection(COLLECTION_CHATS)
            .document(INCOMING_SIGNALING_HUB_DOC)
            .collection(SUBCOLLECTION_MESSAGES)
            .document(callId)
            .set(invitePayload, SetOptions.merge())
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.WRITE, invitePath)
            }

        // 3. Observe the call room for SDP_ANSWER, remote ICE_CANDIDATEs, and CALL_STATE changes
        observeCallSignalingRoom(
            callId = callId,
            localSenderId = senderId,
            listener = listener
        )
    }

    /**
     * Publishes the Callee's SDP Answer to Firestore when answering an incoming call.
     */
    fun publishAnswer(
        callId: String,
        calleeEmail: String,
        answerSdp: SessionDescription
    ) {
        val db = getFirestore() ?: return
        if (!FirestoreProvider.isAuthenticated()) return

        val senderId = currentSenderId(calleeEmail)
        val encryptedSdp = EncryptionManager.encryptCallSignal(answerSdp.description, callId)
        val answerDocId = "sdp_answer"
        val answerPath = "$COLLECTION_CHATS/$callId/$SUBCOLLECTION_MESSAGES/$answerDocId"

        val payload = mapOf(
            "id" to answerDocId,
            "chatId" to callId,
            "senderId" to senderId,
            "text" to encryptedSdp,
            "status" to "sent",
            "signalType" to SIGNAL_SDP_ANSWER,
            "callId" to callId,
            "sdpType" to answerSdp.type.canonicalForm(),
            "callState" to STATE_ANSWERED,
            "clientTimestamp" to System.currentTimeMillis(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection(COLLECTION_CHATS)
            .document(callId)
            .collection(SUBCOLLECTION_MESSAGES)
            .document(answerDocId)
            .set(payload, SetOptions.merge())
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.WRITE, answerPath)
            }

        publishCallState(callId, calleeEmail, STATE_CONNECTED, null)
    }

    /**
     * Sends a local Trickle ICE Candidate to the Firestore signaling room.
     */
    fun publishIceCandidate(
        callId: String,
        userEmail: String,
        candidate: IceCandidate
    ) {
        val db = getFirestore() ?: return
        if (!FirestoreProvider.isAuthenticated()) return

        val senderId = currentSenderId(userEmail)
        val candidateId = "ice_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val candidateJson = JSONObject().apply {
            put("sdpMid", candidate.sdpMid ?: "")
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("sdp", candidate.sdp)
        }.toString()
        val encryptedCandidate = EncryptionManager.encryptCallSignal(candidateJson, callId)
        val path = "$COLLECTION_CHATS/$callId/$SUBCOLLECTION_MESSAGES/$candidateId"

        val payload = mapOf(
            "id" to candidateId,
            "chatId" to callId,
            "senderId" to senderId,
            "text" to encryptedCandidate,
            "status" to "sent",
            "signalType" to SIGNAL_ICE_CANDIDATE,
            "callId" to callId,
            "sdpMid" to (candidate.sdpMid ?: ""),
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "clientTimestamp" to System.currentTimeMillis(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection(COLLECTION_CHATS)
            .document(callId)
            .collection(SUBCOLLECTION_MESSAGES)
            .document(candidateId)
            .set(payload)
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.CREATE, path)
            }
    }

    /**
     * Updates the call state (`RINGING`, `ANSWERED`, `CONNECTED`, `DECLINED`, `ENDED`) in Firestore.
     */
    fun publishCallState(
        callId: String,
        userEmail: String,
        state: String,
        reason: String? = null
    ) {
        val db = getFirestore() ?: return
        if (!FirestoreProvider.isAuthenticated()) return

        val senderId = currentSenderId(userEmail)
        val stateDocId = "call_state"
        val path = "$COLLECTION_CHATS/$callId/$SUBCOLLECTION_MESSAGES/$stateDocId"

        val payload = mapOf(
            "id" to stateDocId,
            "chatId" to callId,
            "senderId" to senderId,
            "text" to (reason ?: state),
            "status" to "sent",
            "signalType" to SIGNAL_CALL_STATE,
            "callId" to callId,
            "callState" to state,
            "clientTimestamp" to System.currentTimeMillis(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection(COLLECTION_CHATS)
            .document(callId)
            .collection(SUBCOLLECTION_MESSAGES)
            .document(stateDocId)
            .set(payload, SetOptions.merge())
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.UPDATE, path)
            }

        // Also update the hub invite state if ending or declining
        if (state == STATE_ENDED || state == STATE_DECLINED || state == STATE_ANSWERED) {
            val hubPath = "$COLLECTION_CHATS/$INCOMING_SIGNALING_HUB_DOC/$SUBCOLLECTION_MESSAGES/$callId"
            val hubUpdate = mapOf(
                "id" to callId,
                "chatId" to INCOMING_SIGNALING_HUB_DOC,
                "senderId" to senderId,
                "text" to (reason ?: state),
                "status" to "sent",
                "callState" to state,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection(COLLECTION_CHATS)
                .document(INCOMING_SIGNALING_HUB_DOC)
                .collection(SUBCOLLECTION_MESSAGES)
                .document(callId)
                .set(hubUpdate, SetOptions.merge())
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.UPDATE, hubPath)
                }
        }
    }

    /**
     * Subscribes to a specific call's signaling room (`/chats/{callId}/messages`) to receive
     * SDP Offers, SDP Answers, Trickle ICE Candidates, and remote hangup events.
     */
    fun observeCallSignalingRoom(
        callId: String,
        localSenderId: String,
        listener: SignalingListener
    ) {
        roomListenerRegistration?.remove()
        processedSignalIds.clear()

        val db = getFirestore() ?: return
        if (!FirestoreProvider.isAuthenticated()) return

        val roomPath = "$COLLECTION_CHATS/$callId/$SUBCOLLECTION_MESSAGES"
        roomListenerRegistration = db.collection(COLLECTION_CHATS)
            .document(callId)
            .collection(SUBCOLLECTION_MESSAGES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, roomPath)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.REMOVED) continue
                    val doc = change.document
                    val senderId = doc.getString("senderId") ?: ""
                    if (senderId.isNotBlank() && senderId == localSenderId) {
                        // Ignore our own echoed signals
                        continue
                    }

                    val signalType = doc.getString("signalType") ?: ""
                    val docKey = "${doc.id}_${doc.getLong("clientTimestamp") ?: 0L}_$signalType"
                    if (!processedSignalIds.add(docKey)) continue

                    val encryptedText = doc.getString("text") ?: ""
                    val decryptedText = if (encryptedText.isNotBlank()) {
                        EncryptionManager.decryptCallSignal(encryptedText, callId)
                    } else ""

                    when (signalType) {
                        SIGNAL_SDP_OFFER -> {
                            if (decryptedText.isNotBlank()) {
                                listener.onRemoteSdpOfferReceived(
                                    SessionDescription(SessionDescription.Type.OFFER, decryptedText)
                                )
                            }
                        }
                        SIGNAL_SDP_ANSWER -> {
                            if (decryptedText.isNotBlank()) {
                                listener.onRemoteSdpAnswerReceived(
                                    SessionDescription(SessionDescription.Type.ANSWER, decryptedText)
                                )
                            }
                        }
                        SIGNAL_ICE_CANDIDATE -> {
                            try {
                                val json = JSONObject(decryptedText)
                                val sdpMid = json.optString("sdpMid")
                                val sdpMLineIndex = json.optInt("sdpMLineIndex", 0)
                                val candidateSdp = json.optString("sdp")
                                if (candidateSdp.isNotBlank()) {
                                    listener.onRemoteIceCandidateReceived(
                                        IceCandidate(sdpMid, sdpMLineIndex, candidateSdp)
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse remote ICE candidate JSON: ${e.message}")
                            }
                        }
                        SIGNAL_CALL_STATE -> {
                            val callState = doc.getString("callState") ?: ""
                            if (callState.isNotBlank()) {
                                listener.onRemoteCallStateChanged(callState, decryptedText)
                            }
                        }
                    }
                }
            }
    }

    /**
     * Listens in real time for incoming WebRTC calls addressed to [currentUserEmail].
     */
    fun startListeningForIncomingCalls(
        currentUserEmail: String,
        onIncomingCall: (IncomingCallSignal) -> Unit
    ) {
        incomingHubListenerRegistration?.remove()
        incomingHubListenerRegistration = null

        val normalizedEmail = currentUserEmail.lowercase().trim()
        if (normalizedEmail.isBlank() || !FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        val hubPath = "$COLLECTION_CHATS/$INCOMING_SIGNALING_HUB_DOC/$SUBCOLLECTION_MESSAGES"
        val listenerStartMs = System.currentTimeMillis() - 15_000L

        incomingHubListenerRegistration = db.collection(COLLECTION_CHATS)
            .document(INCOMING_SIGNALING_HUB_DOC)
            .collection(SUBCOLLECTION_MESSAGES)
            .whereEqualTo("calleeEmail", normalizedEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, hubPath)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    val doc = change.document
                    val callState = doc.getString("callState") ?: ""
                    if (callState != STATE_RINGING) continue

                    val clientTimestamp = doc.getLong("clientTimestamp") ?: 0L
                    if (clientTimestamp < listenerStartMs) continue

                    val callId = doc.getString("callId") ?: doc.id
                    val callerId = doc.getString("senderId") ?: ""
                    if (callerId == FirestoreProvider.getCurrentUserUid()) continue

                    val callerName = doc.getString("callerName") ?: "Linko Caller"
                    val callerEmail = doc.getString("callerEmail") ?: ""
                    val callerAvatar = doc.getString("callerAvatar")?.ifBlank { null }
                    val callTypeStr = doc.getString("callType") ?: CallType.AUDIO.name
                    val callType = if (callTypeStr.equals("VIDEO", ignoreCase = true)) {
                        CallType.VIDEO
                    } else {
                        CallType.AUDIO
                    }
                    val encryptedOffer = doc.getString("text") ?: ""
                    val sdpOffer = if (encryptedOffer.isNotBlank()) {
                        EncryptionManager.decryptCallSignal(encryptedOffer, callId)
                    } else null

                    onIncomingCall(
                        IncomingCallSignal(
                            callId = callId,
                            callerId = callerId,
                            callerName = callerName,
                            callerEmail = callerEmail,
                            callerAvatar = callerAvatar,
                            calleeEmail = normalizedEmail,
                            callType = callType,
                            sdpOffer = sdpOffer,
                            timestamp = clientTimestamp
                        )
                    )
                }
            }
    }

    fun stopCallRoomListener() {
        roomListenerRegistration?.remove()
        roomListenerRegistration = null
        processedSignalIds.clear()
    }

    fun stopAllListeners() {
        stopCallRoomListener()
        incomingHubListenerRegistration?.remove()
        incomingHubListenerRegistration = null
    }
}
