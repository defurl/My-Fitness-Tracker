package com.example.myapplication.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer // Import Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.adapters.ExerciseAdapter
import com.example.myapplication.databinding.FragmentAddExerciseBinding
import com.example.myapplication.data.MyApplication // Import Application class
import com.example.myapplication.viewmodels.AddExerciseViewModel
import com.example.myapplication.viewmodels.AddExerciseViewModelFactory

class AddExerciseFragment : Fragment() {

    private var _binding: FragmentAddExerciseBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddExerciseViewModel
    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- ViewModel Setup ---
        // Get the DAO via the Application context
        val application = requireActivity().application as MyApplication
        val viewModelFactory = AddExerciseViewModelFactory(application.database.exerciseDao())
        // Get the ViewModel instance
        viewModel = ViewModelProvider(this, viewModelFactory).get(AddExerciseViewModel::class.java)
        // --- End ViewModel Setup ---

        setupRecyclerView()
        observeExercises() // *** Enable observing real data ***

        binding.imageViewClose.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.imageViewFilter.setOnClickListener {
            showFilterDialog()
        }

        exerciseAdapter.onItemClick = { selectedExercise ->
            Log.d("AddExerciseFragment", "Selected Exercise: ${selectedExercise.name} (ID: ${selectedExercise.exerciseId})")
            // Send the selected exercise ID back
            findNavController().previousBackStackEntry?.savedStateHandle?.set("selectedExerciseId", selectedExercise.exerciseId)
            findNavController().popBackStack()
        }

        // --- Remove Dummy Data Loading ---
        // loadDummyData() // *** Delete or comment out this line ***
    }

    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter(requireContext())
        binding.recyclerViewExercises.apply {
            adapter = exerciseAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    // *** This function now observes data from the database via ViewModel ***
    private fun observeExercises() {
        // Observe the 'exercises' LiveData (which handles filtering)
        viewModel.exercises.observe(viewLifecycleOwner, Observer { exercisesList ->
            Log.d("AddExerciseFragment", "Observer received list. Size = ${exercisesList?.size}") // Add Log
            exerciseAdapter.submitList(exercisesList)
        })
    }

    private fun showFilterDialog() {
        val muscleGroups = arrayOf("All", "Chest", "Back", "Legs", "Shoulders", "Biceps", "Triceps", "Abs") // Add more if needed
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Muscle Group")
            .setItems(muscleGroups) { _, which ->
                val selectedGroup = if (which == 0) null else muscleGroups[which]
                // Update the filter in the ViewModel
                viewModel.setFilter(selectedGroup)
                Log.d("AddExerciseFragment", "Filter set to: $selectedGroup")
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}