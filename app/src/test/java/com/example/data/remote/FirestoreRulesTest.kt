package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.entity.MessageStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreRulesTest : FirestoreEmulatorTestBase() {

    @Test
    fun userPresence_authenticatedReadAndWrite_succeeds() = runBlocking {
        val uid = signInTestUser("alice@test.com")
        val docId = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
        val payload = mapOf(
            "email" to docId,
            "userId" to uid,
            "online" to true,
            "isOnline" to true,
            "lastSeen" to System.currentTimeMillis()
        )

        withTimeout(5000L) {
            firestore.collection("user_presence").document(docId).set(payload).await()
            val snap = firestore.collection("user_presence").document(docId).get().await()
            assertTrue(snap.exists())
            assertEquals(true, snap.getBoolean("isOnline"))
        }
    }

    @Test
    fun userPresence_unauthenticatedRead_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(5000L) {
                firestore.collection("user_presence").get().await()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertNotNull(caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    @Test
    fun chatMessages_threeStageDeliveryAndRead_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice_msg@test.com")
        val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
        val chatId = "chat_${UUID.randomUUID().toString().take(8)}"

        withTimeout(5000L) {
            // Stage 1: Sent
            firestore.collection("chat_messages").document(msgId).set(
                mapOf(
                    "id" to msgId,
                    "chatId" to chatId,
                    "senderId" to aliceUid,
                    "senderName" to "Alice",
                    "senderEmail" to "alice_msg@test.com",
                    "text" to "Hello!",
                    "status" to MessageStatus.SENT
                )
            ).await()

            val sentSnap = firestore.collection("chat_messages").document(msgId).get().await()
            assertEquals(MessageStatus.SENT, sentSnap.getString("status"))
        }

        // Stage 2 & 3: Recipient marks delivered and read
        signInTestUser("bob_msg@test.com")
        withTimeout(5000L) {
            firestore.collection("chat_messages").document(msgId)
                .update("status", MessageStatus.DELIVERED).await()
            val deliveredSnap = firestore.collection("chat_messages").document(msgId).get().await()
            assertEquals(MessageStatus.DELIVERED, deliveredSnap.getString("status"))

            firestore.collection("chat_messages").document(msgId)
                .update("status", MessageStatus.READ).await()
            val readSnap = firestore.collection("chat_messages").document(msgId).get().await()
            assertEquals(MessageStatus.READ, readSnap.getString("status"))
        }
    }
}
