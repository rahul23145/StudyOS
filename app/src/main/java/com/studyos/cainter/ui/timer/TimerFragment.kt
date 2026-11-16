package com.studyos.cainter.ui.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.R
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.databinding.FragmentTimerBinding
import kotlinx.coroutines.launch

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by activityViewModels {
        val app = requireActivity().application as StudyOSApplication
        TimerViewModel.Factory(app, app.studyRepository)
    }

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

        setupPresets()
        setupControls()
        observeState()
    }

    private fun setupPresets() {
        if (com.studyos.cainter.BuildConfig.IS_CUSTOM_APP) {
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
            // Rahul's CA Inter Presets
            binding.btnPresetStudyBlock.setOnClickListener { viewModel.setPreset("2h Study Block", 120) }
            binding.btnPresetCosting.setOnClickListener { viewModel.setPreset("Costing Lecture (2h 18m)", 138) }
            binding.btnPresetAudit.setOnClickListener { viewModel.setPreset("Audit Lecture (1h 30m)", 90) }
            binding.btnPresetBreak.setOnClickListener { viewModel.setPreset("15m Break", 15) }
            binding.btnPresetTuition.setOnClickListener { viewModel.setPreset("Tuition Class (60m)", 60) }
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
            Toast.makeText(requireContext(), "Session logged in Study Database!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.tvTimerPresetLabel.text = state.presetTitle
                        binding.tvTimerDigits.text = state.formattedTime
                        binding.circularTimerProgress.progress = state.progressPercent

                        when (state.state) {
                            TimerState.IDLE -> {
                                binding.btnStartPauseTimer.text = "Start"
                                binding.btnStartPauseTimer.setIconResource(R.drawable.ic_play)
                                binding.tvTimerStatus.text = "READY"
                            }
                            TimerState.RUNNING -> {
                                binding.btnStartPauseTimer.text = "Pause"
                                binding.btnStartPauseTimer.setIconResource(R.drawable.ic_pause)
                                binding.tvTimerStatus.text = "FOCUSING"
                            }
                            TimerState.PAUSED -> {
                                binding.btnStartPauseTimer.text = "Resume"
                                binding.btnStartPauseTimer.setIconResource(R.drawable.ic_play)
                                binding.tvTimerStatus.text = "PAUSED"
                            }
                            TimerState.FINISHED -> {
                                binding.btnStartPauseTimer.text = "Restart"
                                binding.btnStartPauseTimer.setIconResource(R.drawable.ic_reset)
                                binding.tvTimerStatus.text = "FINISHED"
                            }
                        }
                    }
                }

                launch {
                    viewModel.timerFinishedEvent.collect { title ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Time's Up!")
                            .setMessage("Congratulations! You completed \"$title\". Would you like to save this to your study history?")
                            .setPositiveButton("Log Session") { _, _ ->
                                viewModel.logCurrentSession("Finished complete timer block")
                                Toast.makeText(requireContext(), "Saved to database!", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Dismiss", null)
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
