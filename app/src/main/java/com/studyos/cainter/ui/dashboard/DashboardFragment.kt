package com.studyos.cainter.ui.dashboard

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
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
import com.studyos.cainter.R
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.databinding.FragmentDashboardBinding
import com.studyos.cainter.ui.MainActivity
import com.studyos.cainter.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by activityViewModels {
        val app = requireActivity().application as StudyOSApplication
        DashboardViewModel.Factory(app.scheduleRepository, app.studyRepository)
    }

    private lateinit var adapter: TodayScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupExamCountdown()
        setupRecyclerView()
        observeUiState()
    }

    private fun setupExamCountdown() {
        val prefs = requireContext().getSharedPreferences("studyos_prefs", Context.MODE_PRIVATE)
        updateExamCountdownDisplay(prefs)

        binding.cardExamCountdown.setOnClickListener {
            showExamDatePicker(prefs)
        }
    }

    private fun updateExamCountdownDisplay(prefs: SharedPreferences) {
        val savedDateStr = prefs.getString("key_exam_target_date", "2027-05-02") ?: "2027-05-02"
        val targetDate = try {
            LocalDate.parse(savedDateStr)
        } catch (e: Exception) {
            LocalDate.of(2027, 5, 2)
        }
        val today = LocalDate.now()
        val daysRemaining = ChronoUnit.DAYS.between(today, targetDate).coerceAtLeast(0)

        binding.tvExamCountdownDays.text = "$daysRemaining Days"
        val monthYear = targetDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
        val fullFormatted = targetDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))
        val examTitle = if (com.studyos.cainter.BuildConfig.IS_CUSTOM_APP) "Target Goal / Exam Countdown" else "CA Intermediate Target Exam"
        binding.tvExamCountdownTitle.text = examTitle
        binding.tvExamCountdownSubtitle.text = "Target: $fullFormatted ($monthYear) • Tap to change"
    }

    private fun showExamDatePicker(prefs: SharedPreferences) {
        val savedDateStr = prefs.getString("key_exam_target_date", "2027-05-02") ?: "2027-05-02"
        val targetDate = try {
            LocalDate.parse(savedDateStr)
        } catch (e: Exception) {
            LocalDate.of(2027, 5, 2)
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newTarget = LocalDate.of(year, month + 1, dayOfMonth)
                prefs.edit().putString("key_exam_target_date", newTarget.toString()).apply()
                updateExamCountdownDisplay(prefs)
            },
            targetDate.year,
            targetDate.monthValue - 1,
            targetDate.dayOfMonth
        ).show()
    }

    private fun setupRecyclerView() {
        adapter = TodayScheduleAdapter(
            onToggleCompletion = { item, isCompleted ->
                viewModel.toggleTaskCompletion(item, isCompleted)
            },
            onStartTimer = { item ->
                (activity as? MainActivity)?.openTimerWithPreset(item.title, item.durationMinutes)
            }
        )
        binding.rvTodaySchedule.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodaySchedule.adapter = adapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvDateDisplay.text = state.dateDisplay
                    binding.tvRoutineType.text = state.routineLabel
                    binding.chipStreak.text = "${state.streakDays} Day Streak"

                    binding.tvProgressPercentage.text = "${state.progressPercentage}%"
                    binding.progressDaily.progress = state.progressPercentage

                    binding.tvTasksCompletedCount.text = "${state.completedCount}/${state.totalCount} Done"

                    if (com.studyos.cainter.BuildConfig.IS_CUSTOM_APP) {
                        binding.tvCostingDone.text = "${state.completedCount}"
                        (binding.tvCostingDone.parent as? android.view.ViewGroup)?.let { group ->
                            (group.getChildAt(1) as? android.widget.TextView)?.text = "Tasks Done"
                        }
                        binding.tvAuditDone.text = "${(state.totalCount - state.completedCount).coerceAtLeast(0)}"
                        (binding.tvAuditDone.parent as? android.view.ViewGroup)?.let { group ->
                            (group.getChildAt(1) as? android.widget.TextView)?.text = "Remaining"
                        }
                    } else {
                        // Costing stats
                        val costingToday = state.costingTracker?.completedLectures ?: 0
                        binding.tvCostingDone.text = "$costingToday / ${state.costingTracker?.totalLectures ?: 85}"

                        // Audit stats
                        val auditToday = state.auditTracker?.completedLectures ?: 0
                        binding.tvAuditDone.text = "$auditToday / ${state.auditTracker?.totalLectures ?: 65}"
                    }

                    // Study time & graph
                    binding.tvStudyHoursLogged.text = DateUtils.formatMinutesToHoursAndMinutes(state.studyMinutesToday)

                    val totalAllTimeHours = state.totalStudyMinutesAllTime / 60
                    val totalAllTimeMins = state.totalStudyMinutesAllTime % 60
                    val totalTimeFormatted = if (totalAllTimeHours > 0) "${totalAllTimeHours}h ${totalAllTimeMins}m" else "${totalAllTimeMins}m"
                    binding.tvTotalStudyHoursAllTime.text = totalTimeFormatted
                    binding.chipTotalHours.text = "$totalTimeFormatted Total"

                    binding.dashboardChart.setData(state.chartData)

                    // Current Focus Card
                    if (state.currentItem != null) {
                        val current = state.currentItem
                        val durStr = if (current.durationMinutes >= 60) "${current.durationMinutes / 60}h ${current.durationMinutes % 60}m" else "${current.durationMinutes}m"
                        binding.tvFocusLabel.text = "CURRENT FOCUS BLOCK"
                        binding.tvCurrentActivityTitle.text = current.title
                        binding.tvCurrentActivityTime.text = "⏱️ $durStr block • Target ~${current.startTime} (Flexible)"
                    } else {
                        binding.tvFocusLabel.text = "UPCOMING FOCUS BLOCK"
                        binding.tvCurrentActivityTitle.text = state.nextItem?.title ?: "Free Review / Rest"
                        binding.tvCurrentActivityTime.text = state.nextItem?.let {
                            val durStr = if (it.durationMinutes >= 60) "${it.durationMinutes / 60}h ${it.durationMinutes % 60}m" else "${it.durationMinutes}m"
                            "⏱️ $durStr block • Target ~${it.startTime} (Flexible)"
                        } ?: "Plan your next milestone"
                    }

                    if (state.nextItem != null && state.currentItem != null) {
                        binding.tvUpNext.visibility = View.VISIBLE
                        binding.tvUpNext.text = "Next: ${state.nextItem.title} (~${state.nextItem.startTime})"
                    } else {
                        binding.tvUpNext.visibility = View.GONE
                    }

                    adapter.submitList(state.items)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
