package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object UserPinManager {
    private const val PREF_NAME = "linko_user_pins"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun hashPin(pin: String, salt: String): String {
        val input = "$salt:$pin:linko_secret_salt_2026"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Check if a permanent PIN has already been configured for the given email.
     */
    fun isPinSet(context: Context, email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val prefs = getPrefs(context)
        return prefs.contains("pin_hash_$cleanEmail")
    }

    /**
     * Store the hashed PIN permanently for the given email.
     */
    fun saveUserPin(context: Context, email: String, pin: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanPin = pin.trim()
        if (cleanPin.length < 4) return false

        val hashed = hashPin(cleanPin, cleanEmail)
        getPrefs(context).edit().putString("pin_hash_$cleanEmail", hashed).apply()
        return true
    }

    /**
     * Verify if the entered PIN matches the permanently saved PIN.
     */
    fun verifyUserPin(context: Context, email: String, enteredPin: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanPin = enteredPin.trim()
        val prefs = getPrefs(context)
        val savedHash = prefs.getString("pin_hash_$cleanEmail", null) ?: return false

        val computedHash = hashPin(cleanPin, cleanEmail)
        return savedHash == computedHash
    }
}
