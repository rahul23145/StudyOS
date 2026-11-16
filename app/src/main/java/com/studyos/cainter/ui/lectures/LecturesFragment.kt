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
import com.studyos.cainter.StudyOSApplication
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

        setupButtons()
        observeTrackers()
    }

    private fun setupButtons() {
        binding.btnCostingPlus.setOnClickListener {
            viewModel.incrementLecture("COSTING")
        }
        binding.btnCostingMinus.setOnClickListener {
            viewModel.decrementLecture("COSTING")
        }
        binding.btnAuditPlus.setOnClickListener {
            viewModel.incrementLecture("AUDIT")
        }
        binding.btnAuditMinus.setOnClickListener {
            viewModel.decrementLecture("AUDIT")
        }
    }

    private fun observeTrackers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trackers.collect { trackers ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
