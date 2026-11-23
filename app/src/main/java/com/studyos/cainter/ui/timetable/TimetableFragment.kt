package com.studyos.cainter.ui.timetable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.BuildConfig
import com.studyos.cainter.R
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.data.local.entity.ScheduleType
import com.studyos.cainter.databinding.FragmentTimetableBinding
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TimetableFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimetableViewModel by viewModels {
        val app = requireActivity().application as StudyOSApplication
        TimetableViewModel.Factory(app.scheduleRepository)
    }

    private lateinit var adapter: TimetableAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupDayChips()
        setupButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = TimetableAdapter(
            onEditClicked = { item -> showEditDialog(item) },
            onDeleteClicked = { item -> confirmDelete(item) }
        )
        binding.rvTimetable.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTimetable.adapter = adapter
    }

    private fun setupDayChips() {
        val today = DateUtils.getTodayDayOfWeek()
        when (today) {
            1 -> binding.chipMon.isChecked = true
            2 -> binding.chipTue.isChecked = true
            3 -> binding.chipWed.isChecked = true
            4 -> binding.chipThu.isChecked = true
            5 -> binding.chipFri.isChecked = true
            6 -> binding.chipSat.isChecked = true
            7 -> binding.chipSun.isChecked = true
        }

        binding.chipMon.setOnClickListener { viewModel.selectDay(1) }
        binding.chipTue.setOnClickListener { viewModel.selectDay(2) }
        binding.chipWed.setOnClickListener { viewModel.selectDay(3) }
        binding.chipThu.setOnClickListener { viewModel.selectDay(4) }
        binding.chipFri.setOnClickListener { viewModel.selectDay(5) }
        binding.chipSat.setOnClickListener { viewModel.selectDay(6) }
        binding.chipSun.setOnClickListener { viewModel.selectDay(7) }
    }

    private fun setupButtons() {
        if (com.studyos.cainter.BuildConfig.IS_CUSTOM_APP) {
            binding.cardRebalanceRoutine.visibility = View.GONE
        } else {
            binding.cardRebalanceRoutine.visibility = View.VISIBLE
            binding.btnRebalanceRoutine.setOnClickListener {
                val currentDay = viewModel.selectedDay.value
                val typeId = viewModel.activeScheduleType.value?.id ?: 1L
                val dialog = SmartRoutineOptimizerDialog.newInstance(currentDay, typeId)
                dialog.onAppliedCallback = {
                    viewModel.selectDay(currentDay)
                }
                dialog.show(parentFragmentManager, SmartRoutineOptimizerDialog.TAG)
            }
        }

        binding.btnScheduleType.setOnClickListener {
            val dialog = ScheduleTypeDialog.newInstance()
            dialog.show(parentFragmentManager, "ScheduleTypeDialog")
        }

        binding.fabAddActivity.setOnClickListener {
            showEditDialog(null)
        }

        binding.btnResetDefaults.setOnClickListener {
            val typeName = viewModel.activeScheduleType.value?.name ?: "Current Profile"
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset to Recommended Schedule")
                .setMessage("Reset $typeName to its recommended timetable?")
                .setPositiveButton("Reset") { _, _ ->
                    viewModel.resetToDefaults()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(viewModel.selectedDay, viewModel.activeScheduleType) { day, activeType ->
                        Pair(day, activeType)
                    }.collect { (day, activeType) ->
                        binding.tvDayTitle.text = "${DateUtils.getDayName(day)} Schedule"
                        val type = activeType ?: return@collect
                        val displayName = if (type.name.contains("CA Intermediate", ignoreCase = true)) {
                            "CA Inter ▾"
                        } else if (type.name.length > 14) {
                            "${type.name.take(13)}… ▾"
                        } else {
                            "${type.name} ▾"
                        }
                        binding.btnScheduleType.text = displayName
                        updateOverviewBadge(day, type)
                    }
                }
                launch {
                    viewModel.scheduleItems.collect { items ->
                        adapter.submitList(items)
                    }
                }
            }
        }
    }

    private fun updateOverviewBadge(day: Int, type: ScheduleType) {
        if (type.id == 1L && !BuildConfig.IS_CUSTOM_APP) {
            binding.tvDayOverviewBadge.text = when (day) {
                1, 3, 5 -> "MWF Routine • Tuition 6:00–7:00 PM • Live Audit (2h30m) + Costing 2x (2h18m) • Gym 1h"
                2, 4, 6 -> "TTSS Routine • Running 6:30–7:15 PM • Live Audit (2h30m) + Costing 2x (2h18m) • Gym 1h"
                7 -> "Sunday Audit-Only • 3x 2-Hour Study Blocks • Live Audit Concept Revision • TTSS Running"
                else -> "Standard CA Inter Schedule"
            }
        } else {
            binding.tvDayOverviewBadge.text = "${type.name} • Target ${type.targetDailyHours}h/day • Wake ${type.wakeTime} • Sleep ${type.sleepTime}"
        }
    }

    private fun showEditDialog(item: ScheduleItem?) {
        val currentTypeId = viewModel.activeScheduleType.value?.id ?: 1L
        val dialog = EditScheduleDialog.newInstance(
            item = item,
            defaultDay = viewModel.selectedDay.value,
            scheduleTypeId = currentTypeId,
            onSave = { savedItem -> viewModel.saveItem(savedItem) }
        )
        dialog.show(parentFragmentManager, "EditScheduleDialog")
    }

    private fun confirmDelete(item: ScheduleItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage("Remove '${item.title}' from this day?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
