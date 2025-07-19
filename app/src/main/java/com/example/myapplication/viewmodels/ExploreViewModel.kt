package com.example.myapplication.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.db.dao.ExerciseDao
import com.example.myapplication.data.db.entity.Exercise

/**
 * ViewModel for the Explore screen that displays all available exercises
 *
 * @param exerciseDao Data Access Object for accessing exercise data from the database
 */
class ExploreViewModel(exerciseDao: ExerciseDao) : ViewModel() {

    // LiveData that holds and observes the complete list of exercises
    // When data changes in the database, the UI will automatically update
    val allExercises: LiveData<List<Exercise>> = exerciseDao.getAllExercises()

    // This simple ViewModel just provides access to the exercise data
    // without additional business logic or transformations
}