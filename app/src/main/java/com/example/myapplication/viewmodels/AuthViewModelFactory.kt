package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.db.dao.UserDao
import com.example.myapplication.data.datasource.SessionManager
import android.util.Log

/**
 * Factory for creating AuthViewModel instances with the required dependencies.
 * This is needed because AuthViewModel requires parameters in its constructor.
 */
class AuthViewModelFactory(
    // Database access object for user operations
    private val userDao: UserDao,
    // Manages user login sessions
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the requested ViewModel
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Log the creation attempt for debugging
        Log.d("AuthFactory_DEBUG", "Factory create called for ${modelClass.simpleName}")

        // Check if we're trying to create an AuthViewModel
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // Create and return a new AuthViewModel with required dependencies
            @Suppress("UNCHECKED_CAST") // Safe cast - we verified the class type
            return AuthViewModel(userDao, sessionManager) as T
        }

        // If trying to create anything else, throw an error
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}