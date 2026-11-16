package com.studyos.cainter.ui.timer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.studyos.cainter.R
import com.studyos.cainter.databinding.ItemSequenceTaskBinding

class SequenceTaskAdapter(
    private var tasks: List<SequenceTask>,
    private var activeIndex: Int,
    private val onTaskSelected: (Int) -> Unit,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.Adapter<SequenceTaskAdapter.ViewHolder>() {

    fun updateData(newTasks: List<SequenceTask>, newActiveIndex: Int) {
        tasks = newTasks
        activeIndex = newActiveIndex
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSequenceTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        val isActive = position == activeIndex
        val isDone = task.isCompleted

        holder.binding.tvStepNumber.text = if (isDone) "✓" else "${position + 1}"
        holder.binding.tvSequenceTaskTitle.text = task.title
        val hours = task.durationMinutes / 60
        val mins = task.durationMinutes % 60
        val durationStr = if (hours > 0 && mins > 0) "${hours}h ${mins}m" else if (hours > 0) "${hours}h" else "${mins}m"
        holder.binding.tvSequenceTaskDuration.text = "$durationStr • ${task.durationMinutes} mins"

        when {
            isDone -> {
                holder.binding.tvSequenceStatus.text = "COMPLETED"
                holder.binding.tvSequenceStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.accent_emerald))
                holder.binding.cardSequenceTask.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.divider_light)
                holder.binding.cardSequenceTask.strokeWidth = 1
                holder.binding.cardSequenceTask.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.surface_card_light))
            }
            isActive -> {
                holder.binding.tvSequenceStatus.text = "ACTIVE NOW"
                holder.binding.tvSequenceStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.primary))
                holder.binding.cardSequenceTask.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.primary)
                holder.binding.cardSequenceTask.strokeWidth = 3
                holder.binding.cardSequenceTask.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.surface_variant_light))
            }
            else -> {
                holder.binding.tvSequenceStatus.text = "QUEUED"
                holder.binding.tvSequenceStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_secondary_light))
                holder.binding.cardSequenceTask.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.divider_light)
                holder.binding.cardSequenceTask.strokeWidth = 1
                holder.binding.cardSequenceTask.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.surface_card_light))
            }
        }

        holder.binding.cardSequenceTask.setOnClickListener {
            onTaskSelected(position)
        }

        holder.binding.btnDeleteSequenceTask.setOnClickListener {
            onDeleteClicked(position)
        }
    }

    override fun getItemCount(): Int = tasks.size

    class ViewHolder(val binding: ItemSequenceTaskBinding) : RecyclerView.ViewHolder(binding.root)
}
