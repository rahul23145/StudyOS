package com.studyos.cainter.ui.gym

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyos.cainter.data.local.entity.WorkoutEntry
import com.studyos.cainter.databinding.ItemWorkoutEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GymEntryAdapter(
    private val onDeleteClick: (WorkoutEntry) -> Unit
) : ListAdapter<WorkoutEntry, GymEntryAdapter.ViewHolder>(DiffCallback) {

    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    inner class ViewHolder(val binding: ItemWorkoutEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: WorkoutEntry) {
            binding.tvExerciseName.text = entry.exerciseName
            binding.tvMuscleBadge.text = entry.muscleGroup

            val weightStr = if (entry.weightKg > 0f) {
                if (entry.weightKg % 1.0f == 0.0f) "${entry.weightKg.toInt()} kg" else "${entry.weightKg} kg"
            } else {
                "Bodyweight"
            }
            binding.tvSetsRepsWeight.text = "${entry.sets} Sets × ${entry.reps} Reps • $weightStr"

            val timeStr = timeFormatter.format(Date(entry.timestamp))
            binding.tvEntryDate.text = "${entry.date} • $timeStr"

            if (entry.notes.isNotBlank()) {
                binding.tvExerciseNotes.text = entry.notes
                binding.tvExerciseNotes.visibility = View.VISIBLE
            } else {
                binding.tvExerciseNotes.visibility = View.GONE
            }

            binding.btnDeleteExercise.setOnClickListener {
                onDeleteClick(entry)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkoutEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<WorkoutEntry>() {
        override fun areItemsTheSame(oldItem: WorkoutEntry, newItem: WorkoutEntry): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: WorkoutEntry, newItem: WorkoutEntry): Boolean =
            oldItem == newItem
    }
}
