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

            if (type.isActive) {
                binding.cardScheduleType.strokeColor = ContextCompat.getColor(binding.root.context, R.color.primary)
                binding.cardScheduleType.strokeWidth = 4
                binding.cardScheduleType.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.surface_variant_light))
            } else {
                binding.cardScheduleType.strokeColor = ContextCompat.getColor(binding.root.context, R.color.divider_light)
                binding.cardScheduleType.strokeWidth = 1
                binding.cardScheduleType.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.surface_card_light))
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
