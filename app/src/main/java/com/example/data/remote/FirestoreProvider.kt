package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Throwable) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

object FirestoreProvider {
    private const val TAG = "FirestoreProvider"
    @Volatile
    private var cachedDbId: String? = null

    @Volatile
    private var testFirestore: FirebaseFirestore? = null

    @Volatile
    private var testAuth: FirebaseAuth? = null

    fun setTestInstances(firestore: FirebaseFirestore?, auth: FirebaseAuth? = null) {
        testFirestore = firestore
        testAuth = auth
    }

    fun isAuthenticated(): Boolean {
        return try {
            val auth = testAuth ?: FirebaseAuth.getInstance()
            auth.currentUser != null
        } catch (_: Throwable) {
            false
        }
    }

    fun getCurrentUserUid(): String? {
        return try {
            val auth = testAuth ?: FirebaseAuth.getInstance()
            auth.currentUser?.uid
        } catch (_: Throwable) {
            null
        }
    }

    fun getInstance(context: Context? = null): FirebaseFirestore? {
        testFirestore?.let { return it }
        return try {
            val resolvedContext = context ?: try {
                FirebaseApp.getInstance().applicationContext
            } catch (_: Throwable) {
                null
            } ?: return null

            val dbId = cachedDbId ?: resolvedContext.getString(R.string.firestore_database_id).also {
                cachedDbId = it
            }

            FirebaseFirestore.getInstance(dbId)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to get FirebaseFirestore: ${e.message}")
            null
        }
    }
}
