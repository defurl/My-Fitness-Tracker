package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.db.dao.UserDao
import com.example.myapplication.data.db.entity.User
import com.example.myapplication.data.datasource.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

/**
 * ViewModel that handles user authentication operations (login, register, logout)
 *
 * @param userDao Data Access Object for user database operations
 * @param sessionManager Manages user session persistence
 */
class AuthViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Stores the current authentication state (Loading, Success, Error, or Idle)
    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    // Public read-only access to authentication state for UI to observe
    val authResult: StateFlow<AuthResult> = _authResult

    /**
     * Resets authentication state back to Idle
     * Called after UI has processed success/error messages
     */
    fun resetAuthState() {
        _authResult.value = AuthResult.Idle
    }

    /**
     * Authenticates a user with email and password
     * Updates authResult with appropriate success/error state
     */
    fun login(email: String, password: String) {
        // Validate input fields aren't empty
        if (email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.Error("Email and password cannot be empty.")
            return
        }

        // Show loading state in UI
        _authResult.value = AuthResult.Loading

        // Start background processing
        viewModelScope.launch {
            try {
                // Move database operations to IO thread
                val result = withContext(Dispatchers.IO) {
                    // Try to find user with provided email
                    val user = userDao.getUserByEmail(email)

                    if (user != null) {
                        // If user exists, check password hash with BCrypt
                        if (BCrypt.checkpw(password, user.passwordHash)) {
                            // Password matches - store user session
                            sessionManager.saveSession(user.userId)
                            AuthResult.Success(message = "Login Successful", userId = user.userId)
                        } else {
                            // Password incorrect
                            AuthResult.Error("Invalid email or password.")
                        }
                    } else {
                        // User not found
                        AuthResult.Error("Invalid email or password.")
                    }
                }
                // Update UI with result
                _authResult.value = result
            } catch (e: Exception) {
                // Handle any unexpected errors
                _authResult.value = AuthResult.Error("An error occurred: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Creates a new user account
     * Validates input, hashes password, and saves to database
     */
    fun register(name: String, email: String, password: String, confirmPassword: String) {
        // Validate all input fields
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.Error("Name, email, and password cannot be empty.")
            return
        }
        // Check email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authResult.value = AuthResult.Error("Invalid email format.")
            return
        }
        // Check password length
        if (password.length < 6) {
            _authResult.value = AuthResult.Error("Password must be at least 6 characters.")
            return
        }
        // Check passwords match
        if (password != confirmPassword) {
            _authResult.value = AuthResult.Error("Passwords do not match.")
            return
        }

        // Show loading state
        _authResult.value = AuthResult.Loading

        // Start background processing
        viewModelScope.launch {
            try {
                // Check if email already exists
                val existingUser = withContext(Dispatchers.IO) { userDao.getUserByEmail(email) }
                if (existingUser != null) {
                    _authResult.value = AuthResult.Error("Email address is already registered.")
                    return@launch
                }

                // Move to IO thread for password hashing and database operations
                val result = withContext(Dispatchers.IO) {
                    // Generate secure password hash with BCrypt
                    val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

                    // Create new user object
                    val newUser = User(name = name, email = email, passwordHash = hashedPassword)

                    try {
                        // Insert user in database and get generated ID
                        val newUserId = userDao.insertUser(newUser)
                        AuthResult.Success(message = "Registration Successful!", userId = newUserId)
                    } catch (e: Exception) {
                        // Handle database errors
                        AuthResult.Error("Registration failed: ${e.localizedMessage ?: "Database error"}")
                    }
                }
                // Update UI with result
                _authResult.value = result
            } catch (e: Exception) {
                // Handle any unexpected errors
                _authResult.value = AuthResult.Error("An error occurred: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Logs out the current user by clearing session data
     */
    fun logout() {
        viewModelScope.launch {
            // Clear user session data
            sessionManager.clearSession()
            // No need to update auth state since UI will navigate away
        }
    }

    /**
     * Checks if a user is currently logged in
     * @return true if user is logged in, false otherwise
     */
    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * Gets the current logged-in user's ID
     * @return User ID if logged in, null otherwise
     */
    fun getCurrentUserId(): Long? {
        return sessionManager.getUserId()
    }
}