package com.example.myapplication.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class, // The reference exercise type
            parentColumns = ["exerciseId"],
            childColumns = ["workoutExerciseRefId"],
            onDelete = ForeignKey.RESTRICT // Keep history even if exercise type is deleted? Or CASCADE?
        )
    ],
    indices = [
        Index(value = ["workoutSessionId"]),
        Index(value = ["workoutExerciseRefId"])
    ]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true)
    val workoutExerciseId: Long = 0L, // Unique ID for this specific exercise instance IN the workout
    val workoutSessionId: Long,     // FK to WorkoutSession
    val workoutExerciseRefId: Long, // FK to the reference Exercise table
    val orderInWorkout: Int = 0     // Optional: maintain order
    // Add exercise-specific notes if needed later
)