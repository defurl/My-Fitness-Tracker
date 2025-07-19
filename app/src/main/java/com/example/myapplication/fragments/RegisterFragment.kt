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
import com.example.myapplication.data.MyApplication // Import Application
import com.example.myapplication.data.datasource.SessionManager // Import SessionManager
import com.example.myapplication.databinding.FragmentRegisterBinding
import com.example.myapplication.viewmodels.AuthResult // Import AuthResult
import com.example.myapplication.viewmodels.AuthViewModel
import com.example.myapplication.viewmodels.AuthViewModelFactory
import kotlinx.coroutines.launch // Import launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager // Get instance for factory

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

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
        binding.buttonRegister.setOnClickListener {
            val name = binding.editTextNameReg.text.toString().trim()
            val email = binding.editTextEmailReg.text.toString().trim()
            val password = binding.editTextPasswordReg.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()
            viewModel.register(name, email, password, confirmPassword)
        }

        binding.textViewLoginLink.setOnClickListener {
            // Navigate back to the Login screen
            findNavController().navigateUp() // Or specific action if needed
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authResult.collect { result ->
                    setLoading(false) // Hide loading by default
                    when (result) {
                        is AuthResult.Loading -> {
                            setLoading(true)
                        }
                        is AuthResult.Success -> {
                            Toast.makeText(context, result.message ?: "Registration successful!", Toast.LENGTH_SHORT).show()
                            // Navigate back to Login screen after successful registration
                            findNavController().navigateUp()
                            viewModel.resetAuthState()
                        }
                        is AuthResult.Error -> {
                            Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetAuthState()
                        }
                        is AuthResult.Idle -> {
                            // Initial state
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        // binding.progressBarRegister.isVisible = isLoading // Add a ProgressBar to fragment_register.xml
        binding.buttonRegister.isEnabled = !isLoading
        binding.editTextNameReg.isEnabled = !isLoading
        binding.editTextEmailReg.isEnabled = !isLoading
        binding.editTextPasswordReg.isEnabled = !isLoading
        binding.editTextConfirmPassword.isEnabled = !isLoading
        binding.textViewLoginLink.isEnabled = !isLoading
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}