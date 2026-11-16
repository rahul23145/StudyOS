package com.studyos.cainter.ui.timetable

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.data.local.entity.ScheduleCategory
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.databinding.DialogEditScheduleBinding
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class EditScheduleDialog : DialogFragment() {

    private var _binding: DialogEditScheduleBinding? = null
    private val binding get() = _binding!!

    private var itemToEdit: ScheduleItem? = null
    private var defaultDay: Int = 1
    private var onSaveListener: ((ScheduleItem) -> Unit)? = null

    companion object {
        fun newInstance(
            item: ScheduleItem?,
            defaultDay: Int,
            onSave: (ScheduleItem) -> Unit
        ): EditScheduleDialog {
            val dialog = EditScheduleDialog()
            dialog.itemToEdit = item
            dialog.defaultDay = defaultDay
            dialog.onSaveListener = onSave
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditScheduleBinding.inflate(layoutInflater)

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val dayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, days)
        binding.spinnerDay.adapter = dayAdapter

        val categories = ScheduleCategory.entries.map { it.displayName }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = categoryAdapter

        val item = itemToEdit
        if (item != null) {
            binding.tvDialogTitle.text = "Edit Activity"
            binding.etTitle.setText(item.title)
            binding.etStartTime.setText(item.startTime)
            binding.etEndTime.setText(item.endTime)
            binding.etNotes.setText(item.notes)
            binding.spinnerDay.setSelection((item.dayOfWeek - 1).coerceIn(0, 6))

            val catIndex = ScheduleCategory.entries.indexOfFirst { it.name == item.category }
            if (catIndex >= 0) binding.spinnerCategory.setSelection(catIndex)
        } else {
            binding.tvDialogTitle.text = "Add New Activity"
            binding.spinnerDay.setSelection((defaultDay - 1).coerceIn(0, 6))
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val title = binding.etTitle.text.toString().trim()
                if (title.isBlank()) return@setPositiveButton

                val startTime = binding.etStartTime.text.toString().trim().ifBlank { "09:00" }
                val endTime = binding.etEndTime.text.toString().trim().ifBlank { "10:00" }
                val selectedDay = binding.spinnerDay.selectedItemPosition + 1
                val selectedCategory = ScheduleCategory.entries[binding.spinnerCategory.selectedItemPosition].name
                val notes = binding.etNotes.text.toString().trim()

                val durationMinutes = calculateDuration(startTime, endTime)

                val savedItem = ScheduleItem(
                    id = item?.id ?: 0L,
                    dayOfWeek = selectedDay,
                    title = title,
                    category = selectedCategory,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    notes = notes,
                    sortOrder = item?.sortOrder ?: 99,
                    isCustom = true
                )
                onSaveListener?.invoke(savedItem)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun calculateDuration(start: String, end: String): Int {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val startTime = LocalTime.parse(start, formatter)
            val endTime = LocalTime.parse(end, formatter)
            val minutes = ChronoUnit.MINUTES.between(startTime, endTime).toInt()
            if (minutes > 0) minutes else (minutes + 1440)
        } catch (e: Exception) {
            60
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
