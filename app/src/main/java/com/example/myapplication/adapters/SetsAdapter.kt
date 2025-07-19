package com.example.myapplication.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.db.entity.Exercise
import com.example.myapplication.databinding.ItemExerciseSelectableBinding
import com.example.myapplication.databinding.ItemExerciseSetBinding
import com.example.myapplication.viewmodels.uistate.ActiveSetData
import kotlin.toString

// Type alias for our callback when set data changes
// This makes the code more readable than a long function type
typealias OnSetDataChangeListener = (position: Int, weight: String, reps: String, isCompleted: Boolean) -> Unit

class SetsAdapter(
    private val onSetDataChanged: OnSetDataChangeListener
) : ListAdapter<ActiveSetData, SetsAdapter.SetViewHolder>(SetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val binding = ItemExerciseSetBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SetViewHolder(binding, onSetDataChanged)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SetViewHolder(
        private val binding: ItemExerciseSetBinding,
        private val onDataChanged: OnSetDataChangeListener
    ) : RecyclerView.ViewHolder(binding.root) {

        // Initialize empty text watchers and listeners that will be replaced in bind()
        // This prevents memory leaks by ensuring we can clean up old listeners
        private var weightWatcher = binding.editTextKg.addTextChangedListener { }
        private var repsWatcher = binding.editTextReps.addTextChangedListener { }
        private var checkedChangeListener = binding.checkboxSetComplete.setOnCheckedChangeListener { _, _ -> }

        fun bind(set: ActiveSetData) {
            // Set the set number (e.g., "1", "2", "3")
            binding.textViewSetNumber.text = set.setNumber.toString()

            // Only update EditText if it doesn't have focus OR if the text differs
            // This prevents cursor jumping when user is typing
            if (!binding.editTextKg.hasFocus() && binding.editTextKg.text.toString() != set.weight) {
                binding.editTextKg.setText(set.weight)
            }

            if (!binding.editTextReps.hasFocus() && binding.editTextReps.text.toString() != set.reps) {
                binding.editTextReps.setText(set.reps)
            }

            // Update checkbox state
            binding.checkboxSetComplete.isChecked = set.isCompleted

            // Focus change listeners to update data when user finishes editing
            binding.editTextKg.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && adapterPosition != RecyclerView.NO_POSITION) {
                    onDataChanged(
                        adapterPosition,
                        binding.editTextKg.text.toString(),
                        binding.editTextReps.text.toString(),
                        binding.checkboxSetComplete.isChecked
                    )
                }
            }

            binding.editTextReps.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && adapterPosition != RecyclerView.NO_POSITION) {
                    onDataChanged(
                        adapterPosition,
                        binding.editTextKg.text.toString(),
                        binding.editTextReps.text.toString(),
                        binding.checkboxSetComplete.isChecked
                    )
                }
            }

            // Set up checkbox change listener
            binding.checkboxSetComplete.setOnCheckedChangeListener { _, isChecked ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDataChanged(
                        adapterPosition,
                        binding.editTextKg.text.toString(),
                        binding.editTextReps.text.toString(),
                        isChecked
                    )
                }
            }

            // Re-attach text watchers after setting initial data
            // This allows real-time updates as the user types
            weightWatcher = binding.editTextKg.addTextChangedListener { editable ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDataChanged(adapterPosition, editable.toString(), binding.editTextReps.text.toString(), binding.checkboxSetComplete.isChecked)
                }
            }

            repsWatcher = binding.editTextReps.addTextChangedListener { editable ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDataChanged(adapterPosition, binding.editTextKg.text.toString(), editable.toString(), binding.checkboxSetComplete.isChecked)
                }
            }

            checkedChangeListener = binding.checkboxSetComplete.setOnCheckedChangeListener { _, isChecked ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDataChanged(adapterPosition, binding.editTextKg.text.toString(), binding.editTextReps.text.toString(), isChecked)
                }
            }
        }
    }
}

// DiffUtil for comparing set items efficiently
class SetDiffCallback : DiffUtil.ItemCallback<ActiveSetData>() {
    override fun areItemsTheSame(oldItem: ActiveSetData, newItem: ActiveSetData): Boolean {
        // Use temporary ID to identify sets (since they might not have database IDs yet)
        return oldItem.tempId == newItem.tempId
    }

    override fun areContentsTheSame(oldItem: ActiveSetData, newItem: ActiveSetData): Boolean {
        // Compare all properties to determine if anything changed
        return oldItem == newItem
    }
}

// This adapter handles displaying a list of exercises for selection
class ExerciseAdapter(private val context: Context) :
    ListAdapter<Exercise, ExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    // Callback for when an exercise is clicked
    var onItemClick: ((Exercise) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseSelectableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = getItem(position)
        if (exercise != null) {
            holder.bind(exercise)
        }
    }

    inner class ExerciseViewHolder(private val binding: ItemExerciseSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set up click listener for the entire item
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { exercise ->
                        onItemClick?.invoke(exercise)
                    }
                }
            }
        }

        fun bind(exercise: Exercise) {
            // Set exercise name and muscle group
            binding.textViewExerciseName.text = exercise.name
            binding.textViewMuscleGroup.text = exercise.muscleGroup

            // Handle loading the exercise image with error handling
            if (!exercise.imageResourceName.isNullOrBlank()) {
                try {
                    val resourceId = context.resources.getIdentifier(
                        exercise.imageResourceName, "drawable", context.packageName
                    )
                    if (resourceId != 0) {
                        binding.imageViewExercise.setImageResource(resourceId)
                    } else {
                        Log.w("ExerciseAdapter", "Drawable resource not found: ${exercise.imageResourceName}")
                        binding.imageViewExercise.setImageResource(R.drawable.ic_placeholder_image)
                    }
                } catch (e: Exception) {
                    Log.e("ExerciseAdapter", "Error loading image: ${exercise.imageResourceName}", e)
                    binding.imageViewExercise.setImageResource(R.drawable.ic_placeholder_image)
                }
            } else {
                binding.imageViewExercise.setImageResource(R.drawable.ic_placeholder_image)
            }
        }
    }
}

// DiffUtil for comparing exercises
class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
    override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
        // Compare by ID to check if they're the same exercise
        return oldItem.exerciseId == newItem.exerciseId
    }

    override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
        // Use data class equality to check if all properties match
        return oldItem == newItem
    }
}