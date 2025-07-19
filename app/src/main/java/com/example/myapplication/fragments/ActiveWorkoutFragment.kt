package com.example.myapplication.fragments

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.adapters.ActiveWorkoutAdapter
import com.example.myapplication.data.MyApplication
import com.example.myapplication.data.datasource.SessionManager
import com.example.myapplication.databinding.FragmentActiveWorkoutBinding
import com.example.myapplication.viewmodels.ActiveWorkoutViewModel
import com.example.myapplication.viewmodels.ActiveWorkoutViewModelFactory
import com.example.myapplication.viewmodels.ResourceState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Date

/**
 * This Fragment manages the active workout screen where users can:
 * - See their current exercises
 * - Add new exercises
 * - Track sets, reps, and weights
 * - Monitor workout duration
 * - Save or discard the workout
 */
class ActiveWorkoutFragment : Fragment() {

    // Using ViewBinding to access layout views in a type-safe way
    // The underscore version is nullable because views are destroyed before the fragment
    private var _binding: FragmentActiveWorkoutBinding? = null
    // This is a non-null accessor that will throw an exception if accessed after onDestroyView
    // This helps catch programming errors where we try to access views when they don't exist
    private val binding get() = _binding!!

    // The ViewModel stores all our workout data and business logic
    // Using a ViewModel means our data survives configuration changes like screen rotation
    private lateinit var viewModel: ActiveWorkoutViewModel

    // The adapter connects our data to the RecyclerView
    // It's nullable because the view might be destroyed while fragment is still alive
    private var activeWorkoutAdapter: ActiveWorkoutAdapter? = null

    /**
     * Called to create the fragment's view hierarchy
     * Here we inflate our layout using ViewBinding and set up the ViewModel
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Log to help track the fragment's lifecycle - super useful for debugging!
        Log.d("AWorkout_DEBUG", "onCreateView START")

        // Inflate our XML layout using ViewBinding
        // This gives us type-safe access to all views in the layout
        _binding = FragmentActiveWorkoutBinding.inflate(inflater, container, false)
        Log.d("AWorkout_DEBUG", "Binding inflated")

        try {
            // Setting up our ViewModel is complex, so we use a try-catch to handle any errors
            // This prevents crashes if something goes wrong with database initialization
            Log.d("AWorkout_DEBUG", "ViewModel Init START")

            // Get our Application class which holds the database instance
            // Using a custom Application class is a common pattern for holding app-wide resources
            val application = requireActivity().application as MyApplication

            // Create SessionManager to get the current user's ID
            // We need this to associate the workout with the correct user
            val sessionManagerInstance = SessionManager(requireContext())

            // Get the workout repository that handles database operations
            // This abstracts away the complexity of saving workout data
            val workoutRepo = com.example.myapplication.data.repository.WorkoutRepository(
                application.database.workoutDao(), application.database
            )

            // Get the DAO for exercise-related database operations
            // DAO = Data Access Object, an interface for database operations
            val exerciseDao = application.database.exerciseDao()

            // Create our ViewModel factory with all dependencies
            // We need a factory because our ViewModel has constructor parameters
            val viewModelFactory = ActiveWorkoutViewModelFactory(
                exerciseDao,
                application.database,
                sessionManagerInstance
            )

            // Get the ViewModel instance - ViewModelProvider handles the lifecycle
            // If the fragment is recreated, we'll get the same ViewModel instance with our data
            viewModel = ViewModelProvider(this, viewModelFactory).get(ActiveWorkoutViewModel::class.java)
            Log.d("AWorkout_DEBUG", "ViewModel Init END - SUCCESS")

            // IMPORTANT: Only initialize workout state if this is a brand new fragment
            // If we're recreating after a config change, we don't want to reset workout data
            if (savedInstanceState == null) {
                viewModel.initializeWorkoutSession()
                Log.d("AWorkout_DEBUG", "Initialized VM session state")
            }

        } catch (e: Exception) {
            // Gracefully handle any initialization errors
            // This prevents the app from crashing if there's a database problem
            Log.e("AWorkout_DEBUG", "CRASH DURING VM INIT or InitSession", e)
            Toast.makeText(context,"Error initializing workout screen", Toast.LENGTH_LONG).show()
            findNavController().navigateUp() // Go back if initialization fails
        }

        Log.d("AWorkout_DEBUG", "onCreateView END")
        return binding.root
    }

    /**
     * Called when the fragment's view has been created
     * Here we set up all UI components and observers
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AWorkout_DEBUG", "onViewCreated START")

        // Set up the main UI components in the right order
        setupRecyclerView()      // First set up the list that shows exercises
        setupAndStartTimer()     // Then set up the workout duration timer

        // Set up observers to react to data changes in real-time
        observeViewModelUpdates() // Watch for exercise list changes from ViewModel
        observeExerciseSelection() // Watch for user adding new exercises
        observeSaveState()        // Watch for workout save progress/completion

        // Set up click listeners for all buttons and interactive elements
        setupClickListeners()

        Log.d("AWorkout_DEBUG", "onViewCreated END")
    }

    /**
     * Sets up the RecyclerView that displays all exercises in the current workout
     * This includes creating the adapter and connecting all callbacks
     */
    private fun setupRecyclerView() {
        Log.d("AWorkout_DEBUG", "setupRecyclerView START")

        // Only create a new adapter if one doesn't exist
        // This prevents losing scroll position and state during configuration changes
        if (activeWorkoutAdapter == null) {
            Log.d("AWorkout_DEBUG", "Initializing activeWorkoutAdapter...")

            // Create the adapter with all needed callbacks as lambda functions
            activeWorkoutAdapter = ActiveWorkoutAdapter(
                context = requireContext(),
                // This callback handles when user wants to add a new set to an exercise
                onAddSetClicked = { exerciseListPosition ->
                    Log.d("AWorkout_DEBUG", "onAddSetClicked for index: $exerciseListPosition")
                    viewModel.addSetToExercise(exerciseListPosition)
                },
                // This callback handles when user wants to remove an exercise completely
                onRemoveExerciseClicked = { exerciseListPosition ->
                    Log.d("AWorkout_DEBUG", "onRemoveExerciseClicked for index: $exerciseListPosition")
                    viewModel.removeExercise(exerciseListPosition)
                },
                // This callback is triggered whenever user edits weight, reps, or completion status
                onSetUpdated = { exerciseListPosition, setPosition, weight, reps, isCompleted ->
                    viewModel.updateSetData(exerciseListPosition, setPosition, weight, reps, isCompleted)
                }
            )
            Log.d("AWorkout_DEBUG", "activeWorkoutAdapter Initialized")
        } else {
            Log.d("AWorkout_DEBUG", "activeWorkoutAdapter ALREADY Initialized")
        }

        // Configure the RecyclerView with our adapter and layout manager
        binding.recyclerViewAddedExercises.apply {
            // Only set adapter if it's not already set (prevents unnecessary rebinding)
            if (adapter != activeWorkoutAdapter) {
                Log.d("AWorkout_DEBUG", "Setting adapter on RecyclerView")
                adapter = activeWorkoutAdapter
            }
            // Only set layout manager if it's not already set (prevents layout recalculation)
            if (layoutManager == null) {
                Log.d("AWorkout_DEBUG", "Setting LayoutManager on RecyclerView")
                layoutManager = LinearLayoutManager(requireContext())
            }
        }
        Log.d("AWorkout_DEBUG", "setupRecyclerView END")
    }

