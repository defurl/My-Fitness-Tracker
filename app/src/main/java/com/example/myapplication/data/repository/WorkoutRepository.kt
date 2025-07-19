package com.example.myapplication.data.repository // Adjust package

import android.util.Log
import androidx.room.withTransaction // Import Room Transaction
import com.example.myapplication.data.db.AppDatabase // Import AppDatabase
import com.example.myapplication.data.db.dao.WorkoutDao
import com.example.myapplication.data.db.entity.ExerciseSet
import com.example.myapplication.data.db.entity.WorkoutExercise
import com.example.myapplication.data.db.entity.WorkoutSession
import com.example.myapplication.viewmodels.uistate.ActiveExerciseData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow // For history later
import kotlinx.coroutines.withContext

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val database: AppDatabase // Inject AppDatabase for transactions
) {

    // Saves a completed workout session, including exercises and sets, within a transaction.
    // Calculates total sets.
    suspend fun saveCompletedWorkout(sessionData: WorkoutSession, activeExercises: List<ActiveExerciseData>) {
        // Ensure this runs off the main thread and within a DB transaction
        withContext(Dispatchers.IO) {
            try {
                database.withTransaction { // Room's built-in transaction helper
                    // 1. Insert WorkoutSession (sessionData already has totalSets calculated by ViewModel)
                    val sessionId = workoutDao.insertWorkoutSession(sessionData)
                    Log.d("WorkoutRepo", "Inserted WorkoutSession with ID: $sessionId")

                    // 2. Loop through active exercises performed
                    activeExercises.forEachIndexed { index, activeExerciseData ->
                        // 3. Create and Insert WorkoutExercise
                        val workoutExercise = WorkoutExercise(
                            workoutSessionId = sessionId,
                            workoutExerciseRefId = activeExerciseData.exercise.exerciseId,
                            orderInWorkout = index
                        )
                        val workoutExerciseId = workoutDao.insertWorkoutExercise(workoutExercise)
                        Log.d("WorkoutRepo", "Inserted WorkoutExercise $index with ID: $workoutExerciseId (Ref: ${activeExerciseData.exercise.exerciseId})")


                        // 4. Filter, map, and create ExerciseSet entities
                        val setsToInsert = activeExerciseData.sets
                            .filter { it.reps.isNotBlank() && it.weight.isNotBlank() } // Save only non-empty sets
                            .map { setData ->
                                ExerciseSet(
                                    setWorkoutExerciseId = workoutExerciseId,
                                    setNumber = setData.setNumber,
                                    reps = setData.reps.toIntOrNull() ?: 0,
                                    weight = setData.weight.toDoubleOrNull() ?: 0.0,
                                    isCompleted = setData.isCompleted
                                )
                            }

                        // 5. Insert all valid sets for this exercise
                        if (setsToInsert.isNotEmpty()) {
                            workoutDao.insertAllExerciseSets(setsToInsert)
                            Log.d("WorkoutRepo", "Inserted ${setsToInsert.size} sets for WorkoutExercise ID: $workoutExerciseId")
                        }
                    }
                }
                Log.i("WorkoutRepo", "Workout saved successfully!")
            } catch (e: Exception) {
                Log.e("WorkoutRepo", "Error saving workout transaction", e)
                // Rethrow or handle error state propagation if needed
                throw e // Rethrow to let ViewModel know about the error
            }
        }
    }

    // Gets all workout sessions for a specific user, sorted by start time descending.
    fun getWorkoutHistory(userId: Long): Flow<List<WorkoutSession>> {
        Log.d("WorkoutRepo", "Fetching workout history for user ID: $userId")
        // Directly return the Flow from the DAO
        return workoutDao.getWorkoutSessionsForUser(userId)
    }

    // --- Function to get details (Needed Later for Total Weight) ---
    suspend fun getSetsForWorkoutSession(sessionId: Long): List<ExerciseSet> {
        return withContext(Dispatchers.IO) {
            Log.d("WorkoutRepo", "Fetching sets for session ID: $sessionId")
            workoutDao.getSetsForSession(sessionId)
        }
    }

    // Calculates the total weight lifted (sum of weight * reps) for a given session.
    suspend fun calculateTotalWeightLifted(sessionId: Long): Double {
        return withContext(Dispatchers.IO) {
            try {
                val sets = getSetsForWorkoutSession(sessionId)
                val totalWeight = sets.sumOf { set ->
                    // Ensure reps and weight are valid numbers
                    val reps = set.reps
                    val weight = set.weight
                    if (reps > 0 && weight > 0) {
                        weight * reps
                    } else {
                        0.0 // Don't count sets with 0 reps or weight
                    }
                }
                Log.d("WorkoutRepo", "Calculated total weight for session $sessionId: $totalWeight")
                totalWeight
            } catch (e: Exception) {
                Log.e("WorkoutRepo", "Error calculating total weight for session $sessionId", e)
                0.0 // Return 0.0 on error
            }
        }
    }
}