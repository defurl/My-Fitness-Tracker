package com.example.myapplication.fragments // Adjust package

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer // Use Observer for LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager // Import GridLayoutManager
import com.example.myapplication.adapters.ExerciseAdapter // Reuse adapter
import com.example.myapplication.data.MyApplication
import com.example.myapplication.databinding.FragmentExploreBinding // Use ViewBinding
import com.example.myapplication.viewmodels.ExploreViewModel
import com.example.myapplication.viewmodels.ExploreViewModelFactory

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ExploreViewModel
    private lateinit var exerciseAdapter: ExerciseAdapter // Reuse adapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)

        // --- ViewModel Setup ---
        val application = requireActivity().application as MyApplication
        val factory = ExploreViewModelFactory(application.database.exerciseDao())
        viewModel = ViewModelProvider(this, factory).get(ExploreViewModel::class.java)
        // --- End ViewModel Setup ---

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeExercises()
    }

    private fun setupRecyclerView() {
        // Initialize adapter (pass context, no click listener needed here)
        exerciseAdapter = ExerciseAdapter(requireContext())
        binding.recyclerViewExploreExercises.apply {
            adapter = exerciseAdapter
            // Set layout manager (already set in XML but good practice)
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun observeExercises() {
        viewModel.allExercises.observe(viewLifecycleOwner, Observer { exercises ->
            // Submit list to adapter when LiveData updates
            exerciseAdapter.submitList(exercises)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}