    /**
     * Sets up and starts the workout timer (chronometer)
     * This tracks how long the workout has been active
     */
    private fun setupAndStartTimer() {
        Log.d("AWorkout_DEBUG", "setupAndStartTimer CALLED")
        try {
            // Get the start time from ViewModel (stored in elapsed realtime millis)
            // Using ViewModel ensures timer continues correctly after configuration changes
            val baseTime = viewModel.getStartTimeMillis()
            if (baseTime > 0) {
                // Set chronometer base time (when workout started) and start it
                binding.chronometerWorkoutTimer.base = baseTime
                binding.chronometerWorkoutTimer.start()
                Log.d("AWorkout_TIMER", "Chronometer STARTED with base: $baseTime")
            } else {
                // Handle error case (shouldn't happen with proper ViewModel initialization)
                Log.e("AWorkout_TIMER", "Error: ViewModel start time is 0. Timer not started accurately.")
                // Fallback: use current time as base (not ideal but prevents UI issues)
                binding.chronometerWorkoutTimer.base = SystemClock.elapsedRealtime()
                binding.chronometerWorkoutTimer.start()
            }
        } catch (e: Exception) {
            // Catch any unexpected chronometer errors
            Log.e("AWorkout_TIMER", "Error accessing Chronometer", e)
            Toast.makeText(context,"Timer view error", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Observes changes to the exercise list in the ViewModel
     * When data changes, it updates the RecyclerView automatically
     */
    private fun observeViewModelUpdates() {
        // Launch a coroutine that respects the fragment lifecycle
        // Using coroutines for asynchronous data observation
        viewLifecycleOwner.lifecycleScope.launch {
            // Only observe when fragment is at least in STARTED state (visible to user)
            // This prevents unnecessary updates when fragment isn't visible
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect flow of exercise data (Kotlin Flow API)
                // Flow is like LiveData but with more flexibility for async operations
                viewModel.activeExercises.collect { exercises ->
                    Log.d("AWorkout_DEBUG", "Observer received exercise list. Size = ${exercises.size}")
                    // Check if adapter exists before updating it (fail-safe)
                    if (activeWorkoutAdapter != null) {
                        Log.d("AWorkout_DEBUG", "Adapter is initialized. Calling submitList.")
                        // Update adapter with new exercise list (DiffUtil will calculate changes)
                        activeWorkoutAdapter?.submitList(exercises)
                    } else {
                        // This is unexpected - adapter should exist when collecting flow
                        Log.e("AWorkout_DEBUG", "Observer fired BUT activeWorkoutAdapter is NULL!")
                    }
                }
            }
        }
    }

    /**
     * Observes when a new exercise is selected from the Add Exercise screen
     * Uses Navigation Component's savedStateHandle for fragment-to-fragment communication
     */
    private fun observeExerciseSelection() {
        val navController = findNavController()

        // Get current backstack entry to access its savedStateHandle
        // savedStateHandle persists across navigation events
        val currentNavEntry = navController.currentBackStackEntry

        // Observe changes to the selectedExerciseId in the savedStateHandle
        // This is set by the AddExerciseFragment when user selects an exercise
        currentNavEntry?.savedStateHandle?.getLiveData<Long>("selectedExerciseId")
            ?.observe(viewLifecycleOwner) { exerciseId ->
                Log.d("AWorkout_DEBUG", "Observed selectedExerciseId: $exerciseId")
                if (exerciseId != null && exerciseId != -1L) {
                    // Valid exercise ID received, add it to the workout
                    Log.d("AWorkout_DEBUG", "Valid exerciseId received ($exerciseId). Calling viewModel.addExerciseById...")
                    viewModel.addExerciseById(exerciseId)

                    // Remove value to prevent it from triggering again if we navigate elsewhere and back
                    // This is an important cleanup step for savedStateHandle data
                    currentNavEntry.savedStateHandle.remove<Long>("selectedExerciseId")
                    Log.d("AWorkout_DEBUG", "Removed selectedExerciseId from SavedStateHandle.")
                } else if (exerciseId != null) {
                    // Handle case of placeholder ID (-1), still need to clean up
                    currentNavEntry.savedStateHandle.remove<Long>("selectedExerciseId")
                    Log.d("AWorkout_DEBUG", "Received placeholder exerciseId ($exerciseId). Removed it.")
                }
            }
    }

    /**
     * Observes the state of saving the workout
     * Shows loading indicators, success messages, or error messages as needed
     */
    private fun observeSaveState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect the save state flow from ViewModel
                // ResourceState is a sealed class that represents Loading/Success/Error states
                viewModel.saveState.collect { state ->
                    // Check if we're currently in loading state (saving in progress)
                    val isLoading = state is ResourceState.Loading

                    // Disable finish button during saving to prevent duplicate saves
                    binding.buttonFinishWorkout.isEnabled = !isLoading

                    // Handle different states with a when expression (like switch but more powerful)
                    when (state) {
                        is ResourceState.Success -> {
                            // Show success toast message
                            Toast.makeText(context, state.message ?: "Workout Saved!", Toast.LENGTH_SHORT).show()

                            // Reset state in ViewModel so message doesn't show again on config change
                            viewModel.resetSaveState()

                            try {
                                // Navigate back to home screen
                                // The false parameter means we don't pop the home destination itself
                                findNavController().popBackStack(R.id.navigation_home, false)
                            } catch (e: Exception) {
                                Log.e("AWorkout_DEBUG", "Nav Error on Save Success", e)
                            }
                        }
                        is ResourceState.Error -> {
                            // Show error toast with the error message
                            Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetSaveState()
                        }
                        is ResourceState.Idle -> {
                            // Initial state, do nothing
                            // This is the default state before any save attempt
                        }
                        is ResourceState.Loading -> {
                            // Loading state handled by disabling button above
                            // Could also show a progress indicator here
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets up all click listeners for buttons and interactive elements
     */
    private fun setupClickListeners() {
        // Add Exercise button - navigates to exercise selection screen
        binding.buttonAddExercises.setOnClickListener {
            try {
                // Use navigation component to navigate to add exercise screen
                // The action ID is defined in the navigation graph XML
                findNavController().navigate(R.id.action_navigation_active_workout_to_navigation_add_exercise)
            } catch (e: Exception) {
                // Handle navigation errors (rare but possible)
                Log.e("AWorkout_DEBUG", "Nav Error AddExercise", e)
            }
        }

        // Finish Workout button - shows dialog to name and save workout
        binding.buttonFinishWorkout.setOnClickListener {
            Log.d("AWorkout_DEBUG", "Finish button clicked. Showing name dialog.")
            showNameWorkoutDialog()
        }

        // Discard Workout button - shows confirmation dialog
        binding.buttonDiscardWorkout.setOnClickListener {
            Log.d("AWorkout_DEBUG", "Discard Workout button CLICKED")
            showDiscardConfirmationDialog()
        }

        // Back arrow in toolbar - same as discard but accessible from top of screen
        binding.imageViewBack.setOnClickListener {
            Log.d("AWorkout_DEBUG", "Back Arrow button CLICKED")
            showDiscardConfirmationDialog()
        }
    }

    /**
     * Shows a dialog to let user name their workout before saving
     * This creates a custom dialog with text input field
     */
    private fun showNameWorkoutDialog() {
        // Create a Material Design text input field
        val inputEditText = com.google.android.material.textfield.TextInputEditText(requireContext())
        inputEditText.hint = "Workout Name (Optional)"
        inputEditText.maxLines = 1

        // Calculate padding in pixels based on device density
        // This converts dp units to actual pixels for consistent look
        val padding = (16 * resources.displayMetrics.density).toInt()

        // Create a container for the input field with proper padding
        val layout = android.widget.FrameLayout(requireContext()).apply {
            setPadding(padding, padding/2, padding, padding/2)
            addView(inputEditText)
        }

        // Create and show the dialog using Material Design AlertDialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Finish Workout")
            .setMessage("Enter a name for this workout session?")
            .setView(layout) // Add our custom input field view
            .setNegativeButton("Cancel") { dialog, _ ->
                // User cancelled, just dismiss dialog
                Log.d("AWorkout_DEBUG", "Name dialog cancelled.")
                dialog.dismiss()
            }
            .setPositiveButton("Save") { dialog, _ ->
                // Get the workout name user entered (trim whitespace)
                val workoutName = inputEditText.text?.toString()?.trim()
                Log.d("AWorkout_DEBUG", "Name dialog saved. Name: '$workoutName'")

                // Stop the timer and calculate total duration in milliseconds
                binding.chronometerWorkoutTimer.stop()
                val duration = SystemClock.elapsedRealtime() - viewModel.getStartTimeMillis()

                // Call ViewModel to save the workout with all details
                // We use null if name is empty for better database consistency
                viewModel.finishWorkout(
                    Date(),  // Current date/time as workout end time
                    duration, // Workout duration in milliseconds
                    workoutName?.ifEmpty { null } // Convert empty string to null
                )

                dialog.dismiss()
                // Our saveState observer will handle navigation after saving completes
            }
            .show()
    }

    /**
     * Shows a confirmation dialog before discarding workout
     * Prevents accidental loss of workout data
     */
    private fun showDiscardConfirmationDialog() {
        Log.d("AWorkout_DEBUG", "showDiscardConfirmationDialog CALLED")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Discard Workout?")
            .setMessage("Are you sure you want to discard this workout? Progress will not be saved.")
            .setNegativeButton("Cancel") { dialog, _ ->
                // User cancelled, just dismiss dialog
                Log.d("AWorkout_DEBUG", "Discard Dialog: CANCEL clicked")
                dialog.dismiss()
            }
            .setPositiveButton("Discard") { dialog, _ ->
                // User confirmed discard, proceed with cleanup
                Log.d("AWorkout_DEBUG", "Discard Dialog: DISCARD clicked")
                viewModel.discardWorkout() // Tell ViewModel to clean up any resources
                try {
                    // Navigate back to home screen
                    // The false parameter means we don't pop the home destination itself
                    findNavController().popBackStack(R.id.navigation_home, false)
                    Log.d("AWorkout_DEBUG", "popBackStack successful")
                } catch (e: Exception) {
                    // Handle navigation errors
                    Log.e("AWorkout_DEBUG", "Error navigating back", e)
                    Toast.makeText(context, "Error navigating back", Toast.LENGTH_SHORT).show()
                }
            }
            .setOnCancelListener {
                // Dialog dismissed by clicking outside or back button
                Log.d("AWorkout_DEBUG", "Discard Dialog: Cancelled (outside touch/back press)")
            }
            .show()
    }

    /**
     * Called when the fragment's view is being destroyed
     * Critical for cleanup to prevent memory leaks
     */
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AWorkout_Lifecycle", "onDestroyView - Stopping Chronometer")

        // Stop the timer when view is destroyed to prevent it running in background
        // Need to check if binding is null first for safety
        _binding?.chronometerWorkoutTimer?.stop()

        // CRITICAL: Set binding to null to prevent memory leaks
        // Android keeps fragment instances alive even when their views are destroyed
        // Without this, we'd hold references to destroyed views, causing memory leaks
        _binding = null
    }
}