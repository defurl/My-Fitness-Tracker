package com.example.myapplication.fragments

import android.os.Bundle
import android.util.Log // Import Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.MyApplication
import com.example.myapplication.data.datasource.SessionManager
import com.example.myapplication.data.repository.UserRepository // Import repos
import com.example.myapplication.data.repository.WorkoutRepository
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.viewmodels.HomeViewModel // Import VM & Factory
import com.example.myapplication.viewmodels.HomeViewModelFactory
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // --- ViewModel Setup ---
        val application = requireActivity().application as MyApplication
        // Create repositories (ideally injected later)
        val userRepo = UserRepository(application.database.userDao())
        val workoutRepo = WorkoutRepository(application.database.workoutDao(), application.database)
        val factory = HomeViewModelFactory(
            SessionManager(requireContext()),
            userRepo,
            workoutRepo
        )
        viewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)
        // --- End ViewModel Setup ---

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeUiState() // Observe ViewModel state
    }



    private fun setupListeners() {
        binding.buttonStartWorkout.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_active_workout)
        }
        // ... other listeners ...
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeUiState.collect { state ->
                    Log.d("HomeFragment", "New UI State: $state")
                    // Update User Name
                    binding.textViewUserName.text = if (state.isLoggedIn) state.userName ?: "User" else "Not Logged In"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding to avoid memory leaks
    }
}