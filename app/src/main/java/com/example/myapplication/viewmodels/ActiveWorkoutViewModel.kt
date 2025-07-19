package com.example.myapplication.viewmodels

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.db.dao.ExerciseDao
import com.example.myapplication.data.datasource.SessionManager
import com.example.myapplication.data.db.entity.Exercise
import com.example.myapplication.data.db.entity.ExerciseSet
import com.example.myapplication.data.db.entity.WorkoutExercise
import com.example.myapplication.data.db.entity.WorkoutSession
import com.example.myapplication.data.repository.WorkoutRepository
import com.example.myapplication.viewmodels.uistate.ActiveExerciseData
import com.example.myapplication.viewmodels.uistate.ActiveSetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * This sealed class represents different states of asynchronous operations
 * It helps track whether operations like saving a workout are in progress, succeeded, or failed
 * Using a sealed class restricts the possible states to only those defined here
 */
sealed class ResourceState {
    object Idle : ResourceState()          // Initial state - no operation running
    object Loading : ResourceState()       // Operation in progress
    data class Success(val message: String? = null) : ResourceState()  // Operation succeeded
    data class Error(val message: String) : ResourceState()            // Operation failed
}

/**
 * ViewModel that manages the state and business logic for an active workout session
 *
 * This class handles:
 * - Tracking exercises and sets in the current workout
 * - Managing the workout timer
 * - Adding/removing exercises and sets
 * - Saving the completed workout to the database
 *
 * @param exerciseDao Data Access Object for fetching exercise information from database
 * @param workoutRepository Repository for saving workout data to the database
 * @param sessionManager Manages user login state to associate workouts with users
 */
