package com.example.myapplication.data.db.dao

import androidx.lifecycle.LiveData // Use LiveData for simplicity now
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.db.entity.Exercise

@Dao
interface ExerciseDao {

    // Needed for pre-population
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if exercise already exists
    fun insertAllBlocking(exercises: List<Exercise>) // Use a blocking version for callback

    // Get all exercises as LiveData - UI will auto-update
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): LiveData<List<Exercise>>

    // Get filtered exercises as LiveData
    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: String): LiveData<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getExerciseById(exerciseId: Long): Exercise? // Add this
}