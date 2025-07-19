package com.example.myapplication.viewmodels

import androidx.lifecycle.*
import com.example.myapplication.data.db.dao.ExerciseDao
import com.example.myapplication.data.db.entity.Exercise

/**
 * ViewModel for the Add Exercise screen that handles:
 * - Loading the complete list of available exercises from the database
 * - Filtering exercises by muscle group
 * - Providing LiveData for the UI to observe changes
 *
 * This ViewModel acts as an intermediate layer between the UI and the database,
 * ensuring data survives configuration changes like screen rotations.
 *
 * @param exerciseDao Data Access Object that provides methods to access Exercise data
 */
class AddExerciseViewModel(private val exerciseDao: ExerciseDao) : ViewModel() {

    // --- Filter Management ---

    /**
     * MutableLiveData to store the current filter value
     * Null means no filter is applied (show all exercises)
     * A string value represents the muscle group to filter by
     */
    private val _filter = MutableLiveData<String?>(null) // Current filter

    /**
     * LiveData that automatically updates when the filter changes
     * Uses switchMap to transform the filter value into a list of exercises
     * This is a reactive pattern - when filter changes, the exercise list updates automatically
     */
    val exercises: LiveData<List<Exercise>> = _filter.switchMap { filter ->
        if (filter == null) {
            // If no filter is applied, get all exercises from database
            exerciseDao.getAllExercises() // Observe all exercises
        } else {
            // If filter is applied, get only exercises for that muscle group
            exerciseDao.getExercisesByMuscleGroup(filter) // Observe filtered exercises
        }
    }

    /**
     * Alternative LiveData that always shows all exercises regardless of filter
     * Useful for screens that need the complete list without filtering
     */
    val allExercises: LiveData<List<Exercise>> = exerciseDao.getAllExercises()

    /**
     * Updates the current filter to show only exercises for a specific muscle group
     * Setting this to null clears the filter and shows all exercises
     *
     * @param muscleGroup The muscle group to filter by, or null to show all
     */
    fun setFilter(muscleGroup: String?) {
        _filter.value = muscleGroup
    }
}