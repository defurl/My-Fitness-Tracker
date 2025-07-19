package com.example.myapplication.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.db.dao.UserDao
import com.example.myapplication.data.datasource.SessionManager
import com.example.myapplication.data.repository.WorkoutRepository
import com.example.myapplication.viewmodels.uistate.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel that manages data for the user profile screen
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    // Manages user login session state
    private val sessionManager: SessionManager,
    // Repository for accessing workout data
    private val workoutRepository: WorkoutRepository,
    // Data access object for user operations
    private val userDao: UserDao
) : ViewModel() {

    // Reactive flow that tracks the current user ID
    private val _userIdFlow = MutableStateFlow(sessionManager.getUserId())

    // Main UI state that the profile fragment observes to update the screen
    val profileUiState: StateFlow<ProfileUiState> = _userIdFlow
        .flatMapLatest { userId ->
            Log.d("ProfileViewModel", "User ID changed/observed: $userId")
            if (userId == null) {
                // User is not logged in - show logged out state
                flowOf(ProfileUiState(isLoggedIn = false, isLoading = false))
            } else {
                // User is logged in - combine user details and workout history
                combine(
                    // Get user information
                    userDao.getUserByIdFlow(userId),
                    // Get workout history for this user
                    workoutRepository.getWorkoutHistory(userId)
                ) { user, historySessions ->
                    Log.d(
                        "ProfileViewModel",
                        "Combining user (${user?.name}) and history (size: ${historySessions.size})"
                    )

                    // Process workout history items in parallel
                    val historyItems: List<WorkoutHistoryItemUiState> = coroutineScope {
                        // Map each session to a UI state by calculating additional data
                        val deferredItems = historySessions.map { session ->
                            async {
                                // Calculate total weight lifted for this session
                                val totalWeight = try {
                                    workoutRepository.calculateTotalWeightLifted(session.sessionId)
                                } catch (e: Exception) {
                                    Log.e(
                                        "ProfileViewModel",
                                        "Error calculating weight for session ${session.sessionId}",
                                        e
                                    )
                                    0.0 // Default on error
                                }
                                val formattedDate = formatWorkoutDate(session.startTime)

                                // Create a title for the workout
                                val title = if (!session.workoutName.isNullOrBlank()) {
                                    session.workoutName
                                } else {
                                    "Workout on $formattedDate" // Fallback title
                                }

                                // Create UI state for this history item
                                WorkoutHistoryItemUiState(
                                    sessionId = session.sessionId,
                                    workoutTitle = title.toString(),
                                    date = formattedDate,
                                    duration = formatWorkoutDuration(session.durationMillis),
                                    totalSets = session.totalSets,
                                    totalWeightLifted = formatTotalWeight(totalWeight)
                                )
                            }
                        }
                        // Wait for all calculations to complete
                        deferredItems.awaitAll()
                    }

                    // Create the complete profile UI state
                    ProfileUiState(
                        isLoading = false,
                        isLoggedIn = true,
                        userName = user?.name,
                        history = historyItems,
                        totalWorkouts = historyItems.size
                    )
                }
            }
        }
        .onStart {
            // Show loading state while data is being fetched
            emit(ProfileUiState(isLoading = true, isLoggedIn = sessionManager.isLoggedIn()))
            Log.d("ProfileViewModel", "ProfileUiState Flow started, emitting loading.")
        }
        .catch { e ->
            // Handle errors during data loading
            Log.e("ProfileViewModel", "Error collecting profile state flow", e)
            emit(
                ProfileUiState(
                    isLoading = false,
                    isLoggedIn = sessionManager.isLoggedIn(),
                    errorMessage = e.localizedMessage ?: "Failed to load profile data"
                )
            )
        }
        .stateIn(
            // Configure the StateFlow lifecycle
            scope = viewModelScope,
            // Keep active briefly after UI stops collecting
            started = SharingStarted.WhileSubscribed(5000L),
            // Initial loading state before data arrives
            initialValue = ProfileUiState(
                isLoading = true,
                isLoggedIn = sessionManager.isLoggedIn()
            )
        )

    /**
     * Logs the user out by clearing the session
     */
    fun logout() {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "logout called")
            sessionManager.clearSession()
            // Update the user ID flow to trigger UI state update
            _userIdFlow.value = null
        }
    }
}