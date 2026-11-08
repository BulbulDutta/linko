package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object UserAuthManager {
    private const val PREF_NAME = "linko_auth_credentials"
    private const val KEY_LAST_EMAIL = "last_saved_email"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun hashPassword(password: String, salt: String): String {
        val input = "$salt:$password:linko_salt_v2_2026"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isUserRegistered(context: Context, email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return getPrefs(context).contains("pwd_hash_$cleanEmail") || UserPinManager.isPinSet(context, cleanEmail)
    }

    fun registerUser(
        context: Context,
        email: String,
        password: String,
        displayName: String,
        username: String
    ): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanPwd = password.trim()
        if (cleanEmail.isEmpty() || cleanPwd.length < 6) return false

        val hashed = hashPassword(cleanPwd, cleanEmail)
        getPrefs(context).edit()
            .putString("pwd_hash_$cleanEmail", hashed)
            .putString("name_$cleanEmail", displayName.trim())
            .putString("username_$cleanEmail", username.trim())
            .putString(KEY_LAST_EMAIL, cleanEmail)
            .apply()

        // Also save in UserPinManager for compatibility with legacy PIN routes
        UserPinManager.saveUserPin(context, cleanEmail, cleanPwd.take(6).padEnd(4, '0'))
        return true
    }

    fun verifyPassword(context: Context, email: String, password: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanPwd = password.trim()
        val prefs = getPrefs(context)

        val savedHash = prefs.getString("pwd_hash_$cleanEmail", null)
        if (savedHash != null) {
            val computedHash = hashPassword(cleanPwd, cleanEmail)
            return savedHash == computedHash
        }

        // Fallback check against legacy PIN store
        return UserPinManager.verifyUserPin(context, cleanEmail, cleanPwd)
    }

    fun getUserDisplayName(context: Context, email: String): String {
        val cleanEmail = email.trim().lowercase()
        val savedName = getPrefs(context).getString("name_$cleanEmail", null)
        if (!savedName.isNullOrBlank()) return savedName
        return cleanEmail.substringBefore("@").replace(".", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    fun getUserUsername(context: Context, email: String): String {
        val cleanEmail = email.trim().lowercase()
        val saved = getPrefs(context).getString("username_$cleanEmail", null)
        if (!saved.isNullOrBlank()) return saved
        return cleanEmail.substringBefore("@").replace(".", "_")
    }

    fun getLastSavedEmail(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_EMAIL, "") ?: ""
    }

    fun setLastSavedEmail(context: Context, email: String) {
        getPrefs(context).edit().putString(KEY_LAST_EMAIL, email.trim().lowercase()).apply()
    }

    fun resetPassword(context: Context, email: String, newPassword: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (newPassword.length < 6) return false
        val hashed = hashPassword(newPassword.trim(), cleanEmail)
        getPrefs(context).edit().putString("pwd_hash_$cleanEmail", hashed).apply()
        return true
    }
}
