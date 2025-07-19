package com.example.myapplication.viewmodels.uistate

// Represents the complete UI state for the Home screen
data class HomeUiState(
    val isLoading: Boolean = true,          // Whether data is being loaded
    val isLoggedIn: Boolean = false,        // Whether a user is currently logged in
    val userName: String? = null,           // Name of logged-in user
    val latestWorkout: LatestWorkoutUiState? = null, // Most recent workout summary
    val showWelcomeMessage: Boolean = false, // Whether to display a welcome message
    val errorMessage: String? = null         // Any error to display to the user
)

// Represents summary information about the user's most recent workout
data class LatestWorkoutUiState(
    val sessionId: Long,                     // Database ID of the workout session
    val workoutName: String?,                // Name of the workout
    val dateFormatted: String,               // Human-readable date of the workout
    val durationFormatted: String,           // Human-readable duration (e.g., "1h 30m")
    val totalSets: Int,                      // Total number of sets completed
    val exercisesPreview: List<ExercisePreviewUiState> = emptyList() // List of exercises in this workout
)

// Represents a summarized view of an exercise from a workout
data class ExercisePreviewUiState(
    val name: String,                        // Name of the exercise
    val setsDone: Int,                       // Number of sets completed
    val imageResourceName: String?           // Optional image resource for the exercise
)