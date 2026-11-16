package com.studyos.cainter.ui.timetable

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
import com.studyos.cainter.R
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.databinding.FragmentTimetableBinding
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.launch

class TimetableFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimetableViewModel by activityViewModels {
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
        binding.btnScheduleType.setOnClickListener {
            val dialog = ScheduleTypeDialog.newInstance { selectedType ->
                binding.tvDayOverviewBadge.text = "${selectedType.name} • Target ${selectedType.targetDailyHours}h/day • Wake ${selectedType.wakeTime} • Sleep ${selectedType.sleepTime}"
            }
            dialog.show(parentFragmentManager, "ScheduleTypeDialog")
        }

        binding.fabAddActivity.setOnClickListener {
            showEditDialog(null)
        }

        binding.btnResetDefaults.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset to Recommended Schedule")
                .setMessage(getString(R.string.reset_confirm_msg))
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
                    viewModel.selectedDay.collect { day ->
                        binding.tvDayTitle.text = "${DateUtils.getDayName(day)} Schedule"
                        binding.tvDayOverviewBadge.text = when (day) {
                            1, 3, 5 -> "MWF Routine • Tuition 6:00–7:00 PM • Costing 2x (2h18m) + Audit 2x (1h30m) • Gym 1h"
                            2, 4, 6 -> "TTSS Routine • Running 6:30–7:15 PM • Costing 2x (2h18m) + Audit 2x (1h30m) • Gym 1h"
                            7 -> "Sunday Audit-Only • 3x 2-Hour Study Blocks • 2x Audit Lectures • TTSS Running"
                            else -> "Standard CA Inter Schedule"
                        }
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

    private fun showEditDialog(item: ScheduleItem?) {
        val dialog = EditScheduleDialog.newInstance(
            item = item,
            defaultDay = viewModel.selectedDay.value,
            onSave = { savedItem -> viewModel.saveItem(savedItem) }
        )
        dialog.show(parentFragmentManager, "EditScheduleDialog")
    }

    private fun confirmDelete(item: ScheduleItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Activity")
            .setMessage("Are you sure you want to remove \"${item.title}\"?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteItem(item) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
