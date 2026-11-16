package com.studyos.cainter.ui.lectures

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.databinding.ItemSubjectTrackerBinding
import com.studyos.cainter.util.DateUtils

class SubjectTrackerAdapter(
    private val onIncrement: (LectureTracker) -> Unit,
    private val onDecrement: (LectureTracker) -> Unit,
    private val onDelete: (LectureTracker) -> Unit
) : ListAdapter<LectureTracker, SubjectTrackerAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectTrackerBinding.inflate(
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
        private val binding: ItemSubjectTrackerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tracker: LectureTracker) {
            binding.tvSubjectTitle.text = tracker.displayName
            binding.tvSubjectTargetDesc.text = "Daily Target: ${tracker.dailyTarget} sessions • ${tracker.lectureDurationMinutes}m each"

            val percentage = if (tracker.totalLectures > 0) {
                (tracker.completedLectures * 100) / tracker.totalLectures
            } else 0

            binding.tvPercentage.text = "$percentage%"
            binding.progressSubject.progress = percentage

            val totalWatchedMins = tracker.completedLectures * tracker.lectureDurationMinutes
            val timeString = DateUtils.formatMinutesToHoursAndMinutes(totalWatchedMins)
            binding.tvCompletedCount.text = "${tracker.completedLectures} of ${tracker.totalLectures} Completed ($timeString total)"
            binding.tvCountBadge.text = "${tracker.completedLectures}"

            binding.btnPlus.setOnClickListener { onIncrement(tracker) }
            binding.btnMinus.setOnClickListener { onDecrement(tracker) }
            binding.btnDeleteSubject.setOnClickListener { onDelete(tracker) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<LectureTracker>() {
        override fun areItemsTheSame(oldItem: LectureTracker, newItem: LectureTracker): Boolean {
            return oldItem.subject == newItem.subject
        }

        override fun areContentsTheSame(oldItem: LectureTracker, newItem: LectureTracker): Boolean {
            return oldItem == newItem
        }
    }
}
