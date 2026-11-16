package com.studyos.cainter.ui.timer

import android.content.res.ColorStateList
import android.graphics.Color
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
        val timeRange = if (task.startTime.isNotBlank() && task.endTime.isNotBlank()) "~${task.startTime}–${task.endTime} • " else ""
        holder.binding.tvSequenceTaskDuration.text = "$timeRange$durationStr"

        val context = holder.itemView.context
        val density = context.resources.displayMetrics.density
        val stroke2dp = (2 * density).toInt().coerceAtLeast(2)
        val stroke1dp = (1 * density).toInt().coerceAtLeast(1)

        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        val emeraldColor = ContextCompat.getColor(context, R.color.accent_emerald)
        val dividerColor = ContextCompat.getColor(context, R.color.divider_light)
        val whiteColor = ContextCompat.getColor(context, R.color.surface_card_light)
        val slateLight = ContextCompat.getColor(context, R.color.surface_variant_light)
        val textSecondary = ContextCompat.getColor(context, R.color.text_secondary_light)

        when {
            isDone -> {
                holder.binding.tvStepNumber.text = "✓"
                holder.binding.tvStepNumber.backgroundTintList = ColorStateList.valueOf(emeraldColor)
                holder.binding.tvStepNumber.setTextColor(Color.WHITE)

                holder.binding.tvSequenceStatus.text = "✓ COMPLETED"
                holder.binding.tvSequenceStatus.backgroundTintList = ColorStateList.valueOf(emeraldColor)
                holder.binding.tvSequenceStatus.setTextColor(Color.WHITE)

                holder.binding.cardSequenceTask.strokeColor = emeraldColor
                holder.binding.cardSequenceTask.strokeWidth = stroke1dp
                holder.binding.cardSequenceTask.setCardBackgroundColor(whiteColor)
            }
            isActive -> {
                holder.binding.tvStepNumber.text = "${position + 1}"
                holder.binding.tvStepNumber.backgroundTintList = ColorStateList.valueOf(primaryColor)
                holder.binding.tvStepNumber.setTextColor(Color.WHITE)

                holder.binding.tvSequenceStatus.text = "▶ RUNNING NOW"
                holder.binding.tvSequenceStatus.backgroundTintList = ColorStateList.valueOf(primaryColor)
                holder.binding.tvSequenceStatus.setTextColor(Color.WHITE)

                holder.binding.cardSequenceTask.strokeColor = primaryColor
                holder.binding.cardSequenceTask.strokeWidth = stroke2dp
                holder.binding.cardSequenceTask.setCardBackgroundColor(whiteColor)
            }
            else -> {
                holder.binding.tvStepNumber.text = "${position + 1}"
                holder.binding.tvStepNumber.backgroundTintList = ColorStateList.valueOf(slateLight)
                holder.binding.tvStepNumber.setTextColor(textSecondary)

                holder.binding.tvSequenceStatus.text = "QUEUED"
                holder.binding.tvSequenceStatus.backgroundTintList = ColorStateList.valueOf(slateLight)
                holder.binding.tvSequenceStatus.setTextColor(textSecondary)

                holder.binding.cardSequenceTask.strokeColor = dividerColor
                holder.binding.cardSequenceTask.strokeWidth = stroke1dp
                holder.binding.cardSequenceTask.setCardBackgroundColor(whiteColor)
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
