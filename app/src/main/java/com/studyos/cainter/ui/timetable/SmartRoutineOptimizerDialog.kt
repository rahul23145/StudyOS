package com.studyos.cainter.ui.timetable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.databinding.DialogSmartRoutineOptimizerBinding
import com.studyos.cainter.util.DateUtils
import com.studyos.cainter.util.RoutineOptimizerEngine
import com.studyos.cainter.util.RoutineOptimizerEngine.GymShift
import com.studyos.cainter.util.RoutineOptimizerEngine.OptimizationParams
import com.studyos.cainter.util.RoutineOptimizerEngine.OptimizationResult
import com.studyos.cainter.util.RoutineOptimizerEngine.TuitionShift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmartRoutineOptimizerDialog : BottomSheetDialogFragment() {

    private var _binding: DialogSmartRoutineOptimizerBinding? = null
    private val binding get() = _binding!!

    var targetDay: Int = DateUtils.getTodayDayOfWeek()
    var scheduleTypeId: Long = 1L
    var onAppliedCallback: (() -> Unit)? = null

    private var selectedWakeTime: String = "07:00"
    private var selectedBedTime: String = "23:30"
    private var selectedGymShift: GymShift = GymShift.MORNING
    private var selectedTuitionShift: TuitionShift = TuitionShift.EVENING_6PM
    private var selectedTargetHours: Double = 11.5

    private var currentResult: OptimizationResult? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSmartRoutineOptimizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChipListeners()
        setupActionButtons()
        recalculateAndPreview()
    }

    private fun setupChipListeners() {
        // 1. Wake Time
        binding.chipGroupWakeTime.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipWake0530.id -> selectedWakeTime = "05:30"
                binding.chipWake0630.id -> selectedWakeTime = "06:30"
                binding.chipWake0700.id -> selectedWakeTime = "07:00"
                binding.chipWake0800.id -> selectedWakeTime = "08:00"
                binding.chipWake0830.id -> selectedWakeTime = "08:30"
                binding.chipWake0900.id -> selectedWakeTime = "09:00"
            }
            recalculateAndPreview()
        }

        // 2. Gym Shift
        binding.chipGroupGymShift.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipGymMorning.id -> selectedGymShift = GymShift.MORNING
                binding.chipGymEvening.id -> selectedGymShift = GymShift.EVENING
                binding.chipGymNight.id -> selectedGymShift = GymShift.NIGHT
                binding.chipGymNone.id -> selectedGymShift = GymShift.NONE
            }
            recalculateAndPreview()
        }

        // 3. Tuition Shift
        binding.chipGroupTuitionShift.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipTuitionEvening.id -> selectedTuitionShift = TuitionShift.EVENING_6PM
                binding.chipTuitionMorning8.id -> selectedTuitionShift = TuitionShift.MORNING_8AM
                binding.chipTuitionMorning10.id -> selectedTuitionShift = TuitionShift.MORNING_10AM
                binding.chipTuitionNone.id -> selectedTuitionShift = TuitionShift.NONE
            }
            recalculateAndPreview()
        }

        // 4. Study Target
        binding.chipGroupStudyTarget.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipTarget110.id -> selectedTargetHours = 11.0
                binding.chipTarget115.id -> selectedTargetHours = 11.5
                binding.chipTarget120.id -> selectedTargetHours = 12.0
            }
            recalculateAndPreview()
        }
    }

    private fun recalculateAndPreview() {
        val params = OptimizationParams(
            dayOfWeek = targetDay,
            scheduleTypeId = scheduleTypeId,
            wakeTimeStr = selectedWakeTime,
            bedTimeStr = selectedBedTime,
            gymShift = selectedGymShift,
            tuitionShift = selectedTuitionShift,
            targetStudyHours = selectedTargetHours
        )

        val result = RoutineOptimizerEngine.optimize(params)
        currentResult = result

        binding.tvLiveStudyHours.text = result.totalStudyHoursDisplay

        val breakdown = buildString {
            append("Costing: ").append(result.costingMinutes / 60).append("h ").append(result.costingMinutes % 60).append("m")
            append(" • Audit: ").append(result.auditMinutes / 60).append("h ").append(result.auditMinutes % 60).append("m")
            if (result.tuitionMinutes > 0) {
                append(" • Tuition: ").append(result.tuitionMinutes).append("m")
            }
            append(" • Practice: ").append(result.practiceMinutes / 60).append("h ").append(result.practiceMinutes % 60).append("m")
        }
        binding.tvLiveSubjectBreakdown.text = breakdown
        binding.tvLiveSleepRecovery.text = "🛌 Sleep Recovery: ${result.sleepHoursDisplay}"
    }

    private fun setupActionButtons() {
        binding.btnApplyOptimizedRoutine.setOnClickListener {
            val result = currentResult ?: return@setOnClickListener
            val app = requireActivity().application as StudyOSApplication

            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    app.scheduleRepository.replaceDaySchedule(targetDay, scheduleTypeId, result.items)
                }
                Toast.makeText(
                    requireContext(),
                    "⚡ Routine Rebalanced: ${result.totalStudyHoursDisplay} study locked in!",
                    Toast.LENGTH_LONG
                ).show()
                onAppliedCallback?.invoke()
                dismiss()
            }
        }

        binding.btnResetToStandard.setOnClickListener {
            val app = requireActivity().application as StudyOSApplication

            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    app.scheduleRepository.resetDayToDefault(targetDay, scheduleTypeId)
                }
                Toast.makeText(
                    requireContext(),
                    "Standard CA Inter routine restored.",
                    Toast.LENGTH_SHORT
                ).show()
                onAppliedCallback?.invoke()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SmartRoutineOptimizerDialog"

        fun newInstance(day: Int, typeId: Long = 1L): SmartRoutineOptimizerDialog {
            return SmartRoutineOptimizerDialog().apply {
                targetDay = day
                scheduleTypeId = typeId
            }
        }
    }
}
