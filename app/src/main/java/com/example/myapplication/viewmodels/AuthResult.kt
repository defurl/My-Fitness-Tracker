package com.example.myapplication.viewmodels // Adjust package if needed

/**
 * Represents the possible states/results of an authentication operation (Login/Register).
 */
sealed class AuthResult {
    object Loading : AuthResult() // Operation is in progress
    data class Success(val message: String? = null, val userId: Long? = null) : AuthResult() // Operation succeeded
    data class Error(val message: String) : AuthResult() // Operation failed
    object Idle : AuthResult() // Initial state or after consuming a result
}