package com.studyos.cainter.ui.timer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.BuildConfig
import com.studyos.cainter.R
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.databinding.DialogAddSequenceTaskBinding
import com.studyos.cainter.databinding.FragmentTimerBinding
import com.studyos.cainter.ui.distraction.ConfigureDistractionsDialog
import com.studyos.cainter.util.FocusGuardService
import com.studyos.cainter.util.OverlayBlockerManager
import com.studyos.cainter.util.ZeroDistractionGuard
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by activityViewModels {
        val app = requireActivity().application as StudyOSApplication
        TimerViewModel.Factory(app, app.studyRepository, app.scheduleRepository)
    }

    private lateinit var sequenceAdapter: SequenceTaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupModeToggle()
        setupPresets()
        setupSequenceQueue()
        setupZeroDistractionMode()
        setupControls()
        observeState()
    }

    private fun setupModeToggle() {
        binding.toggleTimerMode.check(R.id.btnModeSingle)
        updateToggleButtonStyles(R.id.btnModeSingle)

        binding.toggleTimerMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            updateToggleButtonStyles(checkedId)
            when (checkedId) {
                R.id.btnModeSingle -> {
                    viewModel.setSequenceMode(false)
                    binding.layoutSinglePresets.visibility = View.VISIBLE
                    binding.layoutSequenceQueue.visibility = View.GONE
                    binding.btnPreviousTask.visibility = View.GONE
                    binding.btnSkipTask.visibility = View.GONE
                    binding.btnLogSession.visibility = View.VISIBLE
                }
                R.id.btnModeSequence -> {
                    viewModel.setSequenceMode(true)
                    binding.layoutSinglePresets.visibility = View.GONE
                    binding.layoutSequenceQueue.visibility = View.VISIBLE
                    binding.btnPreviousTask.visibility = View.VISIBLE
                    binding.btnSkipTask.visibility = View.VISIBLE
                    binding.btnLogSession.visibility = View.GONE
                }
            }
        }
    }

    private fun updateToggleButtonStyles(checkedId: Int) {
        val primary = ContextCompat.getColor(requireContext(), R.color.primary)
        val onPrimary = ContextCompat.getColor(requireContext(), R.color.on_primary)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary_light)

        if (checkedId == R.id.btnModeSingle) {
            binding.btnModeSingle.setBackgroundColor(primary)
            binding.btnModeSingle.setTextColor(onPrimary)
            binding.btnModeSequence.setBackgroundColor(Color.TRANSPARENT)
            binding.btnModeSequence.setTextColor(primary)
        } else {
            binding.btnModeSequence.setBackgroundColor(primary)
            binding.btnModeSequence.setTextColor(onPrimary)
            binding.btnModeSingle.setBackgroundColor(Color.TRANSPARENT)
            binding.btnModeSingle.setTextColor(primary)
        }
    }

    private fun setupPresets() {
        if (BuildConfig.IS_CUSTOM_APP) {
            binding.btnPresetStudyBlock.text = "2h Study Block"
            binding.btnPresetCosting.text = "50m Focus"
            binding.btnPresetAudit.text = "90m Deep Work"
            binding.btnPresetTuition.text = "25m Pomodoro"
            binding.btnPresetBreak.text = "15m Break"

            binding.btnPresetStudyBlock.setOnClickListener { viewModel.setPreset("2h Study Block", 120) }
            binding.btnPresetCosting.setOnClickListener { viewModel.setPreset("50m Focus Session", 50) }
            binding.btnPresetAudit.setOnClickListener { viewModel.setPreset("90m Deep Work Block", 90) }
            binding.btnPresetTuition.setOnClickListener { viewModel.setPreset("25m Pomodoro", 25) }
            binding.btnPresetBreak.setOnClickListener { viewModel.setPreset("15m Break", 15) }
        } else {
            binding.btnPresetStudyBlock.setOnClickListener { viewModel.setPreset("2-Hour Deep Study Block", 120) }
            binding.btnPresetCosting.setOnClickListener { viewModel.setPreset("Costing Lecture (2h 18m)", 138) }
            binding.btnPresetAudit.setOnClickListener { viewModel.setPreset("Audit Lecture (1h 30m)", 90) }
            binding.btnPresetTuition.setOnClickListener { viewModel.setPreset("Tuition Class (60m)", 60) }
            binding.btnPresetBreak.setOnClickListener { viewModel.setPreset("15m Refreshment Break", 15) }
        }
    }

    private fun setupSequenceQueue() {
        sequenceAdapter = SequenceTaskAdapter(
            tasks = emptyList(),
            activeIndex = 0,
            onTaskSelected = { index -> viewModel.selectSequenceTask(index) },
            onDeleteClicked = { index -> viewModel.removeSequenceTask(index) }
        )
        binding.rvSequenceTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSequenceTasks.adapter = sequenceAdapter

        binding.swAutoAdvance.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleAutoAdvance(isChecked)
        }

        binding.btnLoadTodaySchedule.setOnClickListener {
            viewModel.loadTodayScheduleAsSequence()
            Toast.makeText(requireContext(), "Loaded today's scheduled tasks into sequence!", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddSequenceTask.setOnClickListener {
            showAddSequenceTaskDialog()
        }

        binding.btnPreviousTask.setOnClickListener {
            viewModel.previousTask()
        }

        binding.btnSkipTask.setOnClickListener {
            viewModel.skipToNextTask()
        }
    }

    private fun showAddSequenceTaskDialog() {
        val addBinding = DialogAddSequenceTaskBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setView(addBinding.root)
            .setPositiveButton("Add Task") { _, _ ->
                val title = addBinding.etTaskTitle.text.toString().trim()
                val duration = addBinding.etTaskDurationMinutes.text.toString().toIntOrNull() ?: 60
                if (title.isNotBlank()) {
                    viewModel.addCustomSequenceTask(title, duration)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupZeroDistractionMode() {
        val context = requireContext()
        binding.swZeroDistraction.isChecked = ZeroDistractionGuard.isEnabled(context)

        binding.swZeroDistraction.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!ZeroDistractionGuard.hasUsagePermission(context)) {
                    showUsagePermissionDialog()
                } else if (!OverlayBlockerManager.canDrawOverlays(context)) {
                    showOverlayPermissionDialog()
                } else {
                    ZeroDistractionGuard.setEnabled(context, true)
                    val activeTitle = viewModel.uiState.value.presetTitle
                    val activeRemaining = viewModel.uiState.value.formattedTime
                    FocusGuardService.startService(context, activeTitle, activeRemaining)
                    val toastMsg = if (OverlayBlockerManager.isMiuiDevice()) {
                        "🛡️ Zero Distraction Mode Active!\n(Xiaomi: Allow 'Display pop-up windows in background' in permissions)"
                    } else {
                        "🛡️ Zero Distraction Mode Activated!"
                    }
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                }
            } else {
                ZeroDistractionGuard.setEnabled(context, false)
                OverlayBlockerManager.hideOverlay(context)
                if (viewModel.uiState.value.state != TimerState.RUNNING) {
                    FocusGuardService.stopService(context)
                }
                Toast.makeText(context, "Zero Distraction Mode Deactivated", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConfigureDistractions.setOnClickListener {
            ConfigureDistractionsDialog.newInstance().show(parentFragmentManager, "ConfigureDistractions")
        }
    }

    private fun showOverlayPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🛡️ Display Over Other Apps")
            .setMessage("To display the blocked screen right on top of Instagram or YouTube when opened, please allow 'Display over other apps' in Android Settings.\n\n• 100% Offline & Private\n• Essential for blocking distracting apps immediately")
            .setPositiveButton("Open Settings") { _, _ ->
                OverlayBlockerManager.openOverlaySettings(requireContext())
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.swZeroDistraction.isChecked = false
            }
            .show()
    }

    private fun showUsagePermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🛡️ Enable App Blocker")
            .setMessage("Zero Distraction Mode blocks chosen distracting apps (Instagram, YouTube, etc.) while your timer is running.\n\n• 100% Offline & Private (Zero internet permissions)\n• Please grant 'Usage Access' in Android Settings so StudyOS can detect when a distracting app is opened.")
            .setPositiveButton("Open Settings") { _, _ ->
                ZeroDistractionGuard.openUsageSettings(requireContext())
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.swZeroDistraction.isChecked = false
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        val ctx = context ?: return
        val isGuardEnabled = ZeroDistractionGuard.isEnabled(ctx)
        val hasUsage = ZeroDistractionGuard.hasUsagePermission(ctx)

        if (isGuardEnabled) {
            if (!hasUsage) {
                binding.swZeroDistraction.isChecked = false
                ZeroDistractionGuard.setEnabled(ctx, false)
            } else {
                binding.swZeroDistraction.isChecked = true
            }
        }
    }

    private fun setupControls() {
        binding.btnStartPauseTimer.setOnClickListener {
            viewModel.startOrPause()
        }

        binding.btnResetTimer.setOnClickListener {
            viewModel.resetTimer()
        }

        binding.btnLogSession.setOnClickListener {
            viewModel.logCurrentSession()
            Toast.makeText(requireContext(), "Logged study session in database!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.tvTimerDigits.text = state.formattedTime
                        binding.circularTimerProgress.progress = state.progressPercent
                        binding.tvTimerPresetLabel.text = state.presetTitle

                        when (state.state) {
                            TimerState.IDLE -> {
                                binding.btnStartPauseTimer.text = "Start"
                                binding.btnStartPauseTimer.setIconResource(R.drawable.ic_play)
                                binding.tvTimerStatus.text = "READY TO FOCUS"
                                binding.tvTimerStatus.setTextColor(requireContext().getColor(R.color.secondary))
                            }
                            TimerState.RUNNING -> {
                                binding.btnStartPauseTimer.text = "Pause"
                                binding.btnStartPauseTimer.setIconResource(R.drawable.ic_pause)
                                binding.tvTimerStatus.text = "FOCUS IN PROGRESS"
                                binding.tvTimerStatus.setTextColor(requireContext().getColor(R.color.accent_emerald))
                            }
                            TimerState.PAUSED -> {
                                binding.btnStartPauseTimer.text = "Resume"
                                binding.btnStartPauseTimer.setIconResource(R.drawable.ic_play)
                                binding.tvTimerStatus.text = "TIMER PAUSED"
                                binding.tvTimerStatus.setTextColor(requireContext().getColor(R.color.accent_amber))
                            }
                            TimerState.FINISHED -> {
                                binding.btnStartPauseTimer.text = "Reset"
                                binding.btnStartPauseTimer.setIconResource(R.drawable.ic_reset)
                                binding.tvTimerStatus.text = "SESSION COMPLETE! GREAT JOB!"
                                binding.tvTimerStatus.setTextColor(requireContext().getColor(R.color.accent_emerald))
                            }
                        }
                    }
                }

                launch {
                    combine(viewModel.sequenceQueue, viewModel.currentSequenceIndex) { queue, index ->
                        Pair(queue, index)
                    }.collect { (queue, index) ->
                        sequenceAdapter.updateData(queue, index)
                    }
                }

                launch {
                    viewModel.isAutoAdvanceEnabled.collect { isAuto ->
                        if (binding.swAutoAdvance.isChecked != isAuto) {
                            binding.swAutoAdvance.isChecked = isAuto
                        }
                    }
                }

                launch {
                    viewModel.timerFinishedEvent.collect { sessionName ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("🎉 Session Finished!")
                            .setMessage("Congratulations! You completed '$sessionName'.\n\nYour study time has been recorded.")
                            .setPositiveButton("Awesome", null)
                            .show()
                    }
                }
            }
        }
    }

    fun applyPreset(title: String, durationMinutes: Int) {
        viewModel.setPreset(title, durationMinutes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
