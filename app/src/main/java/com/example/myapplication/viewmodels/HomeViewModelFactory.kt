package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.datasource.SessionManager
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.WorkoutRepository

/**
 * Factory for creating HomeViewModel instances with the required dependencies
 */
class HomeViewModelFactory(
    // Manages user login session data
    private val sessionManager: SessionManager,
    // Repository for accessing user information
    private val userRepository: UserRepository,
    // Repository for workout data (will be used for showing recent workouts)
    private val workoutRepository: WorkoutRepository
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the requested ViewModel
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if we're trying to create a HomeViewModel
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            // Create and return a new HomeViewModel with all required dependencies
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(sessionManager, userRepository, workoutRepository) as T
        }
        // If trying to create anything else, throw an error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}