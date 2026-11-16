package com.studyos.cainter.ui.gym

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.R
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.databinding.DialogAddExerciseBinding
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.launch

class AddExerciseDialog : DialogFragment() {

    private var _binding: DialogAddExerciseBinding? = null
    private val binding get() = _binding!!

    private var onExerciseLoggedListener: (() -> Unit)? = null

    fun setOnExerciseLoggedListener(listener: () -> Unit) {
        onExerciseLoggedListener = listener
    }

    companion object {
        fun newInstance(): AddExerciseDialog = AddExerciseDialog()
    }

    private val muscleSuggestions = mapOf(
        "Chest" to listOf("Barbell Bench Press", "Incline Dumbbell Press", "Dumbbell Flyes", "Push-ups", "Chest Dips"),
        "Back" to listOf("Deadlift", "Lat Pulldown", "Pull-ups", "Barbell Row", "Seated Cable Row"),
        "Legs" to listOf("Barbell Squat", "Leg Press", "Romanian Deadlift", "Leg Extension", "Calf Raises"),
        "Shoulders" to listOf("Overhead Dumbbell Press", "Lateral Raises", "Face Pulls", "Military Press"),
        "Arms" to listOf("Bicep Barbell Curl", "Hammer Curls", "Tricep Pushdown", "Skull Crushers", "Preacher Curl"),
        "Core / Abs" to listOf("Plank", "Hanging Leg Raises", "Cable Woodchoppers", "Ab Crunches"),
        "Cardio" to listOf("Treadmill Running", "Outdoor Jogging", "Stationary Bike", "Jump Rope")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMuscleGroupChips()
        updateSuggestions("Chest")

        binding.btnSaveExercise.setOnClickListener {
            saveExercise()
        }
    }

    private fun setupMuscleGroupChips() {
        binding.chipGroupMuscle.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedMuscle = when {
                checkedIds.contains(R.id.chipChest) -> "Chest"
                checkedIds.contains(R.id.chipBack) -> "Back"
                checkedIds.contains(R.id.chipLegs) -> "Legs"
                checkedIds.contains(R.id.chipShoulders) -> "Shoulders"
                checkedIds.contains(R.id.chipArms) -> "Arms"
                checkedIds.contains(R.id.chipCore) -> "Core / Abs"
                checkedIds.contains(R.id.chipCardio) -> "Cardio"
                else -> "Chest"
            }
            updateSuggestions(selectedMuscle)
        }
    }

    private fun updateSuggestions(muscle: String) {
        binding.chipGroupSuggestions.removeAllViews()
        val list = muscleSuggestions[muscle] ?: return

        for (exercise in list) {
            val chip = Chip(requireContext()).apply {
                text = exercise
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    binding.etExerciseName.setText(exercise)
                }
            }
            binding.chipGroupSuggestions.addView(chip)
        }
    }

    private fun saveExercise() {
        val exerciseName = binding.etExerciseName.text.toString().trim()
        if (exerciseName.isBlank()) {
            binding.tilExerciseName.error = "Please enter an exercise name"
            return
        }

        val sets = binding.etSets.text.toString().toIntOrNull() ?: 3
        val reps = binding.etReps.text.toString().toIntOrNull() ?: 10
        val weight = binding.etWeight.text.toString().toFloatOrNull() ?: 0f
        val notes = binding.etNotes.text.toString().trim()

        val muscleGroup = when {
            binding.chipBack.isChecked -> "Back"
            binding.chipLegs.isChecked -> "Legs"
            binding.chipShoulders.isChecked -> "Shoulders"
            binding.chipArms.isChecked -> "Arms"
            binding.chipCore.isChecked -> "Core / Abs"
            binding.chipCardio.isChecked -> "Cardio"
            else -> "Chest"
        }

        val app = requireActivity().application as StudyOSApplication
        val todayDate = DateUtils.getTodayDateString()

        viewLifecycleOwner.lifecycleScope.launch {
            app.workoutRepository.logExercise(
                date = todayDate,
                muscleGroup = muscleGroup,
                exerciseName = exerciseName,
                sets = sets,
                reps = reps,
                weightKg = weight,
                notes = notes
            )
            Toast.makeText(requireContext(), "💪 Logged $exerciseName ($muscleGroup) in Gym Ledger!", Toast.LENGTH_SHORT).show()
            onExerciseLoggedListener?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
