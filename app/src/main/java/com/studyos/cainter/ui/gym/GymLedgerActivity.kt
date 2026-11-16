package com.studyos.cainter.ui.gym

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.data.local.entity.WorkoutEntry
import com.studyos.cainter.databinding.ActivityGymLedgerBinding
import com.studyos.cainter.util.DateUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class GymLedgerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGymLedgerBinding
    private lateinit var adapter: GymEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGymLedgerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbarGym.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = GymEntryAdapter(
            onDeleteClick = { entry ->
                confirmDelete(entry)
            }
        )
        binding.rvGymEntries.layoutManager = LinearLayoutManager(this)
        binding.rvGymEntries.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddExerciseBanner.setOnClickListener {
            val dialog = AddExerciseDialog.newInstance()
            dialog.show(supportFragmentManager, "AddExerciseDialog")
        }
    }

    private fun observeData() {
        val app = application as StudyOSApplication
        val todayDate = DateUtils.getTodayDateString()

        lifecycleScope.launch {
            combine(
                app.workoutRepository.getAllWorkouts(),
                app.workoutRepository.getTodayWorkoutCount(todayDate),
                app.workoutRepository.getTotalExercisesLogged(),
                app.workoutRepository.getTotalWorkoutDays()
            ) { allWorkouts, todayCount, totalCount, totalDays ->
                Quad(allWorkouts, todayCount, totalCount, totalDays)
            }.collect { (allWorkouts, todayCount, totalCount, totalDays) ->
                binding.tvStatTodayExercises.text = "$todayCount"
                binding.tvStatTotalExercises.text = "$totalCount"
                binding.tvStatTotalDays.text = "$totalDays"

                adapter.submitList(allWorkouts)

                if (allWorkouts.isEmpty()) {
                    binding.layoutEmptyGym.visibility = View.VISIBLE
                    binding.rvGymEntries.visibility = View.GONE
                } else {
                    binding.layoutEmptyGym.visibility = View.GONE
                    binding.rvGymEntries.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun confirmDelete(entry: WorkoutEntry) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Exercise Entry")
            .setMessage("Are you sure you want to remove '${entry.exerciseName}' from your workout ledger?")
            .setPositiveButton("Delete") { _, _ ->
                val app = application as StudyOSApplication
                lifecycleScope.launch {
                    app.workoutRepository.deleteExercise(entry.id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
