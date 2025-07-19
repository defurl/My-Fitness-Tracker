package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.db.dao.ExerciseDao


/**
 * Factory class for creating AddExerciseViewModel instances
 *
 * In Android's ViewModel architecture, a ViewModelProvider.Factory is needed when
 * a ViewModel requires constructor parameters. This factory provides the ExerciseDao
 * dependency to the ViewModel.
 *
 * @param exerciseDao Data Access Object for accessing exercise data from the database
 */
class AddExerciseViewModelFactory(private val exerciseDao: ExerciseDao) : ViewModelProvider.Factory {
    /**
     * Creates a new instance of the AddExerciseViewModel
     *
     * @param modelClass The class of the ViewModel to create
     * @return A new instance of the requested ViewModel
     * @throws IllegalArgumentException if the requested class is not supported by this factory
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested class is AddExerciseViewModel or its subclass
        if (modelClass.isAssignableFrom(AddExerciseViewModel::class.java)) {
            // Suppress unchecked cast warning since we've verified the class type
            @Suppress("UNCHECKED_CAST")
            return AddExerciseViewModel(exerciseDao) as T
        }
        // If the requested class is not AddExerciseViewModel, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}