package com.example.myapplication.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.adapters.WorkoutHistoryAdapter // Import History Adapter
import com.example.myapplication.data.MyApplication
import com.example.myapplication.data.datasource.SessionManager // Keep SessionManager if needed, or rely on VM
import com.example.myapplication.databinding.FragmentProfileBinding // Use correct binding
import com.example.myapplication.viewmodels.ProfileViewModel // Import Profile VM
import com.example.myapplication.viewmodels.ProfileViewModelFactory // Import Profile Factory
import com.example.myapplication.viewmodels.uistate.ProfileUiState // Import UI State
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private lateinit var workoutHistoryAdapter: WorkoutHistoryAdapter // Adapter for history list

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // --- ViewModel Setup ---
        val application = requireActivity().application as MyApplication
        val sessionManagerInstance = SessionManager(requireContext()) // Still needed for Factory
        val workoutRepositoryInstance = com.example.myapplication.data.repository.WorkoutRepository(
            application.database.workoutDao(), application.database
        )
        val viewModelFactory = ProfileViewModelFactory(
            sessionManagerInstance,
            workoutRepositoryInstance,
            application.database.userDao() // Pass UserDao
        )
        viewModel = ViewModelProvider(this, viewModelFactory).get(ProfileViewModel::class.java)
        // --- End ViewModel Setup ---

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners() // Setup listeners for Login/Register/Logout buttons
        observeProfileState() // Observe the combined UI state
    }

    private fun setupRecyclerView() {
        workoutHistoryAdapter = WorkoutHistoryAdapter { sessionId ->
            // TODO: Handle click on a history item (e.g., navigate to details)
            Log.d("ProfileFragment", "History item clicked: $sessionId")
            Toast.makeText(context, "Clicked workout $sessionId", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewWorkoutHistory.apply {
            adapter = workoutHistoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        // Setup buttons within the groups
        binding.buttonGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_loginFragment)
        }
        binding.buttonGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_registerFragment)
        }
        binding.buttonLogout.setOnClickListener {
            viewModel.logout() // Call logout on ViewModel
        }
    }


    private fun observeProfileState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileUiState.collect { state ->
                    Log.d("ProfileFragment", "New Profile State Received: $state")
                    // Handle Loading State
                    binding.progressBarProfile.isVisible = state.isLoading

                    // Handle Logged In vs Logged Out Views
                    binding.loggedOutContentGroup.isVisible = !state.isLoggedIn && !state.isLoading
                    binding.loggedInContentGroup.isVisible = state.isLoggedIn && !state.isLoading

                    if (state.isLoggedIn) {
                        // Update Logged In UI
                        binding.textViewProfileName.text = "Welcome, ${state.userName ?: "User"}"
                        // Update History List
                        workoutHistoryAdapter.submitList(state.history)
                        // binding.textViewWorkoutCount.text = "Total Workouts: ${state.totalWorkouts}"
                    }

                    // Handle Error Message
                    if (state.errorMessage != null) {
                        Toast.makeText(context, "Error: ${state.errorMessage}", Toast.LENGTH_LONG).show()
                        // Optionally reset error state in ViewModel after showing
                        // viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}