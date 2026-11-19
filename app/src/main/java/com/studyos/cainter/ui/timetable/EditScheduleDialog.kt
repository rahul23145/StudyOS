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
    private var scheduleTypeId: Long = 1L
    private var onSaveListener: ((ScheduleItem) -> Unit)? = null

    companion object {
        fun newInstance(
            item: ScheduleItem?,
            defaultDay: Int,
            scheduleTypeId: Long = 1L,
            onSave: (ScheduleItem) -> Unit
        ): EditScheduleDialog {
            val dialog = EditScheduleDialog()
            dialog.itemToEdit = item
            dialog.defaultDay = defaultDay
            dialog.scheduleTypeId = scheduleTypeId
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
            binding.etStartTime.setText(com.studyos.cainter.util.DateUtils.formatTo12Hour(item.startTime))
            binding.etEndTime.setText(com.studyos.cainter.util.DateUtils.formatTo12Hour(item.endTime))
            binding.etNotes.setText(item.notes)
            binding.spinnerDay.setSelection((item.dayOfWeek - 1).coerceIn(0, 6))

            val catIndex = ScheduleCategory.entries.indexOfFirst { it.name == item.category }
            if (catIndex >= 0) binding.spinnerCategory.setSelection(catIndex)
        } else {
            binding.tvDialogTitle.text = "Add New Activity"
            binding.etStartTime.setText("09:00 AM")
            binding.etEndTime.setText("10:00 AM")
            binding.spinnerDay.setSelection((defaultDay - 1).coerceIn(0, 6))
        }

        binding.etStartTime.setOnClickListener {
            val current = com.studyos.cainter.util.DateUtils.to24Hour(binding.etStartTime.text.toString())
            val parts = current.split(":")
            val h = parts[0].toIntOrNull() ?: 9
            val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
            android.app.TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                val time24 = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute)
                binding.etStartTime.setText(com.studyos.cainter.util.DateUtils.formatTo12Hour(time24))
            }, h, m, false).show()
        }

        binding.etEndTime.setOnClickListener {
            val current = com.studyos.cainter.util.DateUtils.to24Hour(binding.etEndTime.text.toString())
            val parts = current.split(":")
            val h = parts[0].toIntOrNull() ?: 10
            val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
            android.app.TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                val time24 = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute)
                binding.etEndTime.setText(com.studyos.cainter.util.DateUtils.formatTo12Hour(time24))
            }, h, m, false).show()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val title = binding.etTitle.text.toString().trim()
                if (title.isBlank()) return@setPositiveButton

                val rawStart = binding.etStartTime.text.toString().trim().ifBlank { "09:00 AM" }
                val rawEnd = binding.etEndTime.text.toString().trim().ifBlank { "10:00 AM" }
                val startTime24 = com.studyos.cainter.util.DateUtils.to24Hour(rawStart)
                val endTime24 = com.studyos.cainter.util.DateUtils.to24Hour(rawEnd)

                val selectedDay = binding.spinnerDay.selectedItemPosition + 1
                val selectedCategory = ScheduleCategory.entries[binding.spinnerCategory.selectedItemPosition].name
                val notes = binding.etNotes.text.toString().trim()

                val durationMinutes = calculateDuration(startTime24, endTime24)

                val savedItem = ScheduleItem(
                    id = item?.id ?: 0L,
                    scheduleTypeId = item?.scheduleTypeId ?: scheduleTypeId,
                    dayOfWeek = selectedDay,
                    title = title,
                    category = selectedCategory,
                    startTime = startTime24,
                    endTime = endTime24,
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
            val s24 = com.studyos.cainter.util.DateUtils.to24Hour(start)
            val e24 = com.studyos.cainter.util.DateUtils.to24Hour(end)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val startTime = LocalTime.parse(s24, formatter)
            val endTime = LocalTime.parse(e24, formatter)
            val minutes = ChronoUnit.MINUTES.between(startTime, endTime).toInt()
            if (minutes > 0) minutes else (minutes + 1440)
        } catch (_: Exception) {
            60
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
