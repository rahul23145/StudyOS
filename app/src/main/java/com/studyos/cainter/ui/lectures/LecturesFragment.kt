package com.studyos.cainter.ui.lectures

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.BuildConfig
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.databinding.DialogAddSubjectBinding
import com.studyos.cainter.databinding.FragmentLecturesBinding
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.launch

class LecturesFragment : Fragment() {

    private var _binding: FragmentLecturesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LecturesViewModel by activityViewModels {
        val app = requireActivity().application as StudyOSApplication
        LecturesViewModel.Factory(app.studyRepository)
    }

    private var subjectAdapter: SubjectTrackerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLecturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.IS_CUSTOM_APP) {
            setupCustomAppView()
        } else {
            setupCaInterAppView()
        }

        observeTrackers()
    }

    private fun setupCustomAppView() {
        binding.tvLecturesTitle.text = "My Subjects & Targets"
        binding.tvLecturesSubtitle.text = "Track video courses, chapters & learning goals"
        binding.btnAddSubject.visibility = View.VISIBLE
        binding.rvCustomSubjects.visibility = View.VISIBLE
        binding.layoutCaInterCards.visibility = View.GONE

        subjectAdapter = SubjectTrackerAdapter(
            onIncrement = { tracker -> viewModel.incrementLecture(tracker.subject, tracker.lectureDurationMinutes) },
            onDecrement = { tracker -> viewModel.decrementLecture(tracker.subject) },
            onDelete = { tracker -> confirmDeleteSubject(tracker) }
        )
        binding.rvCustomSubjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCustomSubjects.adapter = subjectAdapter

        binding.btnAddSubject.setOnClickListener {
            showAddSubjectDialog()
        }
    }

    private fun setupCaInterAppView() {
        binding.tvLecturesTitle.text = "CA Inter Lecture Progress"
        binding.tvLecturesSubtitle.text = "2x Costing (2h18m) • 2x Audit (1h30m Mon–Sat) • Sunday Audit-Only"
        binding.btnAddSubject.visibility = View.GONE
        binding.rvCustomSubjects.visibility = View.GONE
        binding.layoutCaInterCards.visibility = View.VISIBLE

        binding.btnCostingPlus.setOnClickListener {
            viewModel.incrementLecture("COSTING", 138)
        }
        binding.btnCostingMinus.setOnClickListener {
            viewModel.decrementLecture("COSTING")
        }
        binding.btnAuditPlus.setOnClickListener {
            viewModel.incrementLecture("AUDIT", 90)
        }
        binding.btnAuditMinus.setOnClickListener {
            viewModel.decrementLecture("AUDIT")
        }
    }

    private fun showAddSubjectDialog() {
        val dialogBinding = DialogAddSubjectBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Add Subject") { _, _ ->
                val name = dialogBinding.etSubjectName.text.toString().trim()
                if (name.isBlank()) return@setPositiveButton

                val total = dialogBinding.etTotalLectures.text.toString().toIntOrNull() ?: 40
                val dailyTarget = dialogBinding.etDailyTarget.text.toString().toIntOrNull() ?: 1
                val duration = dialogBinding.etDurationMinutes.text.toString().toIntOrNull() ?: 60

                viewModel.addSubject(name, total, duration, dailyTarget)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteSubject(tracker: LectureTracker) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Subject")
            .setMessage("Are you sure you want to remove \"${tracker.displayName}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSubject(tracker.subject)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeTrackers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trackers.collect { trackers ->
                    if (BuildConfig.IS_CUSTOM_APP) {
                        subjectAdapter?.submitList(trackers)
                    } else {
                        val costing = trackers.firstOrNull { it.subject == "COSTING" }
                        val audit = trackers.firstOrNull { it.subject == "AUDIT" }

                        if (costing != null) {
                            val percentage = if (costing.totalLectures > 0) {
                                (costing.completedLectures * 100) / costing.totalLectures
                            } else 0
                            binding.tvCostingPercentage.text = "$percentage%"
                            binding.progressCosting.progress = percentage
                            val totalWatchedMins = costing.completedLectures * costing.lectureDurationMinutes
                            val hours = DateUtils.formatMinutesToHoursAndMinutes(totalWatchedMins)
                            binding.tvCostingCompletedCount.text = "${costing.completedLectures} of ${costing.totalLectures} Completed ($hours watched)"
                        }

                        if (audit != null) {
                            val percentage = if (audit.totalLectures > 0) {
                                (audit.completedLectures * 100) / audit.totalLectures
                            } else 0
                            binding.tvAuditPercentage.text = "$percentage%"
                            binding.progressAudit.progress = percentage
                            val totalWatchedMins = audit.completedLectures * audit.lectureDurationMinutes
                            val hours = DateUtils.formatMinutesToHoursAndMinutes(totalWatchedMins)
                            binding.tvAuditCompletedCount.text = "${audit.completedLectures} of ${audit.totalLectures} Completed ($hours watched)"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
