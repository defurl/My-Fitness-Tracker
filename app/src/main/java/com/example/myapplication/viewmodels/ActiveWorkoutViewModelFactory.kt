package com.example.myapplication.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.dao.ExerciseDao
import com.example.myapplication.data.datasource.SessionManager
import com.example.myapplication.data.repository.WorkoutRepository

/**
 * Factory class for creating ActiveWorkoutViewModel instances.
 *
 * In Android's ViewModel architecture, a ViewModelProvider.Factory is needed when
 * a ViewModel has constructor parameters. This factory enables dependency injection
 * by providing all required dependencies to the ViewModel.
 *
 * @param exerciseDao Data Access Object for accessing exercise data from the database
 * @param database The application's database instance - provides access to all DAOs
 * @param sessionManager Component that manages user session state (logged-in user information)
 */
class ActiveWorkoutViewModelFactory(
    private val exerciseDao: ExerciseDao,
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the requested ViewModel class.
     *
     * This method is called by the ViewModelProvider when a new ViewModel instance is needed.
     * It handles the construction of ActiveWorkoutViewModel with all required dependencies.
     *
     * @param modelClass The class of the ViewModel to create
     * @return A new instance of the requested ViewModel type
     * @throws IllegalArgumentException if the requested class is not supported by this factory
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested class is ActiveWorkoutViewModel or its subclass
        if (modelClass.isAssignableFrom(ActiveWorkoutViewModel::class.java)) {
            // Create WorkoutRepository instance using database's workoutDao
            // This follows the Repository pattern where repositories abstract data operations
            val workoutRepository = WorkoutRepository(database.workoutDao(), database)

            // Suppress unchecked cast warning - we've verified the class type above
            @Suppress("UNCHECKED_CAST")
            // Create and return a new ActiveWorkoutViewModel with all dependencies
            // The ViewModel receives:
            // 1. exerciseDao - for fetching exercise data
            // 2. workoutRepository - for saving workout data
            // 3. sessionManager - for getting current user information
            return ActiveWorkoutViewModel(exerciseDao, workoutRepository, sessionManager) as T
        }

        // If the requested class is not ActiveWorkoutViewModel, throw an exception
        // This ensures the factory is only used for creating ActiveWorkoutViewModel instances
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}