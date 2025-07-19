package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ListWorkoutHistoryBinding
import com.example.myapplication.viewmodels.uistate.WorkoutHistoryItemUiState

class WorkoutHistoryAdapter(
    private val onItemClicked: (sessionId: Long) -> Unit // Callback for when a history item is clicked
) : ListAdapter<WorkoutHistoryItemUiState, WorkoutHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        // Create our ViewHolder using ViewBinding
        val binding = ListWorkoutHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        // Bind the workout history item at this position to our ViewHolder
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        private val binding: ListWorkoutHistoryBinding,
        private val onItemClicked: (sessionId: Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        // We store the current session ID to use in the click listener
        private var currentSessionId: Long? = null

        init {
            // Set up click listener for the entire item
            binding.root.setOnClickListener {
                currentSessionId?.let { id ->
                    onItemClicked(id) // Pass the session ID to our callback
                }
            }
        }

        fun bind(item: WorkoutHistoryItemUiState) {
            // Store the session ID for the click listener
            currentSessionId = item.sessionId

            // Set the workout title (e.g., "Chest Day", "Full Body", etc.)
            binding.textViewHistoryTitle.text = item.workoutTitle

            // Set the date when the workout was performed
            binding.textViewHistoryDate.text = item.date

            // Set workout statistics
            binding.textViewHistoryDurationValue.text = item.duration
            binding.textViewHistorySetsValue.text = item.totalSets.toString()
            binding.textViewHistoryWeightValue.text = item.totalWeightLifted
        }
    }
}

// DiffUtil for comparing workout history items efficiently
class HistoryDiffCallback : DiffUtil.ItemCallback<WorkoutHistoryItemUiState>() {
    override fun areItemsTheSame(oldItem: WorkoutHistoryItemUiState, newItem: WorkoutHistoryItemUiState): Boolean {
        // Compare session IDs to determine if they're the same workout session
        return oldItem.sessionId == newItem.sessionId
    }

    override fun areContentsTheSame(oldItem: WorkoutHistoryItemUiState, newItem: WorkoutHistoryItemUiState): Boolean {
        // Compare all properties to see if anything changed
        return oldItem == newItem
    }
}