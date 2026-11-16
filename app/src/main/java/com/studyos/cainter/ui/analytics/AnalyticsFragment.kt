package com.studyos.cainter.ui.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.studyos.cainter.BuildConfig
import com.studyos.cainter.R
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.databinding.FragmentAnalyticsBinding
import com.studyos.cainter.util.AppUsageTracker
import com.studyos.cainter.util.DateUtils
import com.studyos.cainter.util.MentorPdfReportGenerator
import com.studyos.cainter.util.StepCounterManager
import com.studyos.cainter.util.ZeroDistractionGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by activityViewModels {
        val app = requireActivity().application as StudyOSApplication
        AnalyticsViewModel.Factory(app.scheduleRepository, app.studyRepository)
    }

    private lateinit var adapter: RecentSessionsAdapter

    private val stepPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            StepCounterManager.setTrackingEnabled(requireContext(), true)
            updateStepTrackingSwitchUI(true)
            Snackbar.make(binding.root, "✅ Step tracking running in background", Snackbar.LENGTH_SHORT).show()
        } else {
            StepCounterManager.setTrackingEnabled(requireContext(), false)
            updateStepTrackingSwitchUI(false)
            Snackbar.make(binding.root, "⚠️ Physical Activity permission required for step tracking", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isCustom = BuildConfig.IS_CUSTOM_APP
        if (isCustom) {
            binding.cardCaInterAdherence.visibility = View.GONE
            // Mentor report feature is EXCLUSIVE to CA Inter edition
            binding.cardMentorReport.visibility = View.GONE
        } else {
            binding.cardMentorReport.visibility = View.VISIBLE
            setupMentorReportButton()
        }

        setupRecyclerView()
        setupStepTrackingSwitch()
        setupAppUsageCard()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        loadAppUsageStats()
        updateStepTrackingSwitchUI(StepCounterManager.isTrackingEnabled(requireContext()))
    }

    private fun setupMentorReportButton() {
        binding.btnGenerateMentorPdf.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    binding.btnGenerateMentorPdf.isEnabled = false
                    binding.btnGenerateMentorPdf.text = "⏳ Generating PDF..."

                    val uri = MentorPdfReportGenerator.generateWeeklyReport(requireContext())
                    if (uri != null) {
                        Snackbar.make(
                            binding.root,
                            "✅ Saved to Downloads: StudyOS Weekly Report PDF",
                            Snackbar.LENGTH_LONG
                        ).setAction("Open PDF") {
                            MentorPdfReportGenerator.openPdfViewer(requireContext(), uri)
                        }.show()
                    } else {
                        Snackbar.make(binding.root, "❌ Failed to save PDF to Downloads", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                } finally {
                    binding.btnGenerateMentorPdf.isEnabled = true
                    binding.btnGenerateMentorPdf.text = "📄 Save Weekly PDF to Downloads"
                }
            }
        }
    }

    private fun setupStepTrackingSwitch() {
        val isEnabled = StepCounterManager.isTrackingEnabled(requireContext())
        updateStepTrackingSwitchUI(isEnabled)
    }

    private fun updateStepTrackingSwitchUI(isEnabled: Boolean) {
        binding.switchStepTracking.setOnCheckedChangeListener(null)
        binding.switchStepTracking.isChecked = isEnabled
        if (isEnabled) {
            binding.tvStepTrackingStatus.text = "● Active in background"
            binding.tvStepTrackingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_emerald))
        } else {
            binding.tvStepTrackingStatus.text = "○ Stopped (not running in background)"
            binding.tvStepTrackingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary_light))
        }

        binding.switchStepTracking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                    !StepCounterManager.hasActivityRecognitionPermission(requireContext())) {
                    stepPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    StepCounterManager.setTrackingEnabled(requireContext(), true)
                    updateStepTrackingSwitchUI(true)
                    Snackbar.make(binding.root, "✅ Step tracking running in background", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                StepCounterManager.setTrackingEnabled(requireContext(), false)
                updateStepTrackingSwitchUI(false)
                Snackbar.make(binding.root, "Step counter stopped. Not running in background.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAppUsageCard() {
        binding.btnEnableUsageAccess.setOnClickListener {
            ZeroDistractionGuard.openUsageSettings(requireContext())
        }
    }

    private fun loadAppUsageStats() {
        val hasPermission = AppUsageTracker.hasUsageAccessPermission(requireContext())
        if (!hasPermission) {
            binding.btnEnableUsageAccess.visibility = View.VISIBLE
            binding.tvTotalOtherScreenTime.text = "Needs Permission"
            binding.layoutAppUsageList.removeAllViews()
            return
        }

        binding.btnEnableUsageAccess.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val usageList = withContext(Dispatchers.IO) {
                AppUsageTracker.getTodayAppUsage(requireContext())
            }

            if (_binding == null) return@launch

            // Update Social Media Card (Instagram & YouTube)
            val instaMillis = usageList.firstOrNull { it.packageName == "com.instagram.android" }?.foregroundTimeMillis ?: 0L
            val ytMillis = usageList.firstOrNull { it.packageName == "com.google.android.youtube" }?.foregroundTimeMillis ?: 0L
            val instaMins = (instaMillis / 60_000L).toInt()
            val ytMins = (ytMillis / 60_000L).toInt()
            val socialTotalMins = instaMins + ytMins

            binding.tvInstagramTime.text = if (instaMins >= 60) "${instaMins / 60}h ${instaMins % 60}m" else "${instaMins}m today"
            binding.tvYouTubeTime.text = if (ytMins >= 60) "${ytMins / 60}h ${ytMins % 60}m" else "${ytMins}m today"
            binding.tvSocialTotalBadge.text = if (socialTotalMins >= 60) "${socialTotalMins / 60}h ${socialTotalMins % 60}m" else "${socialTotalMins}m today"

            binding.layoutAppUsageList.removeAllViews()
            var totalTimeMillis = 0L

            val topApps = usageList.take(5)
            for (app in topApps) {
                totalTimeMillis += app.foregroundTimeMillis
                val row = layoutInflater.inflate(R.layout.item_app_usage, binding.layoutAppUsageList, false)
                val ivIcon = row.findViewById<ImageView>(R.id.ivAppIcon)
                val tvName = row.findViewById<TextView>(R.id.tvAppName)
                val tvCategory = row.findViewById<TextView>(R.id.tvAppCategory)
                val tvDuration = row.findViewById<TextView>(R.id.tvAppDuration)

                if (app.iconDrawable != null) {
                    ivIcon.setImageDrawable(app.iconDrawable)
                } else {
                    ivIcon.setImageResource(R.mipmap.ic_launcher)
                }

                tvName.text = app.appName
                tvCategory.text = app.category
                val totalMins = (app.foregroundTimeMillis / 60_000L).toInt()
                tvDuration.text = if (totalMins >= 60) {
                    "${totalMins / 60}h ${totalMins % 60}m"
                } else {
                    "${totalMins}m"
                }

                binding.layoutAppUsageList.addView(row)
            }

            val totalMinsAll = (usageList.sumOf { it.foregroundTimeMillis } / 60_000L).toInt()
            binding.tvTotalOtherScreenTime.text = if (totalMinsAll >= 60) {
                "${totalMinsAll / 60}h ${totalMinsAll % 60}m"
            } else {
                "${totalMinsAll}m"
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = RecentSessionsAdapter()
        binding.rvRecentSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentSessions.adapter = adapter
    }

    private fun observeData() {
        val app = requireActivity().application as StudyOSApplication

        // 1. Observe Study Sessions & Charts
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvAnalyticsTodayMinutes.text = DateUtils.formatMinutesToHoursAndMinutes(state.todayMinutes)
                    binding.tvAnalyticsWeekMinutes.text = DateUtils.formatMinutesToHoursAndMinutes(state.weekMinutes)
                    binding.tvAnalyticsTotalMinutes.text = DateUtils.formatMinutesToHoursAndMinutes(state.totalAllTimeMinutes)
                    binding.chartWeeklyStudy.setData(state.chartData)
                    adapter.submitList(state.recentSessions)
                }
            }
        }

        // 2. Observe Step Counter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.stepRepository.getTodayStepRecord().collect { record ->
                    val steps = record?.totalSteps ?: 0
                    val distanceKm = (record?.distanceMeters ?: 0f) / 1000f
                    val calories = record?.caloriesBurned ?: 0f

                    binding.tvTodaySteps.text = String.format(Locale.US, "%,d", steps)
                    val progress = (steps * 100 / 8000).coerceIn(0, 100)
                    binding.progressSteps.progress = progress
                    binding.tvStepDistance.text = String.format(Locale.US, "Distance: %.1f km", distanceKm)
                    binding.tvStepCalories.text = String.format(Locale.US, "Burned: %.0f kcal", calories)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
