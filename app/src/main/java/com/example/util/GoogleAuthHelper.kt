package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    suspend fun signInWithGoogle(
        context: Context,
        onSuccess: (user: FirebaseUser, email: String, displayName: String, username: String) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        val activity = context as? Activity
        if (activity == null) {
            onError("Context must be an Activity to launch Google Sign-In")
            return
        }

        try {
            val serverClientId = context.getString(R.string.default_web_client_id)
            if (serverClientId.isBlank()) {
                onError("Google Web Client ID is not configured")
                return
            }

            val credentialManager = CredentialManager.create(context)
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Authenticate with Firebase Auth
                val auth = FirebaseAuth.getInstance()
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val email = firebaseUser.email ?: ""
                    val displayName = firebaseUser.displayName ?: email.substringBefore("@")
                    val username = email.substringBefore("@").replace(".", "_")

                    // Sync user profile to Firestore
                    saveUserToFirestore(context, firebaseUser, email, displayName, username)

                    onSuccess(firebaseUser, email, displayName, username)
                } else {
                    onError("Authentication failed: No user returned")
                }
            } else {
                onError("Unexpected credential type returned")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In was cancelled by user: ${e.message}")
            onCancelled()
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            onError(e.localizedMessage ?: "Google Sign-In failed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed Google Sign-In flow: ${e.message}", e)
            onError(e.localizedMessage ?: "An error occurred during Google Sign-In")
        }
    }

    private suspend fun saveUserToFirestore(
        context: Context,
        user: FirebaseUser,
        email: String,
        displayName: String,
        username: String
    ) {
        try {
            val db = com.example.data.remote.FirestoreProvider.getInstance(context) ?: return

            val userDoc = mapOf(
                "uid" to user.uid,
                "email" to email.lowercase().trim(),
                "displayName" to displayName,
                "username" to username,
                "avatarUri" to (user.photoUrl?.toString() ?: ""),
                "lastSeen" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("users").document(user.uid)
                .set(userDoc, SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully synced user ${user.uid} to Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync user to Firestore: ${e.message}")
        }
    }
}
