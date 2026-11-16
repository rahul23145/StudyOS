package com.studyos.cainter.ui.timetable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyos.cainter.R
import com.studyos.cainter.data.local.entity.ScheduleType
import com.studyos.cainter.databinding.ItemScheduleTypeBinding

class ScheduleTypeAdapter(
    private val onSelected: (ScheduleType) -> Unit
) : ListAdapter<ScheduleType, ScheduleTypeAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduleTypeBinding.inflate(
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
        private val binding: ItemScheduleTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(type: ScheduleType) {
            binding.tvTypeName.text = type.name
            binding.tvTypeDesc.text = type.description
            binding.tvTypeTarget.text = "Target: ${type.targetDailyHours}h/day • Wake ${type.wakeTime} • Sleep ${type.sleepTime}"
            binding.rbSelected.isChecked = type.isActive

            val density = binding.root.context.resources.displayMetrics.density
            val stroke2dp = (2 * density).toInt().coerceAtLeast(2)
            val stroke1dp = (1 * density).toInt().coerceAtLeast(1)

            if (type.isActive) {
                binding.cardScheduleType.strokeColor = ContextCompat.getColor(binding.root.context, R.color.primary)
                binding.cardScheduleType.strokeWidth = stroke2dp
                binding.cardScheduleType.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.surface_card_light))
                binding.tvActiveBadge.visibility = View.VISIBLE
                binding.tvTypeName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.primary))
            } else {
                binding.cardScheduleType.strokeColor = ContextCompat.getColor(binding.root.context, R.color.divider_light)
                binding.cardScheduleType.strokeWidth = stroke1dp
                binding.cardScheduleType.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.surface_card_light))
                binding.tvActiveBadge.visibility = View.GONE
                binding.tvTypeName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_primary_light))
            }

            val clickListener = View.OnClickListener {
                onSelected(type)
            }
            binding.cardScheduleType.setOnClickListener(clickListener)
            binding.rbSelected.setOnClickListener(clickListener)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScheduleType>() {
        override fun areItemsTheSame(oldItem: ScheduleType, newItem: ScheduleType): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ScheduleType, newItem: ScheduleType): Boolean = oldItem == newItem
    }
}
