package com.example.myapplication.viewmodels.uistate

import com.example.myapplication.data.db.entity.Exercise

// Represents a single exercise set during an active workout
data class ActiveSetData(
    var setNumber: Int,                      // The position/number of this set
    var weight: String = "",                 // Weight used for this set (stored as String for direct EditText binding)
    var reps: String = "",                   // Number of repetitions performed (stored as String for direct EditText binding)
    var isCompleted: Boolean = false,        // Whether the user has marked this set as completed
    val tempId: Long = System.currentTimeMillis() // Unique identifier for list operations (RecyclerView)
)

// Represents an exercise being performed in the current workout session
data class ActiveExerciseData(
    val exercise: Exercise,                  // Reference to the base exercise definition (name, type, etc.)
    val sets: MutableList<ActiveSetData> = mutableListOf(), // All sets tracked for this exercise
    val uniqueId: Long = exercise.exerciseId // Identifier for list operations (RecyclerView)
)