package com.studyos.cainter.ui.timetable

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.databinding.ItemTimetableScheduleBinding

class TimetableAdapter(
    private val onEditClicked: (ScheduleItem) -> Unit,
    private val onDeleteClicked: (ScheduleItem) -> Unit
) : ListAdapter<ScheduleItem, TimetableAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimetableScheduleBinding.inflate(
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
        private val binding: ItemTimetableScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleItem) {
            binding.tvTitle.text = item.title

            val durationFormatted = if (item.durationMinutes >= 60) {
                val h = item.durationMinutes / 60
                val m = item.durationMinutes % 60
                if (m > 0) "${h}h ${m}m" else "${h} hr"
            } else {
                "${item.durationMinutes}m"
            }
            binding.tvStartTime.text = com.studyos.cainter.util.DateUtils.formatTo12Hour(item.startTime)
            binding.tvEndTime.text = com.studyos.cainter.util.DateUtils.formatTo12Hour(item.endTime)
            binding.tvDuration.text = durationFormatted
            binding.tvNotes.text = if (item.notes.isNotBlank()) item.notes else "No notes"

            val category = item.getCategoryEnum()
            binding.tvCategory.text = category.displayName
            try {
                val color = Color.parseColor(category.colorHex)
                binding.categoryIndicator.setBackgroundColor(color)
                binding.tvCategory.setTextColor(color)
            } catch (e: Exception) {
                // fallback
            }

            binding.btnEdit.setOnClickListener { onEditClicked(item) }
            binding.btnDelete.setOnClickListener { onDeleteClicked(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScheduleItem>() {
        override fun areItemsTheSame(oldItem: ScheduleItem, newItem: ScheduleItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScheduleItem, newItem: ScheduleItem): Boolean {
            return oldItem == newItem
        }
    }
}
