package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.db.dao.UserDao
import com.example.myapplication.data.datasource.SessionManager
import com.example.myapplication.data.repository.WorkoutRepository

/**
 * Factory for creating ProfileViewModel instances with the required dependencies
 */
class ProfileViewModelFactory(
    // Manages user login session data
    private val sessionManager: SessionManager,
    // Repository for accessing workout data
    private val workoutRepository: WorkoutRepository,
    // Data access object for user operations in the database
    private val userDao: UserDao
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the requested ViewModel
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if we're trying to create a ProfileViewModel
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            // Create and return a new ProfileViewModel with all required dependencies
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(sessionManager, workoutRepository, userDao) as T
        }
        // If trying to create anything else, throw an error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}