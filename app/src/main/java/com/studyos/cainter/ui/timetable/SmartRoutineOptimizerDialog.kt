package com.studyos.cainter.ui.timetable

import android.app.TimePickerDialog
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
import com.studyos.cainter.util.RoutineOptimizerEngine.CustomOptimizationParams
import com.studyos.cainter.util.RoutineOptimizerEngine.OptimizationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SmartRoutineOptimizerDialog : BottomSheetDialogFragment() {

    private var _binding: DialogSmartRoutineOptimizerBinding? = null
    private val binding get() = _binding!!

    var targetDay: Int = DateUtils.getTodayDayOfWeek()
    var scheduleTypeId: Long = 1L
    var onAppliedCallback: (() -> Unit)? = null

    // User-entered customizable times
    private var selectedWakeTime: String = "07:00"
    private var hasGym: Boolean = true
    private var selectedGymStartTime: String = "07:15"
    private var selectedGymDuration: Int = 60
    private var hasTuition: Boolean = true
    private var selectedTuitionStartTime: String = "18:00"
    private var selectedTuitionDuration: Int = 60
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

        initializeDefaultsForDay()

        setupWakeTimeControls()
        setupGymControls()
        setupTuitionControls()
        setupStudyTargetControls()
        setupActionButtons()

        updateButtonLabels()
        recalculateAndPreview()
    }

    private fun initializeDefaultsForDay() {
        val isTtss = targetDay in listOf(2, 4, 6, 7)
        if (isTtss) {
            hasTuition = false
            selectedTuitionStartTime = "11:00"
            binding.switchTuitionToday.isChecked = false
            binding.layoutTuitionDetails.visibility = View.GONE
            binding.tvTuitionSubtitle.text = "Off on TTSS (Tue/Thu/Sat/Sun). Turn ON if you have extra tuition."
            binding.btnResetToStandard.text = "Standard TTSS"
        } else {
            hasTuition = true
            selectedTuitionStartTime = "18:00"
            binding.switchTuitionToday.isChecked = true
            binding.layoutTuitionDetails.visibility = View.VISIBLE
            binding.tvTuitionSubtitle.text = "Standard on MWF (Mon/Wed/Fri) 6:00–7:00 PM."
            binding.btnResetToStandard.text = "Standard MWF"
        }
        binding.tvDialogTargetDay.text = "📅 ${DateUtils.getDayName(targetDay)} (${if (isTtss) "TTSS Routine" else "MWF Routine"})"
    }

    private fun setupWakeTimeControls() {
        binding.btnPickWakeTime.setOnClickListener {
            showTimePicker(selectedWakeTime) { newTime ->
                selectedWakeTime = newTime
                binding.chipGroupWakeTime.clearCheck()
                updateButtonLabels()
                recalculateAndPreview()
            }
        }

        binding.chipGroupWakeTime.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipWake0530.id -> selectedWakeTime = "05:30"
                binding.chipWake0630.id -> selectedWakeTime = "06:30"
                binding.chipWake0700.id -> selectedWakeTime = "07:00"
                binding.chipWake0800.id -> selectedWakeTime = "08:00"
                binding.chipWake0830.id -> selectedWakeTime = "08:30"
                binding.chipWake0900.id -> selectedWakeTime = "09:00"
                binding.chipWakeCustom.id -> {
                    showTimePicker(selectedWakeTime) { newTime ->
                        selectedWakeTime = newTime
                        updateButtonLabels()
                        recalculateAndPreview()
                    }
                    return@setOnCheckedStateChangeListener
                }
            }
            updateButtonLabels()
            recalculateAndPreview()
        }
    }

    private fun setupGymControls() {
        binding.switchGymToday.setOnCheckedChangeListener { _, isChecked ->
            hasGym = isChecked
            binding.layoutGymDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
            recalculateAndPreview()
        }

        binding.btnPickGymTime.setOnClickListener {
            showTimePicker(selectedGymStartTime) { newTime ->
                selectedGymStartTime = newTime
                binding.chipGroupGymTiming.clearCheck()
                updateButtonLabels()
                recalculateAndPreview()
            }
        }

        binding.chipGroupGymTiming.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipGymPresetMorning.id -> selectedGymStartTime = "07:15"
                binding.chipGymPresetEvening530.id -> selectedGymStartTime = "17:30"
                binding.chipGymPresetEvening600.id -> selectedGymStartTime = "18:00"
                binding.chipGymPresetNight.id -> selectedGymStartTime = "20:30"
                binding.chipGymCustom.id -> {
                    showTimePicker(selectedGymStartTime) { newTime ->
                        selectedGymStartTime = newTime
                        updateButtonLabels()
                        recalculateAndPreview()
                    }
                    return@setOnCheckedStateChangeListener
                }
            }
            updateButtonLabels()
            recalculateAndPreview()
        }

        binding.chipGroupGymDuration.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipGymDur45.id -> selectedGymDuration = 45
                binding.chipGymDur60.id -> selectedGymDuration = 60
                binding.chipGymDur90.id -> selectedGymDuration = 90
            }
            recalculateAndPreview()
        }
    }

    private fun setupTuitionControls() {
        binding.switchTuitionToday.setOnCheckedChangeListener { _, isChecked ->
            hasTuition = isChecked
            binding.layoutTuitionDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
            recalculateAndPreview()
        }

        binding.btnPickTuitionTime.setOnClickListener {
            showTimePicker(selectedTuitionStartTime) { newTime ->
                selectedTuitionStartTime = newTime
                binding.chipGroupTuitionTiming.clearCheck()
                updateButtonLabels()
                recalculateAndPreview()
            }
        }

        binding.chipGroupTuitionTiming.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipTuitionPresetEvening6.id -> selectedTuitionStartTime = "18:00"
                binding.chipTuitionPresetMorning8.id -> selectedTuitionStartTime = "08:00"
                binding.chipTuitionPresetMorning10.id -> selectedTuitionStartTime = "10:00"
                binding.chipTuitionPresetMorning11.id -> selectedTuitionStartTime = "11:00"
                binding.chipTuitionPresetAfternoon3.id -> selectedTuitionStartTime = "15:00"
                binding.chipTuitionCustom.id -> {
                    showTimePicker(selectedTuitionStartTime) { newTime ->
                        selectedTuitionStartTime = newTime
                        updateButtonLabels()
                        recalculateAndPreview()
                    }
                    return@setOnCheckedStateChangeListener
                }
            }
            updateButtonLabels()
            recalculateAndPreview()
        }

        binding.chipGroupTuitionDuration.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipTuitionDur60.id -> selectedTuitionDuration = 60
                binding.chipTuitionDur90.id -> selectedTuitionDuration = 90
                binding.chipTuitionDur120.id -> selectedTuitionDuration = 120
            }
            recalculateAndPreview()
        }
    }

    private fun setupStudyTargetControls() {
        binding.chipGroupStudyTarget.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.chipTarget110.id -> selectedTargetHours = 11.0
                binding.chipTarget115.id -> selectedTargetHours = 11.5
                binding.chipTarget120.id -> selectedTargetHours = 12.0
            }
            recalculateAndPreview()
        }
    }

    private fun updateButtonLabels() {
        binding.btnPickWakeTime.text = "⏰ Wake Up: ${RoutineOptimizerEngine.formatTo12Hour(selectedWakeTime)}  (Tap to Enter Time)"
        binding.btnPickGymTime.text = "🏋️ Gym Start: ${RoutineOptimizerEngine.formatTo12Hour(selectedGymStartTime)}  (Tap to Enter Time)"
        binding.btnPickTuitionTime.text = "📚 Tuition Start: ${RoutineOptimizerEngine.formatTo12Hour(selectedTuitionStartTime)}  (Tap to Enter Time)"
    }

    private fun showTimePicker(currentTimeStr: String, onTimePicked: (String) -> Unit) {
        val totalMins = RoutineOptimizerEngine.timeToMinutes(currentTimeStr)
        val initialHour = totalMins / 60
        val initialMin = totalMins % 60

        val dialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                onTimePicked(formatted)
            },
            initialHour,
            initialMin,
            false // 12-hour format with AM/PM for user ease
        )
        dialog.show()
    }

    private fun recalculateAndPreview() {
        val params = CustomOptimizationParams(
            dayOfWeek = targetDay,
            scheduleTypeId = scheduleTypeId,
            wakeTimeStr = selectedWakeTime,
            hasGym = hasGym,
            gymStartTimeStr = selectedGymStartTime,
            gymDurationMinutes = selectedGymDuration,
            hasTuition = hasTuition,
            tuitionStartTimeStr = selectedTuitionStartTime,
            tuitionDurationMinutes = selectedTuitionDuration,
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
        binding.tvLiveTimelinePreview.text = "📋 ${result.timelinePreview}"
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
