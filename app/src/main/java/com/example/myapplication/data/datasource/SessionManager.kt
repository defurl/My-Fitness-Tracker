package com.example.myapplication.data.datasource // Adjust package if needed

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "FitnessAppPrefs"
        private const val KEY_USER_ID = "user_id"
        private const val INVALID_USER_ID = -1L // Represents logged out state
    }

    // --- Session Management Methods ---
    fun saveSession(userId: Long) {
        val editor = prefs.edit()
        editor.putLong(KEY_USER_ID, userId)
        editor.apply() // Apply changes asynchronously
    }

    //  --- User ID Management Methods ---
    fun getUserId(): Long? {
        val userId = prefs.getLong(KEY_USER_ID, INVALID_USER_ID)
        return if (userId == INVALID_USER_ID) null else userId
    }

    // --- User Login State Check ---
    fun isLoggedIn(): Boolean {
        return getUserId() != null
    }


    // --- Clear Session ---
    fun clearSession() {
        val editor = prefs.edit()
        editor.remove(KEY_USER_ID)
        editor.apply()
    }
}