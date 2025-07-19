package com.example.myapplication.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutExercise::class,
        parentColumns = ["workoutExerciseId"],
        childColumns = ["setWorkoutExerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["setWorkoutExerciseId"])]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val setId: Long = 0L,
    val setWorkoutExerciseId: Long, // FK to WorkoutExercise

    val setNumber: Int,    // e.g., 1, 2, 3
    val reps: Int,         // Repetitions performed
    val weight: Double,    // Weight used (Double for kg/lbs)
    val isCompleted: Boolean = false // For the checkmark (default to false)
    // Add weightUnit: String = "kg" if needed
)