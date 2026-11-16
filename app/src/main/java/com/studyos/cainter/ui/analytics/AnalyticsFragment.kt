package com.studyos.cainter.ui.analytics

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
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.databinding.FragmentAnalyticsBinding
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.launch

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by activityViewModels {
        val app = requireActivity().application as StudyOSApplication
        AnalyticsViewModel.Factory(app.scheduleRepository, app.studyRepository)
    }

    private lateinit var adapter: RecentSessionsAdapter

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

        if (com.studyos.cainter.BuildConfig.IS_CUSTOM_APP) {
            binding.cardCaInterAdherence.visibility = View.GONE
        }

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = RecentSessionsAdapter()
        binding.rvRecentSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentSessions.adapter = adapter
    }

    private fun observeData() {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
