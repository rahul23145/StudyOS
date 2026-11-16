package com.studyos.cainter.ui.timetable

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.data.local.entity.ScheduleType
import com.studyos.cainter.data.repository.ScheduleRepository
import com.studyos.cainter.databinding.DialogAddScheduleTypeBinding
import com.studyos.cainter.databinding.DialogScheduleTypesBinding
import kotlinx.coroutines.launch

class ScheduleTypeDialog : DialogFragment() {

    private var _binding: DialogScheduleTypesBinding? = null
    private val binding get() = _binding!!

    private var onTypeSelected: ((ScheduleType) -> Unit)? = null

    companion object {
        fun newInstance(onSelected: ((ScheduleType) -> Unit)? = null): ScheduleTypeDialog {
            val dialog = ScheduleTypeDialog()
            dialog.onTypeSelected = onSelected
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogScheduleTypesBinding.inflate(layoutInflater)
        val app = requireActivity().application as StudyOSApplication
        val repository = app.scheduleRepository

        val adapter = ScheduleTypeAdapter { selectedType ->
            lifecycleScope.launch {
                repository.setActiveScheduleType(selectedType.id)
                Toast.makeText(requireContext(), "Switched to: ${selectedType.name}", Toast.LENGTH_SHORT).show()
                onTypeSelected?.invoke(selectedType)
                dismiss()
            }
        }

        binding.rvScheduleTypes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScheduleTypes.adapter = adapter

        lifecycleScope.launch {
            repository.getAllScheduleTypes().collect { types ->
                adapter.submitList(types)
            }
        }

        binding.btnCreateNewType.setOnClickListener {
            showCreateNewTypeDialog(repository)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setNegativeButton("Close", null)
            .create()
    }

    private fun showCreateNewTypeDialog(repository: ScheduleRepository) {
        val addBinding = DialogAddScheduleTypeBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(requireContext())
            .setView(addBinding.root)
            .setPositiveButton("Save Profile") { _, _ ->
                val name = addBinding.etProfileName.text.toString().trim()
                if (name.isBlank()) return@setPositiveButton

                val desc = addBinding.etProfileDesc.text.toString().trim()
                val targetHours = addBinding.etTargetHours.text.toString().toFloatOrNull() ?: 8.0f
                val wakeTime = addBinding.etWakeTime.text.toString().trim().ifBlank { "06:30" }
                val sleepTime = addBinding.etSleepTime.text.toString().trim().ifBlank { "23:00" }

                lifecycleScope.launch {
                    val newType = ScheduleType(
                        name = name,
                        description = if (desc.isNotBlank()) desc else "Custom Schedule Profile",
                        targetDailyHours = targetHours,
                        wakeTime = wakeTime,
                        sleepTime = sleepTime,
                        isActive = true
                    )
                    val id = repository.createScheduleType(newType)
                    repository.setActiveScheduleType(id)
                    Toast.makeText(requireContext(), "Created & Switched to: $name", Toast.LENGTH_SHORT).show()
                    onTypeSelected?.invoke(newType.copy(id = id))
                    dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