class ActiveWorkoutViewModel(
    private val exerciseDao: ExerciseDao,
    private val workoutRepository: WorkoutRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- Workout Data State ---

    // MutableStateFlow to store the list of exercises in the current workout
    // We use StateFlow instead of LiveData for better integration with coroutines
    private val _activeExercises = MutableStateFlow<List<ActiveExerciseData>>(emptyList())

    // Public immutable StateFlow that the UI can observe
    // This pattern prevents the UI from directly modifying our state
    val activeExercises: StateFlow<List<ActiveExerciseData>> = _activeExercises.asStateFlow()

    // StateFlow to track the current save operation state
    // Allows UI to show loading indicators, success messages, or error messages
    private val _saveState = MutableStateFlow<ResourceState>(ResourceState.Idle)
    val saveState: StateFlow<ResourceState> = _saveState.asStateFlow()

    // --- Timer State ---

    // Tracks when the workout started (in SystemClock.elapsedRealtime milliseconds)
    // Using SystemClock.elapsedRealtime() is better than System.currentTimeMillis()
    // because it's not affected by system time changes
    private var workoutStartTimeMillis: Long = 0L

    // Flag to prevent reinitializing an active workout
    // This is important for handling configuration changes properly
    private var isWorkoutStarted: Boolean = false

    /**
     * Initializes a new workout session if one isn't already active
     * Sets the start time and clears previous workout data
     */
    fun initializeWorkoutSession() {
        if (!isWorkoutStarted) {
            Log.d("ActiveWorkoutVM_TIMER", "Initializing NEW workout session state.")
            _activeExercises.value = emptyList()  // Clear any existing exercises
            _saveState.value = ResourceState.Idle  // Reset save state
            workoutStartTimeMillis = SystemClock.elapsedRealtime()  // Record start time
            isWorkoutStarted = true  // Mark workout as started
        } else {
            Log.d("ActiveWorkoutVM_TIMER", "Workout session already initialized.")
        }
    }

    /**
     * Returns the workout start time in elapsed realtime milliseconds
     * Used by the UI chronometer to show accurate workout duration
     */
    fun getStartTimeMillis(): Long {
        // If start time somehow wasn't set, return current time as fallback
        return if (workoutStartTimeMillis == 0L) SystemClock.elapsedRealtime() else workoutStartTimeMillis
    }

    // --- Exercise and Set Management Functions ---

    /**
     * Adds an exercise to the workout by its database ID
     * Fetches the exercise details from the database and adds it to the active exercise list
     *
     * @param exerciseId The database ID of the exercise to add
     */
    fun addExerciseById(exerciseId: Long) {
        Log.d("ActiveWorkoutVM_DEBUG", "addExerciseById called with ID: $exerciseId")

        // Launch a coroutine in the ViewModel scope
        // This ensures the operation is canceled if the ViewModel is cleared
        viewModelScope.launch {
            // Fetch exercise from database on IO thread to avoid blocking the UI
            val exercise = withContext(Dispatchers.IO) {
                Log.d("ActiveWorkoutVM_DEBUG", "Fetching exercise $exerciseId from DAO...")
                exerciseDao.getExerciseById(exerciseId)
            }

            Log.d("ActiveWorkoutVM_DEBUG", "Exercise fetched from DAO: ${exercise?.name}")

            if (exercise != null) {
                // Check if this exercise is already in the list (prevent duplicates)
                val exists = _activeExercises.value.any { it.exercise.exerciseId == exerciseId }

                if (!exists) {
                    // Create a new ActiveExerciseData with one initial set
                    val newActiveExercise = ActiveExerciseData(
                        exercise = exercise,
                        sets = mutableListOf(ActiveSetData(setNumber = 1)) // Start with set 1
                    )

                    Log.d("ActiveWorkoutVM_DEBUG", "Adding exercise ${exercise.name} to StateFlow")

                    // Add the new exercise to the current list
                    // The 'update' function lets us modify the MutableStateFlow value
                    _activeExercises.update { currentList -> currentList + newActiveExercise }
                } else {
                    Log.d("ActiveWorkoutVM_DEBUG", "Exercise ID $exerciseId already in the list.")
                }
            } else {
                Log.e("ActiveWorkoutVM_DEBUG", "Exercise with ID $exerciseId not found in DB.")
            }
        }
    }

    /**
     * Adds a new set to an exercise at the specified index
     * Automatically increments the set number
     *
     * @param exerciseListIndex The position of the exercise in the list
     */
    fun addSetToExercise(exerciseListIndex: Int) {
        Log.d("ActiveWorkoutVM", "addSetToExercise called for index: $exerciseListIndex")

        // Update the StateFlow with a new list that has the additional set
        _activeExercises.update { currentList ->
            // Validate index to prevent out of bounds errors
            if (exerciseListIndex < 0 || exerciseListIndex >= currentList.size) {
                Log.e("ActiveWorkoutVM", "addSetToExercise: Invalid index")
                return@update currentList  // Return unchanged list if index is invalid
            }

            // Map over the list, only modifying the exercise at the specified index
            currentList.mapIndexed { index, activeExercise ->
                if (index == exerciseListIndex) {
                    // Calculate the next set number (highest current set number + 1)
                    val nextSetNumber = (activeExercise.sets.maxOfOrNull { it.setNumber } ?: 0) + 1

                    Log.d("ActiveWorkoutVM", "Adding set number $nextSetNumber to exercise ID ${activeExercise.exercise.exerciseId}")

                    // Add the new set to this exercise's sets
                    val updatedSets = activeExercise.sets + ActiveSetData(setNumber = nextSetNumber)

                    // Return a copy of the exercise with updated sets
                    // We use copy() because ActiveExerciseData is a data class
                    activeExercise.copy(sets = updatedSets.toMutableList())
                } else {
                    // Return other exercises unchanged
                    activeExercise
                }
            }
        }
    }

    /**
     * Updates the data for a specific set of an exercise
     *
     * @param exerciseListIndex The position of the exercise in the list
     * @param setIndex The position of the set within the exercise
     * @param weight The weight used (as a string)
     * @param reps The number of repetitions (as a string)
     * @param isCompleted Whether the set is marked as completed
     */
    fun updateSetData(exerciseListIndex: Int, setIndex: Int, weight: String, reps: String, isCompleted: Boolean) {
        _activeExercises.update { currentList ->
            // Validate exercise index
            if (exerciseListIndex < 0 || exerciseListIndex >= currentList.size) return@update currentList

            // Map over the list, only modifying the exercise at the specified index
            currentList.mapIndexed { exIndex, activeExercise ->
                if (exIndex == exerciseListIndex) {
                    // Validate set index
                    if (setIndex < 0 || setIndex >= activeExercise.sets.size) return@mapIndexed activeExercise

                    // Map over the sets, only modifying the set at the specified index
                    val updatedSets = activeExercise.sets.mapIndexed { sIndex, currentSet ->
                        if (sIndex == setIndex) {
                            // Return a new set with updated values
                            currentSet.copy(
                                weight = weight,
                                reps = reps,
                                isCompleted = isCompleted
                            )
                        } else {
                            // Return other sets unchanged
                            currentSet
                        }
                    }

                    // Return a copy of the exercise with updated sets
                    activeExercise.copy(sets = updatedSets.toMutableList())
                } else {
                    // Return other exercises unchanged
                    activeExercise
                }
            }
        }
    }

    /**
     * Removes an exercise from the workout at the specified index
     *
     * @param exerciseListIndex The position of the exercise to remove
     */
    fun removeExercise(exerciseListIndex: Int) {
        Log.d("ActiveWorkoutVM", "removeExercise called for index: $exerciseListIndex")

        _activeExercises.update { currentList ->
            // Validate index to prevent crashes
            if (exerciseListIndex < 0 || exerciseListIndex >= currentList.size) {
                Log.e("ActiveWorkoutVM", "removeExercise: Invalid index")
                return@update currentList  // Return unchanged list if index is invalid
            }

            // Filter out the exercise at the specified index
            currentList.filterIndexed { index, _ -> index != exerciseListIndex }
        }
    }

    // --- Finish / Discard Workout ---

    /**
     * Saves the completed workout to the database
     * Creates a WorkoutSession record and associated exercise/set records
     *
     * @param endTime The time when the workout ended
     * @param durationMillis The total workout duration in milliseconds
     * @param workoutName Optional name for the workout (can be null)
     */
    fun finishWorkout(endTime: Date, durationMillis: Long, workoutName: String?) {
        // Get current user ID from session manager
        val userId = sessionManager.getUserId()

        // Validate that user is logged in and workout has been started
        if (userId == null || !isWorkoutStarted) {
            Log.e("ActiveWorkoutVM", "Cannot finish: User=$userId, Started=$isWorkoutStarted")
            _saveState.value = ResourceState.Error(if (userId == null) "User not logged in." else "Workout not started.")
            return
        }

        // Calculate workout duration from start time to current time
        val calculatedDuration = SystemClock.elapsedRealtime() - workoutStartTimeMillis

        // Basic validation to ensure duration makes sense
        if (calculatedDuration <= 0) {
            Log.e("ActiveWorkoutVM", "Cannot finish: Invalid duration calculated.")
            _saveState.value = ResourceState.Error("Invalid workout duration.")
            return
        }

        // Get current exercises list and validate it's not empty
        val currentExercisesValue = _activeExercises.value
        if (currentExercisesValue.isEmpty()) {
            Log.i("ActiveWorkoutVM", "No exercises added, workout not saved.")
            _saveState.value = ResourceState.Error("No exercises to save.")
            isWorkoutStarted = false  // Reset started flag
            return
        }

        // Count total completed sets across all exercises
        // Only count sets where both weight and reps have been entered
        val totalSetsCount = currentExercisesValue.sumOf { exerciseData ->
            exerciseData.sets.count { it.reps.isNotBlank() && it.weight.isNotBlank() }
        }

        // Create the workout session object with all metadata
        val session = WorkoutSession(
            sessionUserId = userId!!,
            startTime = Date(System.currentTimeMillis() - calculatedDuration),  // Calculate start time
            endTime = endTime,
            durationMillis = calculatedDuration,
            totalSets = totalSetsCount,
            workoutName = workoutName  // Optional user-provided name
        )

        // Update state to Loading to show progress in UI
        _saveState.value = ResourceState.Loading

        // Launch a coroutine to save the workout in background
        viewModelScope.launch {
            try {
                // Call repository to save the workout session and all its exercises/sets
                workoutRepository.saveCompletedWorkout(session, currentExercisesValue)

                // Update state to Success to show completion in UI
                _saveState.value = ResourceState.Success("Workout Saved!")

                // Reset workout started flag
                isWorkoutStarted = false
            } catch (e: Exception) {
                // Log error and update state to Error to show failure in UI
                Log.e("ActiveWorkoutVM", "Error saving workout via repository", e)
                _saveState.value = ResourceState.Error("Failed to save workout: ${e.message}")
            }
        }
    }

    /**
     * Discards the current workout without saving
     * Just resets internal state for a fresh workout next time
     */
    fun discardWorkout() {
        Log.d("ActiveWorkoutVM", "Workout explicitly discarded.")
        isWorkoutStarted = false  // Reset flag so next workout starts fresh
        // We don't need to clear _activeExercises immediately as the fragment will be destroyed
        // It will be cleared next time initializeWorkoutSession() is called
    }

    /**
     * Resets the save state back to Idle
     * Called after UI has processed success/error states
     */
    fun resetSaveState() {
        _saveState.value = ResourceState.Idle
    }

}