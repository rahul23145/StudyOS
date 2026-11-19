package com.studyos.cainter.ui.dashboard

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyos.cainter.data.local.entity.ScheduleCategory
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.databinding.ItemTodayScheduleBinding

data class TodayScheduleUiModel(
    val item: ScheduleItem,
    val isCompleted: Boolean
)

class TodayScheduleAdapter(
    private val onToggleCompletion: (ScheduleItem, Boolean) -> Unit,
    private val onStartTimer: (ScheduleItem) -> Unit
) : ListAdapter<TodayScheduleUiModel, TodayScheduleAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodayScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTodayScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: TodayScheduleUiModel) {
            val item = model.item
            binding.tvTitle.text = item.title

            val durationFormatted = if (item.durationMinutes >= 60) {
                val h = item.durationMinutes / 60
                val m = item.durationMinutes % 60
                if (m > 0) "${h}h ${m}m" else "${h} hr"
            } else {
                "${item.durationMinutes} min"
            }
            val start12 = com.studyos.cainter.util.DateUtils.formatTo12Hour(item.startTime)
            binding.tvTime.text = "⏱️ $durationFormatted • ~$start12 (Flexible)"
            binding.tvNotes.text = if (item.notes.isNotBlank()) item.notes else "No notes"

            val category = item.getCategoryEnum()
            binding.tvCategoryBadge.text = category.displayName
            try {
                binding.tvCategoryBadge.setTextColor(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                // fallback
            }

            // Completed state & strikethrough
            binding.cbCompleted.setOnCheckedChangeListener(null)
            binding.cbCompleted.isChecked = model.isCompleted
            if (model.isCompleted) {
                binding.tvTitle.paintFlags = binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.6f
            } else {
                binding.tvTitle.paintFlags = binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }

            binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggleCompletion(item, isChecked)
            }

            binding.btnQuickTimer.setOnClickListener {
                onStartTimer(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TodayScheduleUiModel>() {
        override fun areItemsTheSame(oldItem: TodayScheduleUiModel, newItem: TodayScheduleUiModel): Boolean {
            return oldItem.item.id == newItem.item.id
        }

        override fun areContentsTheSame(oldItem: TodayScheduleUiModel, newItem: TodayScheduleUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
