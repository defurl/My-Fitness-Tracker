package com.example.myapplication.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Import Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import androidx.lifecycle.repeatOnLifecycle // Import repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.MyApplication // Import Application
import com.example.myapplication.data.datasource.SessionManager // Import SessionManager
import com.example.myapplication.databinding.FragmentLoginBinding
import com.example.myapplication.viewmodels.AuthResult // Import AuthResult
import com.example.myapplication.viewmodels.AuthViewModel
import com.example.myapplication.viewmodels.AuthViewModelFactory
import kotlinx.coroutines.launch // Import launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager // Get instance for factory

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        // --- ViewModel Setup ---
        val application = requireActivity().application as MyApplication
        sessionManager = SessionManager(requireContext()) // Initialize SessionManager
        val viewModelFactory = AuthViewModelFactory(application.database.userDao(), sessionManager)
        viewModel = ViewModelProvider(this, viewModelFactory).get(AuthViewModel::class.java)
        // --- End ViewModel Setup ---

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString() // No trim for password
            viewModel.login(email, password)
        }

        binding.textViewRegisterLink.setOnClickListener {
            // This line uses the correct ID defined in the nav graph
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun observeViewModel() {
        // Observe the authResult StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authResult.collect { result ->
                    // Hide loading initially
                    setLoading(false)
                    when (result) {
                        is AuthResult.Loading -> {
                            setLoading(true)
                        }
                        is AuthResult.Success -> {
                            Toast.makeText(context, result.message ?: "Login successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to the main part of the app (e.g., Home or Profile)
                            // Clear back stack up to home to prevent going back to login
                            findNavController().navigate(R.id.action_loginFragment_to_navigation_home) // Adjust action as needed
                            viewModel.resetAuthState() // Reset state after handling
                        }
                        is AuthResult.Error -> {
                            Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetAuthState() // Reset state after handling
                        }
                        is AuthResult.Idle -> {
                            // Do nothing, initial state
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        // binding.progressBar.isVisible = isLoading // Add a ProgressBar to your layout
        binding.buttonLogin.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
        binding.textViewRegisterLink.isEnabled = !isLoading // Optional: disable link too
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}