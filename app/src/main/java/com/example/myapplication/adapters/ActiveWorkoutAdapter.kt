package com.example.myapplication.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemActiveExerciseDetailedBinding
import com.example.myapplication.viewmodels.uistate.ActiveExerciseData

// These type aliases make our callbacks more readable and self-documenting
// Using function types with clear parameter names helps understand what data is passed
typealias OnAddSetClickListener = (exerciseListPosition: Int) -> Unit
typealias OnRemoveExerciseClickListener = (exerciseListPosition: Int) -> Unit
typealias OnSetDataUpdateListener = (exerciseListPosition: Int, setPosition: Int, weight: String, reps: String, isCompleted: Boolean) -> Unit


class ActiveWorkoutAdapter(
    private val context: Context,
    private val onAddSetClicked: OnAddSetClickListener,
    private val onRemoveExerciseClicked: OnRemoveExerciseClickListener,
    private val onSetUpdated: OnSetDataUpdateListener
) : ListAdapter<ActiveExerciseData, ActiveWorkoutAdapter.ActiveExerciseViewHolder>(ActiveExerciseDiffCallback()) {

    // This viewPool is shared between all nested RecyclerViews to improve performance
    // It allows RecyclerView to reuse ViewHolders between different nested lists
    private val viewPool = RecyclerView.RecycledViewPool()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveExerciseViewHolder {
        // Create our ViewHolder using ViewBinding for type safety
        val binding = ItemActiveExerciseDetailedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ActiveExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActiveExerciseViewHolder, position: Int) {
        // Bind data at the current position to our ViewHolder
        holder.bind(getItem(position))
    }

    inner class ActiveExerciseViewHolder(private val binding: ItemActiveExerciseDetailedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // This adapter handles the nested list of sets for each exercise
        // We create it with a lambda that forwards set updates to our parent callback
        private val setsAdapter = SetsAdapter { setPosition, weight, reps, isCompleted ->
            // Only forward events if our position is valid (not being recycled)
            if (adapterPosition != RecyclerView.NO_POSITION) {
                onSetUpdated(adapterPosition, setPosition, weight, reps, isCompleted)
            }
        }

        init {
            // Set up the nested RecyclerView for exercise sets
            binding.recyclerViewSets.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = setsAdapter
                setRecycledViewPool(viewPool) // Share view pool for better performance
            }

            // Set up click listener for the "Add Set" button
            binding.buttonAddSetDetailed.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onAddSetClicked(adapterPosition)
                }
            }

            // Set up the 3-dots menu for exercise options (like remove)
            binding.buttonMenuExercise.setOnClickListener { view ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    showPopupMenu(view, adapterPosition)
                }
            }
        }

        fun bind(activeExercise: ActiveExerciseData) {
            // Set the exercise name
            binding.textViewActiveExerciseNameDetailed.text = activeExercise.exercise.name

            // Load the exercise image - handle errors gracefully with fallback to placeholder
            if (!activeExercise.exercise.imageResourceName.isNullOrBlank()) {
                try {
                    val resourceId = context.resources.getIdentifier(
                        activeExercise.exercise.imageResourceName, "drawable", context.packageName)
                    if (resourceId != 0) binding.imageViewActiveExercise.setImageResource(resourceId)
                    else binding.imageViewActiveExercise.setImageResource(R.drawable.ic_placeholder_image)
                } catch (e: Exception) {
                    Log.e("ActiveWorkoutAdapter", "Error loading image", e)
                    binding.imageViewActiveExercise.setImageResource(R.drawable.ic_placeholder_image)
                }
            } else {
                binding.imageViewActiveExercise.setImageResource(R.drawable.ic_placeholder_image)
            }

            // Update the nested adapter with the current exercise's sets
            // We pass a copy to avoid unintended side effects
            setsAdapter.submitList(activeExercise.sets.toList())
        }

        // This displays a popup menu when user taps the 3-dots menu
        private fun showPopupMenu(anchorView: View, exercisePosition: Int) {
            val popup = PopupMenu(context, anchorView)
            popup.menuInflater.inflate(R.menu.menu_active_exercise, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_remove_exercise -> {
                        // Call our callback when user selects "Remove Exercise"
                        onRemoveExerciseClicked(exercisePosition)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}

// DiffUtil improves RecyclerView performance by calculating the difference
// between two lists and only updating items that changed
class ActiveExerciseDiffCallback : DiffUtil.ItemCallback<ActiveExerciseData>() {
    override fun areItemsTheSame(oldItem: ActiveExerciseData, newItem: ActiveExerciseData): Boolean {
        // Check if items represent the same entity using their unique ID
        return oldItem.uniqueId == newItem.uniqueId
    }

    override fun areContentsTheSame(oldItem: ActiveExerciseData, newItem: ActiveExerciseData): Boolean {
        // Check if all contents are the same - using data class equals() implementation
        return oldItem == newItem
    }
}