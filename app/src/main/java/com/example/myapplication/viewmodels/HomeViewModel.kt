package com.example.myapplication.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.datasource.SessionManager
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.data.repository.WorkoutRepository
import com.example.myapplication.viewmodels.uistate.HomeUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * ViewModel for the Home screen that handles user state and workout information
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    // Manages user login session data
    private val sessionManager: SessionManager,
    // Repository for accessing user information
    private val userRepository: UserRepository,
    // Repository for accessing workout data (for future implementation)
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    // Simple flow that emits the current user ID (or null if not logged in)
    private val userIdFlow = flow { emit(sessionManager.getUserId()) }

    // Main UI state that the fragment observes to update the screen
    val homeUiState: StateFlow<HomeUiState> = userIdFlow
        .flatMapLatest { userId ->
            if (userId == null) {
                // User is not logged in - show logged out state
                flowOf(HomeUiState(isLoading = false, isLoggedIn = false, showWelcomeMessage = true))
            } else {
                // User is logged in - fetch their details from repository
                userRepository.getUserStream(userId).map { user ->
                    HomeUiState(
                        isLoading = false,
                        isLoggedIn = true,
                        userName = user?.name,
                        // Workout data will be added in future implementation
                        latestWorkout = null,
                        showWelcomeMessage = true
                    )
                }
            }
        }
        .onStart {
            // Show loading state while data is being fetched
            emit(HomeUiState(isLoading = true))
        }
        .catch { e ->
            // Handle errors and log them
            Log.e("HomeViewModel", "Error collecting home state", e)
            emit(HomeUiState(isLoading = false, errorMessage = e.message))
        }
        .stateIn(
            // Configure the StateFlow lifecycle
            scope = viewModelScope,
            // Only active when the UI is collecting
            started = SharingStarted.WhileSubscribed(5000),
            // Initial loading state before data arrives
            initialValue = HomeUiState(isLoading = true)
        )
}