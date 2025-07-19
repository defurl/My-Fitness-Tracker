package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.db.dao.ExerciseDao

/**
 * Factory for creating ExploreViewModel instances with the required dependencies
 */
class ExploreViewModelFactory(
    // Database access object for exercise operations
    private val exerciseDao: ExerciseDao
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the requested ViewModel
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if we're trying to create an ExploreViewModel
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            // Create and return a new ExploreViewModel with the exercise data access
            @Suppress("UNCHECKED_CAST")
            return ExploreViewModel(exerciseDao) as T
        }
        // If trying to create anything else, throw an error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}