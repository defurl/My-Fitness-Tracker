package com.example.myapplication.viewmodels.uistate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Represents a single workout history item shown in the profile screen
data class WorkoutHistoryItemUiState(
    val sessionId: Long,                     // Database ID of the workout session
    val workoutTitle: String,                // Display title for the workout
    val date: String,                        // Formatted date string (e.g., "Apr 3, 2025")
    val duration: String,                    // Formatted duration string (e.g., "1h 15m")
    val totalSets: Int,                      // Total number of sets in this workout
    val totalWeightLifted: String            // Formatted total weight (e.g., "1250.5 kg")
)

// Represents the complete UI state for the Profile screen
data class ProfileUiState(
    val isLoading: Boolean = true,           // Whether data is being loaded
    val isLoggedIn: Boolean = false,         // Whether a user is currently logged in
    val userName: String? = null,            // Name of the logged-in user
    val history: List<WorkoutHistoryItemUiState> = emptyList(), // Workout history items
    val totalWorkouts: Int = 0,              // Total number of workouts completed
    val errorMessage: String? = null         // Any error to display to the user
)

// Converts a Date object to a readable date string
fun formatWorkoutDate(date: Date?): String {
    if (date == null) return "N/A"
    val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return format.format(date)
}

// Converts milliseconds to a human-readable duration (e.g., "1h 15m")
fun formatWorkoutDuration(durationMillis: Long?): String {
    if (durationMillis == null || durationMillis <= 0) return "--:--"

    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

    return when {
        hours > 0 -> String.format("%dh %dm", hours, minutes)
        minutes > 0 -> String.format("%dm %ds", minutes, seconds)
        seconds > 0 -> String.format("%ds", seconds)
        else -> "0s"
    }
}

// Formats a weight value with units (e.g., "1250.5 kg")
fun formatTotalWeight(weight: Double?): String {
    if (weight == null || weight <= 0) return "- kg"
    return String.format("%.1f kg", weight)
}