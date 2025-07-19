package com.example.myapplication.data.db.dao // Adjust package if needed

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query // Import Query
import androidx.room.Transaction
import com.example.myapplication.data.db.entity.ExerciseSet
import com.example.myapplication.data.db.entity.WorkoutExercise
import com.example.myapplication.data.db.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow // Import Flow for history

@Dao
interface WorkoutDao {

    // --- Insertion Methods ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSet(exerciseSet: ExerciseSet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExerciseSets(sets: List<ExerciseSet>)

    // Gets all workout sessions for a specific user, ordered by start time descending.
    //  Returns a Flow, so the UI can automatically update if the history changes.
    @Query("SELECT * FROM workout_sessions WHERE sessionUserId = :userId ORDER BY startTime DESC")
    fun getWorkoutSessionsForUser(userId: Long): Flow<List<WorkoutSession>>

    // Gets all exercise sets associated with a specific workout session ID.
    // This requires joining through the workout_exercises table.
    // Returns a simple List as it's typically fetched on demand for calculation.
    @Query("""
        SELECT es.* FROM exercise_sets es 
        INNER JOIN workout_exercises we ON es.setWorkoutExerciseId = we.workoutExerciseId 
        WHERE we.workoutSessionId = :sessionId
    """)
    suspend fun getSetsForSession(sessionId: Long): List<ExerciseSet> // Changed name slightly for clarity

    @Query("""
    SELECT es.* FROM exercise_sets es
    INNER JOIN workout_exercises we ON es.setWorkoutExerciseId = we.workoutExerciseId
    WHERE we.workoutSessionId = :sessionId
""")
    suspend fun getSetsForWorkoutSession(sessionId: Long): List<ExerciseSet>
}