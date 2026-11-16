package com.studyos.cainter.ui.analytics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyos.cainter.data.local.entity.StudySession
import com.studyos.cainter.databinding.ItemTimetableScheduleBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RecentSessionsAdapter : ListAdapter<StudySession, RecentSessionsAdapter.ViewHolder>(DiffCallback) {

    private val timeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

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

        fun bind(session: StudySession) {
            val formattedDate = Instant.ofEpochMilli(session.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(timeFormatter)

            binding.tvTitle.text = session.subject
            binding.tvStartTime.text = formattedDate
            binding.tvEndTime.text = "${session.durationMinutes} min"
            binding.tvDuration.text = "LOGGED"
            binding.tvCategory.text = session.category
            binding.tvNotes.text = if (session.notes.isNotBlank()) session.notes else "Completed focus session"

            binding.btnEdit.visibility = android.view.View.GONE
            binding.btnDelete.visibility = android.view.View.GONE
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<StudySession>() {
        override fun areItemsTheSame(oldItem: StudySession, newItem: StudySession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StudySession, newItem: StudySession): Boolean {
            return oldItem == newItem
        }
    }
}
